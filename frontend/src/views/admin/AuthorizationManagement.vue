<template>
  <div class="admin-authorizations">
    <el-card class="auth-card" shadow="never" :body-style="{ padding: 0 }">

      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="搜索卡密或用户ID" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="6" :md="4" class="mb-10">
            <el-select v-model="statusFilter" placeholder="授权状态" clearable @change="handleSearch" style="width: 100%">
              <el-option label="全部" value="" />
              <el-option label="已授权" value="1" />
              <el-option label="已过期" value="expired" />
              <el-option label="已吊销" value="0" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="10" :md="14" class="button-group">
            <el-button @click="resetFilters">重置</el-button>
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="primary" @click="addAuth">
              添加授权
            </el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="authList" style="width: 100%" v-loading="loading" stripe>
          <el-table-column label="用户" min-width="160" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              <div>{{ scope.row.username || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品" min-width="120" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ scope.row.productName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="specName" label="规格" min-width="100" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ scope.row.specName || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="卡密" min-width="180" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              <span v-if="scope.row.cardKey" class="card-key-text" @click="copyCardKey(scope.row.cardKey)">{{ scope.row.cardKey }}</span>
              <span v-else style="color: var(--el-text-color-secondary)">管理员直授</span>
            </template>
          </el-table-column>
          <el-table-column prop="activatedAt" label="授权时间" width="170" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.activatedAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="expiresAt" label="到期时间" width="170" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.expiresAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row)">
                {{ statusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="180" fixed="right" align="center">
            <template #default="scope">
              <el-button v-if="scope.row.status === 1" link size="default" type="warning"
                @click="revokeAuth(scope.row)">吊销</el-button>
              <el-button v-else link size="default" type="success" @click="restoreAuth(scope.row)">恢复</el-button>
              <el-button link size="default" @click="editAuth(scope.row)">编辑</el-button>
              <el-button link size="default" type="danger" @click="deleteAuth(scope.row)">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无授权数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="showDialog" :title="editingAuth ? '编辑授权' : '添加授权'" :width="isMobile ? '90%' : '520px'">
      <el-form :model="authForm" :rules="authRules" ref="authFormRef" label-width="90px">
        <template v-if="!editingAuth">
          <el-form-item label="用户" prop="userId">
            <el-select v-model="authForm.userId" filterable placeholder="选择用户" style="width: 100%">
              <el-option v-for="u in userOptions" :key="u.id" :value="u.id"
                :label="`${u.username}（${u.email || u.id}）`" />
            </el-select>
          </el-form-item>
          <el-form-item label="商品" prop="productId">
            <el-select v-model="authForm.productId" filterable placeholder="选择商品" style="width: 100%"
              @change="authForm.specId = null">
              <el-option v-for="p in productOptions" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="规格" prop="specId">
          <el-select v-model="authForm.specId" filterable clearable placeholder="选择规格（可选）" style="width: 100%">
            <el-option v-for="s in filteredSpecOptions" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="卡密" prop="cardKey">
          <el-input v-model="authForm.cardKey" placeholder="绑定卡密（可选）" />
        </el-form-item>
        <el-form-item label="到期时间" prop="expiresAt">
          <el-date-picker v-model="authForm.expiresAt" type="datetime" placeholder="选择到期时间"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" @click="saveAuth">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../services/api'
import { formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

const copyCardKey = async (cardKey) => {
  try {
    await navigator.clipboard.writeText(cardKey)
    ElMessage.success('卡密已复制')
  } catch {
    ElMessage.warning('请手动复制')
  }
}

const loading = ref(false)
const isMobile = useIsMobile()

const authList = ref([])
const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showDialog = ref(false)
const editingAuth = ref(null)
const authFormRef = ref(null)

const userOptions = ref([])
const productOptions = ref([])
const specOptions = ref([])

const authForm = reactive({
  userId: '',
  productId: '',
  specId: null,
  cardKey: '',
  expiresAt: ''
})

const authRules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  expiresAt: [{ required: true, message: '请选择到期时间', trigger: 'change' }]
}

const filteredSpecOptions = computed(() => {
  if (!editingAuth.value) {
    return specOptions.value.filter(s => s.productId === authForm.productId)
  }
  return specOptions.value
})

const isExpired = (row) => {
  return row.expiresAt && new Date(row.expiresAt) <= new Date()
}

const statusText = (row) => {
  if (row.status === 0) return '已吊销'
  return isExpired(row) ? '已过期' : '已授权'
}

const statusTagType = (row) => {
  if (row.status === 0) return 'danger'
  return isExpired(row) ? 'warning' : 'success'
}

const loadAuthList = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value || undefined
    }

    // 已过期是前端根据 expires_at 计算的，后端只支持 0/1
    if (statusFilter.value === '0' || statusFilter.value === '1') {
      params.status = statusFilter.value
    }

    const response = await api.admin.getAuthorizationList(params)
    if (response && response.data) {
      let records = response.data.records || []
      if (statusFilter.value === 'expired') {
        records = records.filter(r => r.status === 1 && isExpired(r))
      }
      authList.value = records
      total.value = response.data.total || 0
    } else {
      authList.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载授权数据失败')
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  try {
    const [usersRes, productsRes, specsRes] = await Promise.all([
      api.admin.getCustomerUserList({ page: 1, size: 1000 }),
      api.admin.getProductList({ page: 1, size: 1000 }),
      api.admin.getSpecificationDTOs()
    ])
    userOptions.value = usersRes?.data?.records || []
    productOptions.value = productsRes?.data?.records || []
    specOptions.value = specsRes?.data || []
  } catch (error) {
    // 选项加载失败不阻塞页面
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadAuthList()
}

const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  currentPage.value = 1
  loadAuthList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadAuthList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadAuthList()
}

const addAuth = () => {
  editingAuth.value = null
  Object.assign(authForm, { userId: '', productId: '', specId: null, cardKey: '', expiresAt: '' })
  showDialog.value = true
}

const editAuth = (row) => {
  editingAuth.value = row
  Object.assign(authForm, {
    userId: row.userId,
    productId: row.productId,
    specId: row.specId,
    cardKey: row.cardKey || '',
    expiresAt: row.expiresAt
  })
  showDialog.value = true
}

const saveAuth = async () => {
  if (!authFormRef.value) return
  try {
    await authFormRef.value.validate()

    if (editingAuth.value) {
      await api.admin.updateAuthorization(editingAuth.value.id, {
        specId: authForm.specId,
        cardKey: authForm.cardKey || null,
        expiresAt: authForm.expiresAt
      })
      ElMessage.success('授权更新成功')
    } else {
      await api.admin.createAuthorization({
        userId: authForm.userId,
        productId: authForm.productId,
        specId: authForm.specId,
        cardKey: authForm.cardKey || null,
        expiresAt: authForm.expiresAt
      })
      ElMessage.success('授权创建成功')
    }
    showDialog.value = false
    editingAuth.value = null
    loadAuthList()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存授权失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const revokeAuth = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要吊销用户 "${row.username || row.userId}" 的授权吗？`,
      '确认吊销',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await api.admin.revokeAuthorization(row.id)
    ElMessage.success('授权已吊销')
    loadAuthList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('吊销失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const restoreAuth = async (row) => {
  try {
    await api.admin.restoreAuthorization(row.id)
    ElMessage.success('授权已恢复')
    loadAuthList()
  } catch (error) {
    ElMessage.error('恢复失败: ' + (error.response?.data?.message || error.message))
  }
}

const deleteAuth = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除该授权记录吗？此操作不可恢复！`,
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error', confirmButtonClass: 'el-button--danger' }
    )
    await api.admin.deleteAuthorization(row.id)
    ElMessage.success('授权删除成功')
    loadAuthList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

onMounted(() => {
  loadAuthList()
  loadOptions()
})
</script>

<style scoped>
.admin-authorizations {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.auth-card {
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
  align-items: flex-start;
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

.card-key-text {
  cursor: pointer;
  font-family: monospace;
}
.card-key-text:hover {
  color: var(--el-color-primary);
}
</style>
