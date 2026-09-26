<template>
  <div class="admin-product-management">
    <el-card class="product-card" shadow="never" :body-style="{ padding: 0 }">

      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="商品名称" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="6" :md="4" class="mb-10">
            <el-select v-model="statusFilter" placeholder="商品状态" clearable @change="handleSearch()" style="width: 100%">
              <el-option label="全部" value="" />
              <el-option label="上架" value="active" />
              <el-option label="下架" value="inactive" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="10" :md="14" class="button-group">
            <el-button @click="resetFilters()">重置</el-button>
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="primary" @click="handleAddProduct">新增商品</el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="products" v-loading="loading" style="width: 100%" :key="tableKey"
          :reserve-selection="false" :row-key="row => row.id || Math.random()">
          <el-table-column prop="id" label="ID" width="100" align="center">
            <template #default="scope">
              <span class="id-display">{{ formatId(scope.row.id) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="商品名称" min-width="180" align="left" :show-overflow-tooltip="true" />
          <el-table-column prop="description" label="商品描述" min-width="200" align="left" :show-overflow-tooltip="true" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'">
                {{ scope.row.status === 'active' ? '上架' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.createdAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link size="default" @click="handleEditProduct(row)">编辑</el-button>
              <el-popconfirm
                title="确定删除该商品吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleDeleteProduct(row)">
                <template #reference>
                  <el-button link size="default" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无商品数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="showAddDialog" :title="editingProduct ? '编辑商品' : '添加商品'" :width="isMobile ? '90%' : '500px'">
      <el-form :model="productForm" :rules="productRules" ref="productFormRef" :label-width="isMobile ? '60px' : '80px'">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="productForm.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="productForm.status">
            <el-radio label="active">上架</el-radio>
            <el-radio label="inactive">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="saveProduct">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../services/api'
import { formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

const loading = ref(false)
const isMobile = useIsMobile()

onMounted(() => {
  loadProducts()
})

const products = ref([])

const tableKey = ref(0)

const searchQuery = ref('')
const statusFilter = ref('')

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showAddDialog = ref(false)
const editingProduct = ref(null)

const productForm = reactive({
  name: '',
  description: '',
  status: 'active'
})

const formatId = (id) => {
  if (!id) return ''
  const idStr = id.toString()
  if (idStr.length > 8) {
    return `${idStr.substring(0, 8)}...`
  }
  return idStr
}

const productRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

const loadProducts = async () => {
  loading.value = true
  try {
    const response = await api.admin.getProductList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value,
      status: statusFilter.value
    })

    if (response && response.data) {
      products.value = response.data.records || response.data.content || []
      total.value = response.data.total || response.data.totalElements || 0
    } else {
      products.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载商品数据失败，请检查网络连接')
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
    tableKey.value += 1
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadProducts()
}

const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  handleSearch()
}

const handleAddProduct = () => {
  showAddDialog.value = true
  editingProduct.value = null
  resetForm()
}

const handleEditProduct = (row) => {
  editingProduct.value = row
  Object.assign(productForm, row)
  showAddDialog.value = true
}

const saveProduct = async () => {
  try {
    if (editingProduct.value) {
      const response = await api.admin.editProduct(productForm.id, productForm);
      if (response && response.code === 200) {
        ElMessage.success('商品更新成功');
        showAddDialog.value = false;
        loadProducts();
      } else {
        ElMessage.error(response?.message || '保存商品失败');
      }
    } else {
      const response = await api.admin.createProduct(productForm);
      if (response && response.code === 200) {
        ElMessage.success('商品添加成功');
        showAddDialog.value = false;
        loadProducts();
      } else {
        ElMessage.error(response?.message || '保存商品失败');
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '保存商品失败，请检查网络连接');
  }
}

const resetForm = () => {
  editingProduct.value = null
  Object.assign(productForm, {
    name: '',
    description: '',
    status: 'active'
  })
}

const handleDeleteProduct = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    const response = await api.admin.deleteProduct(row.id)
    if (response && response.code === 200) {
      ElMessage.success('删除成功')
      loadProducts()
    } else {
      ElMessage.error(response?.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadProducts()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadProducts()
}

</script>

<style scoped>
.admin-product-management {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.product-card {
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.search-bar {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #e5edf5;
}

.mb-10 {
  margin-bottom: 10px;
}

.button-group {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 10px;
}

.flex-grow {
  flex-grow: 1;
}

.table-container {
  padding: 0 20px;
}

.id-display {
  font-family: 'SF Mono', 'Courier New', monospace;
  font-size: 12px;
  color: #64748d;
  font-feature-settings: "tnum";
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 0;
  padding: 16px 20px;
  border-top: 1px solid #e5edf5;
}
</style>
