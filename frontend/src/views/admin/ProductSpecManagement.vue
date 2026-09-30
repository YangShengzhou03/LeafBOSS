<template>
  <div class="admin-product-spec">
    <el-card class="product-spec-card" shadow="never" :body-style="{ padding: 0 }">

      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="搜索商品名称或规格名称" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="6" :md="4" class="mb-10">
            <el-select v-model="productFilter" placeholder="商品筛选" clearable @change="handleSearch"
              style="width: 100%">
              <el-option label="全部" value="" />
              <el-option v-for="product in products" :key="product.id" :label="product.name" :value="product.id" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="10" :md="14" class="button-group">
            <el-button @click="resetFilters">重置</el-button>
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="primary" @click="openAddDialog">添加规格</el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="specs" style="width: 100%" v-loading="loading" :scroll="{ x: 1200 }">
          <el-table-column prop="id" label="ID" width="100" align="center">
            <template #default="scope">
              <span class="id-display">{{ formatId(scope.row.id) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品名称" min-width="180" align="left" :show-overflow-tooltip="true">
            <template #default="scope">
              {{ scope.row.productName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="name" label="规格名称" min-width="150" align="left" :show-overflow-tooltip="true" />
          <el-table-column prop="price" label="价格" width="120" align="center">
            <template #default="scope">
              ¥{{ scope.row.price }}
            </template>
          </el-table-column>
          <el-table-column prop="validDays" label="有效天数" width="100" align="center">
            <template #default="scope">
              {{ scope.row.validDays || 30 }}天
            </template>
          </el-table-column>
          <el-table-column prop="totalKeys" label="卡密总数" width="120" align="center">
            <template #default="scope">
              {{ scope.row.totalKeys || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="usedKeys" label="已使用" width="100" align="center">
            <template #default="scope">
              {{ scope.row.usedKeys || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="usageRate" label="使用率" width="120" align="center">
            <template #default="scope">
              <span :class="getUsageRateClass(scope.row.usedKeys, scope.row.totalKeys)">
                {{ calculateUsageRate(scope.row.usedKeys, scope.row.totalKeys) }}%
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="规格状态" width="120" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'">
                {{ scope.row.status === 'active' ? '发放中' : '已停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.createTime) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="220" fixed="right" align="center">
            <template #default="scope">
              <el-button link size="default" @click="editSpec(scope.row)">编辑</el-button>
              <el-button link size="default" :type="scope.row.status === 'active' ? 'warning' : 'primary'"
                @click="toggleSpecStatus(scope.row)">
                {{ scope.row.status === 'active' ? '停用' : '启用' }}
              </el-button>
              <el-popconfirm
                title="确定删除该规格吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="deleteSpec(scope.row)">
                <template #reference>
                  <el-button link size="default" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无规格数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="totalSpecs" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="showAddDialog" :title="editingSpec ? '编辑规格' : '添加规格'" width="500px">
      <el-form :model="specForm" :rules="specRules" ref="specFormRef" label-width="100px">
        <el-form-item label="商品名称" prop="productId">
          <el-select v-model="specForm.productId" placeholder="请选择商品" style="width: 100%">
            <el-option v-for="product in products" :key="product.id" :label="product.name" :value="product.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格名称" prop="name">
          <el-input v-model="specForm.name" placeholder="请输入规格名称" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="specForm.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效天数" prop="validDays">
          <el-input-number v-model="specForm.validDays" :min="1" :max="3650" style="width: 100%" />
          <div class="form-tip">卡密激活后的有效期限，单位：天</div>
        </el-form-item>

      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="saveSpec">确定</el-button>
        </span>
      </template>
    </el-dialog>


  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../services/api'
import { formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

const loading = ref(false)
const isMobile = useIsMobile()

const specs = ref([])

const products = ref([])

const searchQuery = ref('')
const productFilter = ref('')

const currentPage = ref(1)
const pageSize = ref(10)
const totalSpecs = ref(0)

const showAddDialog = ref(false)
const editingSpec = ref(null)

const specForm = reactive({
  productId: '',
  productName: '',
  name: '',
  price: 0,
  validDays: 30
})

const formatId = (id) => {
  if (!id) return ''
  if (id.length > 8) {
    return `${id.substring(0, 8)}...`
  }
  return id
}

const specRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  name: [{ required: true, message: '请输入规格名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const calculateUsageRate = (used, total) => {
  if (!total || total === 0) return 0
  return Math.round((used / total) * 100)
}

const getUsageRateClass = (used, total) => {
  const rate = calculateUsageRate(used, total)
  if (rate >= 80) return 'high-usage'
  if (rate >= 50) return 'medium-usage'
  return 'low-usage'
}

const loadProducts = async () => {
  try {
    const response = await api.admin.getProductList({
      page: 1,
      size: 1000,
      status: 'active'
    })

    if (response && response.data) {
      const data = response.data
      if (data.records) {
        products.value = data.records
      } else if (data.content) {
        products.value = data.content
      } else {
        products.value = []
      }
    } else {
      products.value = []
    }
  } catch (error) {
    products.value = []
  }
}

const loadSpecs = async () => {
  loading.value = true
  try {
    const response = await api.admin.getSpecListDTO({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value,
      productId: productFilter.value
    })

    if (response && response.data) {
      const pageData = response.data
      const specList = pageData.records || pageData.content || pageData.list || []

      const processedSpecs = specList.map(spec => {
        const product = products.value.find(p => p.id === spec.productId)
        const productName = product ? product.name : spec.productName || '未知商品'

        let createTime = spec.createTime || spec.createdAt || spec.create_time || ''
        if (createTime) {
          if (typeof createTime === 'number' || /^\d+$/.test(createTime)) {
            createTime = formatDateTime(parseInt(createTime))
          } else if (typeof createTime === 'string') {
            createTime = formatDateTime(createTime)
          }
        }

        return {
          ...spec,
          productName: productName,
          createTime: createTime || '未知时间',
          totalKeys: spec.totalKeys || 0,
          usedKeys: spec.usedKeys || 0,
          unusedKeys: spec.unusedKeys || 0
        }
      })

      specs.value = processedSpecs
      totalSpecs.value = pageData.total || pageData.totalElements || specList.length
    } else {
      specs.value = []
      totalSpecs.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载商品规格失败，请检查网络连接')
    specs.value = []
    totalSpecs.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadSpecs()
}

const resetFilters = () => {
  searchQuery.value = ''
  productFilter.value = ''
  handleSearch()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadSpecs()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadSpecs()
}

const openAddDialog = () => {
  editingSpec.value = null
  Object.assign(specForm, {
    // 默认选中第一个商品
    productId: products.value.length > 0 ? products.value[0].id : '',
    name: '',
    price: 0,
    validDays: 30
  })
  showAddDialog.value = true
}

const editSpec = (spec) => {
  editingSpec.value = spec
  Object.assign(specForm, {
    productId: spec.productId || '',
    name: spec.name || '',
    price: spec.price || 0,
    validDays: spec.validDays || 30
  })
  showAddDialog.value = true
}

const toggleSpecStatus = async (spec) => {
  try {
    await ElMessageBox.confirm(
      `确定要${spec.status === 'active' ? '停用' : '启用'}该规格吗？`,
      '提示',
      { type: 'warning' }
    )

    const newStatus = spec.status === 'active' ? 'inactive' : 'active'

    const response = await api.admin.editSpec(spec.id, {
      status: newStatus
    })

    if (response && response.code === 200) {
      spec.status = newStatus
      ElMessage.success('操作成功')

      loadSpecs()
    } else {
      const msg = response?.message || '操作失败'
      ElMessage.error(msg)
    }
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error.response?.data?.message || error.message || '操作失败'
      ElMessage.error(msg)
    }
  }
}

const deleteSpec = async (spec) => {
  try {
    const response = await api.admin.deleteSpec(spec.id)
    if (response && response.code === 200) {
      ElMessage.success('删除成功')
      const index = specs.value.findIndex(s => s.id === spec.id)
      if (index !== -1) {
        specs.value.splice(index, 1)
      }
      loadSpecs()
    } else {
      const msg = response?.message || '删除失败'
      ElMessage.error(msg)
    }
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '删除失败'
    ElMessage.error(msg)
  }
}

const saveSpec = async () => {
  try {
    const selectedProduct = products.value.find(p => p.id === specForm.productId)
    if (!selectedProduct) {
      ElMessage.error('请选择有效的商品')
      return
    }

    const submitData = {
      ...specForm,
      productName: selectedProduct.name
    }

    if (editingSpec.value) {
      await api.admin.editSpec(editingSpec.value.id, submitData)

      const index = specs.value.findIndex(s => s.id === editingSpec.value.id)
      if (index !== -1) {
        specs.value[index] = {
          ...specs.value[index],
          ...submitData,
          productName: selectedProduct.name
        }
      }
    } else {
      const response = await api.admin.createSpec(submitData)

      const newSpec = {
        id: response.data?.id || Date.now(),
        ...submitData,
        productName: selectedProduct.name,
        totalKeys: 0,
        usedKeys: 0,
        status: 'active',
        createTime: formatDateTime(Date.now())
      }
      specs.value.unshift(newSpec)
    }

    showAddDialog.value = false
    ElMessage.success(editingSpec.value ? '更新成功' : '添加成功')
    resetForm()

    loadSpecs()
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '操作失败'
    ElMessage.error(msg)
  }
}

const resetForm = () => {
  editingSpec.value = null
  Object.assign(specForm, {
    productId: '',
    productName: '',
    name: '',
    price: 0,
    validDays: 30
  })
}

onMounted(async () => {
  await loadProducts()
  await loadSpecs()
})
</script>

<style scoped>
.admin-product-spec {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.product-spec-card {
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.search-bar {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #e5edf5;
}

.button-group {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.mb-10 {
  margin-bottom: 10px;
}

.flex-grow {
  flex-grow: 1;
}

.table-container {
  padding: 0 20px;
}

.id-display {
  font-family: 'SourceCodePro', 'Courier New', monospace;
  font-size: 12px;
  color: #64748d;
  font-weight: 500;
}

.high-usage {
  color: #ea2261;
  font-weight: 400;
}

.medium-usage {
  color: #9b6829;
  font-weight: 400;
}

.low-usage {
  color: #15be53;
  font-weight: 400;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 0;
  padding: 16px 20px;
  border-top: 1px solid #e5edf5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>