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
            <el-select v-model="statusFilter" placeholder="管理员状态" clearable @change="handleSearch" style="width: 100%">
              <el-option label="全部" value="" />
              <el-option label="启用" value="active" />
              <el-option label="禁用" value="inactive" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="10" :md="14" class="button-group">
            <el-button @click="resetFilters">重置</el-button>
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="primary" @click="addUser">
              添加
            </el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="users" style="width: 100%" v-loading="loading" stripe>
          <el-table-column prop="username" label="管理员名" min-width="120" align="center" :show-overflow-tooltip="true" />
          <el-table-column prop="email" label="邮箱" min-width="200" align="center" :show-overflow-tooltip="true" />

          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'">
                {{ scope.row.status === 'active' ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="180" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.createdAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="registerIp" label="注册IP" width="200" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatIpWithRegion(scope.row.registerIp, scope.row.registerRegion) }}
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginTime" label="最后上线时间" width="180" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.lastLoginTime) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginIp" label="最后登录IP" width="200" align="center"
            :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatIpWithRegion(scope.row.lastLoginIp, scope.row.lastLoginRegion) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="200" fixed="right" align="center">
            <template #default="scope">
              <el-button link size="default" @click="editUser(scope.row)">编辑</el-button>
              <el-button link size="default" type="info" @click="resetPassword(scope.row)">重置密码</el-button>
              <el-popconfirm
                title="确定删除该管理员吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="deleteUser(scope.row)">
                <template #reference>
                  <el-button link size="default" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无管理人员数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="totalUsers" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="showAddUserDialog" :title="editingUser ? '编辑管理人员' : '添加管理人员'" :width="isMobile ? '90%' : '500px'">
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" :label-width="isMobile ? '60px' : '80px'">
        <el-form-item label="用户名" prop="username" v-if="editingUser">
          <el-input v-model="userForm.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!editingUser">
          <el-input v-model="userForm.password" type="password" show-password />
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
import api from '../../services/api'
import { formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

const loading = ref(false)
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
  status: 'active'
})

const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
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
  status: [
    { required: true, message: '请选择管理员状态', trigger: 'change' }
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

    const response = await api.admin.getUserList(params)

    if (response && response.data) {
      users.value = response.data.records || response.data.content || []
      totalUsers.value = response.data.total || response.data.totalElements || 0
    } else {
      users.value = []
      totalUsers.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载管理人员数据失败')
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
    const { value: operatorPassword } = await ElMessageBox.prompt(
      `确定要重置管理人员 "${user.email}" 的密码为"123456"吗？请输入您的密码确认操作。`,
      '确认重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        inputType: 'password',
        inputPlaceholder: '您的登录密码',
        inputValidator: (v) => (v && v.trim() ? true : '请输入您的密码')
      }
    )

    const response = await api.user.adminResetPassword({
      email: user.email,
      newPassword: '123456',
      operatorPassword
    })
    if (response && response.code === 200) {
      ElMessage.success(`密码重置成功，新密码为：123456`)
    } else {
      ElMessage.error(response?.message || '重置密码失败')
    }
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
  userForm.status = user.status
  userForm.password = ''
  showAddUserDialog.value = true
}

const saveUser = async () => {
  if (!userFormRef.value) return

  try {
    await userFormRef.value.validate()

    const userData = {
      email: userForm.email,
      status: userForm.status
    }

    if (editingUser.value && userForm.username) {
      userData.username = userForm.username
    }

    if (!editingUser.value && userForm.password) {
      userData.password = userForm.password
    }

    if (editingUser.value) {
      await api.user.updateUser(editingUser.value.id, userData)
    } else {
      await api.user.createUser(userData)
    }

    ElMessage.success(editingUser.value ? '管理人员更新成功' : '管理人员添加成功')
    showAddUserDialog.value = false
    editingUser.value = null
    resetUserForm()
    loadUsers()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('保存管理人员失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const addUser = () => {
  editingUser.value = null
  resetUserForm()
  showAddUserDialog.value = true
}

const deleteUser = async (user) => {
  await api.user.deleteUser(user.id)
  ElMessage.success('管理人员删除成功')
  loadUsers()
}

const resetUserForm = () => {
  Object.assign(userForm, {
    email: '',
    password: '',
    status: 'active'
  })
}

const formatIpWithRegion = (ip, region) => {
  if (!ip) return '-'
  if (!region) return ip
  return `${ip}（${region}）`
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
