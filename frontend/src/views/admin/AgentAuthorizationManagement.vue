<template>
  <div class="agent-auth-management">
    <el-card shadow="never" :body-style="{ padding: 0 }">
      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="搜索代理商邮箱/用户名" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="6" :md="4" class="mb-10">
            <el-select v-model="statusFilter" placeholder="状态" clearable @change="handleSearch" style="width: 100%">
              <el-option label="全部" value="" />
              <el-option label="有效" value="1" />
              <el-option label="已吊销" value="0" />
              <el-option label="已过期" value="expired" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="10" :md="14" class="button-group">
            <el-button @click="resetFilters">重置</el-button>
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="primary" @click="addAuth">添加授权</el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="authList" v-loading="loading" stripe>
          <el-table-column label="代理商" min-width="180" :show-overflow-tooltip="true">
            <template #default="{ row }">
              <div>{{ row.agentName || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="商品" min-width="140" :show-overflow-tooltip="true">
            <template #default="{ row }">
              {{ row.productName || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="余额" width="100" align="center">
            <template #default="{ row }">
              {{ row.remainingCount ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="授权时间" width="170" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.grantedAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="到期时间" width="170" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.expiresAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row)">{{ statusText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="180" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" link type="warning" @click="revokeAuth(row)">吊销</el-button>
              <el-button v-else link type="success" @click="restoreAuth(row)">恢复</el-button>
              <el-button link @click="editAuth(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteAuth(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无代理授权数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="showDialog" :title="editingAuth ? '编辑代理授权' : '添加代理授权'" :width="isMobile ? '90%' : '520px'">
      <el-form :model="authForm" :rules="authRules" ref="authFormRef" label-width="100px">
        <el-form-item v-if="!editingAuth" label="代理商" prop="agentId">
          <el-select v-model="authForm.agentId" filterable placeholder="选择代理商" style="width: 100%">
            <el-option v-for="u in agentOptions" :key="u.id" :value="u.id"
              :label="`${u.username}（${u.email}）`" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!editingAuth" label="商品" prop="productId">
          <el-select v-model="authForm.productId" filterable placeholder="选择商品" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="余额" prop="remainingCount">
          <el-input-number v-model="authForm.remainingCount" :min="0" :max="999999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="到期时间" prop="expiresAt">
          <el-date-picker v-model="authForm.expiresAt" type="datetime" placeholder="选择到期时间（可选）"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" clearable />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../services/api'
import { formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

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

const agentOptions = ref([])
const productOptions = ref([])

const authForm = reactive({
  agentId: '',
  productId: '',
  remainingCount: 100,
  expiresAt: ''
})

const authRules = {
  agentId: [{ required: true, message: '请选择代理商', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  remainingCount: [{ required: true, message: '请输入余额', trigger: 'blur' }]
}

const isExpired = (row) => {
  return row.expiresAt && new Date(row.expiresAt) <= new Date()
}

const statusText = (row) => {
  if (row.status === 0) return '已吊销'
  return isExpired(row) ? '已过期' : '有效'
}

const statusTagType = (row) => {
  if (row.status === 0) return 'danger'
  return isExpired(row) ? 'warning' : 'success'
}

const loadList = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value || undefined
    }
    if (statusFilter.value === '0' || statusFilter.value === '1') {
      params.status = statusFilter.value
    }
    const response = await api.admin.getAgentAuthorizationList(params)
    if (response && response.data) {
      let records = response.data.records || []
      if (statusFilter.value === 'expired') {
        records = records.filter(r => r.status === 1 && isExpired(r))
      }
      authList.value = records
      total.value = response.data.total || 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载代理授权失败')
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  try {
    const [usersRes, productsRes] = await Promise.all([
      api.admin.getCustomerUserList({ page: 1, size: 1000 }),
      api.admin.getProductList({ page: 1, size: 1000 })
    ])
    // 仅显示代理商角色用户
    agentOptions.value = (usersRes?.data?.records || []).filter(u => u.role === 'agent')
    productOptions.value = productsRes?.data?.records || []
  } catch {
    // 选项加载失败不阻塞
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadList()
}

const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  currentPage.value = 1
  loadList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadList()
}

const addAuth = () => {
  editingAuth.value = null
  Object.assign(authForm, { agentId: '', productId: '', remainingCount: 100, expiresAt: '' })
  showDialog.value = true
}

const editAuth = (row) => {
  editingAuth.value = row
  Object.assign(authForm, {
    agentId: row.agentId,
    productId: row.productId,
    remainingCount: row.remainingCount,
    expiresAt: row.expiresAt
  })
  showDialog.value = true
}

const saveAuth = async () => {
  if (!authFormRef.value) return
  try {
    await authFormRef.value.validate()
    if (editingAuth.value) {
      await api.admin.updateAgentAuthorization(editingAuth.value.id, {
        remainingCount: authForm.remainingCount,
        expiresAt: authForm.expiresAt || null
      })
      ElMessage.success('代理授权更新成功')
    } else {
      await api.admin.createAgentAuthorization({
        agentId: authForm.agentId,
        productId: authForm.productId,
        remainingCount: authForm.remainingCount,
        expiresAt: authForm.expiresAt || null
      })
      ElMessage.success('代理授权创建成功')
    }
    showDialog.value = false
    editingAuth.value = null
    loadList()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const revokeAuth = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定吊销代理商 "${row.agentName || row.agentId}" 对商品 "${row.productName}" 的授权吗？吊销后该代理商将无法再为此商品生成卡密。`,
      '确认吊销',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await api.admin.revokeAgentAuthorization(row.id)
    ElMessage.success('已吊销')
    loadList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('吊销失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const restoreAuth = async (row) => {
  try {
    await api.admin.restoreAgentAuthorization(row.id)
    ElMessage.success('已恢复')
    loadList()
  } catch (error) {
    ElMessage.error('恢复失败: ' + (error.response?.data?.message || error.message))
  }
}

const deleteAuth = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定删除该代理授权记录？此操作不可恢复！',
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error', confirmButtonClass: 'el-button--danger' }
    )
    await api.admin.deleteAgentAuthorization(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

onMounted(() => {
  loadList()
  loadOptions()
})
</script>

<style scoped>
.agent-auth-management {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
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
</style>
