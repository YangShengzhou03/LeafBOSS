<template>
  <div class="admin-users">
    <el-card class="users-card" shadow="never" :body-style="{ padding: 0 }">

      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="搜索邮箱或用户名" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="6" :md="4" class="mb-10">
            <el-select v-model="statusFilter" placeholder="用户状态" clearable @change="handleSearch" style="width: 100%">
              <el-option label="全部" value="" />
              <el-option label="启用" value="active" />
              <el-option label="禁用" value="inactive" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="10" :md="14" class="button-group">
            <el-button @click="resetFilters">重置</el-button>
            <el-button :loading="exporting" @click="handleExportUsers">
              导出
            </el-button>
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="primary" @click="addUser">
              添加用户
            </el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="users" style="width: 100%" v-loading="loading" stripe>
          <el-table-column prop="username" label="用户名" min-width="120" align="center" :show-overflow-tooltip="true" />
          <el-table-column prop="email" label="邮箱" min-width="200" align="center" :show-overflow-tooltip="true" />

          <el-table-column prop="role" label="角色" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.role === 'agent' ? 'warning' : 'info'">
                {{ scope.row.role === 'agent' ? '代理商' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'">
                {{ scope.row.status === 'active' ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="registeredAt" label="注册时间" width="180" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.registeredAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="registerIp" label="注册IP" width="260" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatIpWithRegion(scope.row.registerIp, scope.row.registerRegion) }}
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginTime" label="最后登录时间" width="180" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.lastLoginTime) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginIp" label="最后登录IP" width="260" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatIpWithRegion(scope.row.lastLoginIp, scope.row.lastLoginRegion) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="200" fixed="right" align="center">
            <template #default="scope">
              <el-button link size="default" @click="editUser(scope.row)">编辑</el-button>
              <el-button link size="default" type="info" @click="resetPassword(scope.row)">重置密码</el-button>
              <el-button link size="default" type="danger" @click="deleteUser(scope.row)">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无用户数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="totalUsers" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="showAddUserDialog" :title="editingUser ? '编辑用户' : '添加用户'" :width="isMobile ? '90%' : '500px'">
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" :label-width="isMobile ? '60px' : '80px'">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!editingUser">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="userForm.role">
            <el-radio label="user">用户</el-radio>
            <el-radio label="agent">代理商</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddUserDialog = false">取消</el-button>
          <el-button type="primary" @click="saveUser">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import api from '../../services/api'
import { formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

const loading = ref(false)
const exporting = ref(false)
const isMobile = useIsMobile()

onMounted(() => {
  loadUsers()
})

const users = ref([])
const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const totalUsers = ref(0)
const showAddUserDialog = ref(false)
const editingUser = ref(null)
const userFormRef = ref(null)

const userForm = reactive({
  username: '',
  email: '',
  password: '',
  role: 'user',
  status: 'active'
})

const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur', validator: (rule, value, callback) => {
      if (!editingUser.value && !value) {
        callback(new Error('请输入密码'))
      } else if (value && (value.length < 6 || value.length > 20)) {
        callback(new Error('长度在 6 到 20 个字符'))
      } else {
        callback()
      }
    }}
  ],
  role: [
    { required: true, message: '请选择用户角色', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择用户状态', trigger: 'change' }
  ]
}

const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }

    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }

    if (statusFilter.value) {
      params.status = statusFilter.value
    }

    const response = await api.admin.getCustomerUserList(params)

    if (response && response.data) {
      users.value = response.data.records || response.data.content || []
      totalUsers.value = response.data.total || response.data.totalElements || 0
    } else {
      users.value = []
      totalUsers.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载用户数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

const resetFilters = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  currentPage.value = 1
  loadUsers()
}

const resetPassword = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要重置用户 "${user.email}" 的密码为"123456"吗？`,
      '确认重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.admin.resetCustomerUserPassword({
      email: user.email,
      newPassword: '123456'
    })
    ElMessage.success(`密码重置成功，新密码为：123456`)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重置密码失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadUsers()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadUsers()
}

const editUser = (user) => {
  editingUser.value = user
  userForm.username = user.username
  userForm.email = user.email
  userForm.role = user.role || 'user'
  userForm.status = user.status
  userForm.password = ''
  showAddUserDialog.value = true
}

const saveUser = async () => {
  if (!userFormRef.value) return

  try {
    await userFormRef.value.validate()

    const userData = {
      username: userForm.username,
      email: userForm.email,
      role: userForm.role,
      status: userForm.status
    }

    if (!editingUser.value && userForm.password) {
      userData.password = userForm.password
    }

    if (editingUser.value) {
      await api.admin.updateCustomerUser(editingUser.value.id, userData)
    } else {
      await api.user.createCustomerUser(userData)
    }

    ElMessage.success(editingUser.value ? '用户更新成功' : '用户添加成功')
    showAddUserDialog.value = false
    editingUser.value = null
    resetUserForm()
    loadUsers()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存用户失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const addUser = () => {
  editingUser.value = null
  resetUserForm()
  showAddUserDialog.value = true
}

const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.email}" 吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )

    await api.admin.deleteCustomerUser(user.id)
    ElMessage.success('用户删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除用户失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const resetUserForm = () => {
  Object.assign(userForm, {
    username: '',
    email: '',
    password: '',
    role: 'user',
    status: 'active'
  })
}

const formatIpWithRegion = (ip, region) => {
  if (!ip) return '-'
  if (!region) return ip
  return `${ip}（${region}）`
}

const handleExportUsers = async () => {
  if (exporting.value) return
  exporting.value = true
  try {
    const all = []
    const size = 500
    for (let page = 1; ; page++) {
      const params = { page, size }
      if (searchQuery.value) params.keyword = searchQuery.value
      if (statusFilter.value) params.status = statusFilter.value
      const response = await api.admin.getCustomerUserList(params)
      const records = response?.data?.records || response?.data?.content || []
      all.push(...records)
      if (records.length < size) break
    }
    if (all.length === 0) {
      ElMessage.warning('暂无用户数据可导出')
      return
    }
    const rows = all.map(u => ({
      'ID': u.id,
      '用户名': u.username || '-',
      '邮箱': u.email || '-',
      '角色': u.role === 'agent' ? '代理商' : '用户',
      '状态': u.status === 'active' ? '启用' : '禁用',
      '注册时间': formatDateTime(u.registeredAt) || '-',
      '注册IP': formatIpWithRegion(u.registerIp, u.registerRegion),
      '最后登录时间': formatDateTime(u.lastLoginTime) || '-',
      '最后登录IP': formatIpWithRegion(u.lastLoginIp, u.lastLoginRegion)
    }))
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '用户数据')
    XLSX.writeFile(wb, `用户数据_${new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success(`已导出 ${all.length} 个用户`)
  } catch (error) {
    ElMessage.error(error.message || '导出用户数据失败')
  } finally {
    exporting.value = false
  }
}

</script>

<style scoped>
.admin-users {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.users-card {
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
</style>
