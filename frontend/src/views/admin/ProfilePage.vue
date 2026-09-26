<template>
  <div class="admin-profile">
    <el-card class="profile-card" shadow="never" :body-style="{ padding: '24px' }">

      <div class="profile-actions">
        <el-button type="primary" @click="editProfile">
          修改资料
        </el-button>
      </div>
      <div class="user-info-display">
        <el-avatar :size="100" class="user-avatar">
          {{ userInfo.nickname ? userInfo.nickname.charAt(0) : 'U' }}
        </el-avatar>

        <div class="info-section">
          <h2 class="user-name">{{ userInfo.nickname ? userInfo.nickname : '未设置昵称' }}</h2>
          <p class="user-email">{{ userInfo.email || '未设置邮箱' }}</p>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">注册时间：</span>
              <span class="info-value">{{ userInfo.createTime ? formatDateTime(userInfo.createTime) : '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">账户状态：</span>
              <el-tag type="success">启用</el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="profileDialogVisible" title="修改资料" width="500px">
      <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" type="email" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="profileForm.password" type="password" placeholder="请输入新密码（留空则不修改）" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="profileDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveProfile" :loading="saving">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../services/api'
import { formatDateTime } from '@/utils/utils.js'

const profileFormRef = ref()

const profileDialogVisible = ref(false)
const saving = ref(false)

const userInfo = ref({
  id: '',
  nickname: '',
  email: '',
  createTime: ''
})

const profileForm = reactive({
  nickname: '',
  email: '',
  password: ''
})

const profileRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const loadUserInfo = async () => {
  try {
    const response = await api.user.getCurrentUser()

    if (response && response.code === 200 && response.data) {
      const userData = response.data
      userInfo.value = {
        id: userData.id || userData.userId || '',
        nickname: userData.nickname || userData.username || userData.name || '',
        email: userData.email || '',
        createTime: userData.createTime || userData.createdAt || userData.registrationTime || ''
      }

    } else {
      ElMessage.error(response?.message || '获取用户信息失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载用户信息失败，请检查网络连接')
  }
}

const editProfile = () => {
  Object.assign(profileForm, userInfo.value)
  profileDialogVisible.value = true
}

const saveProfile = async () => {
  if (!profileFormRef.value) return

  try {
    await profileFormRef.value.validate()
    saving.value = true

    const submitData = {
      username: profileForm.nickname,
      email: profileForm.email
    }

    if (profileForm.password) {
      submitData.password = profileForm.password
    }

    const response = await api.user.updateUserInfo(submitData)

    if (response && response.code === 200) {
      userInfo.value = { ...userInfo.value, ...profileForm }
      profileDialogVisible.value = false
      ElMessage.success('个人资料保存成功')
    } else {
      ElMessage.error(response?.message || '保存失败，请重试')
    }
  } catch (error) {
    if (error.errors) {
      ElMessage.warning('请检查表单填写是否正确')
    } else {
      ElMessage.error(error.message || '保存失败，请检查网络连接')
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.admin-profile {
  background-color: transparent;
  padding: 24px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.profile-card {
  width: 100%;
  max-width: 900px;
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
  overflow: hidden;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.user-info-display {
  display: flex;
  align-items: flex-start;
  gap: 32px;
  padding: 20px 0;
}

.user-avatar {
  border: 3px solid #f1f5f9;
  background-color: #533afd;
  color: white;
  font-size: 24px;
  font-weight: 300;
}

.info-section {
  flex: 1;
}

.user-name {
  margin: 0 0 4px 0;
  font-size: 24px;
  font-weight: 300;
  color: #061b31;
  letter-spacing: -0.24px;
}

.user-email {
  margin: 0 0 16px 0;
  color: #64748d;
  font-size: 14px;
  font-weight: 400;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 6px;
  border: 1px solid #e5edf5;
}

.info-label {
  font-weight: 400;
  color: #64748d;
  min-width: 90px;
  font-size: 14px;
}

.info-value {
  color: #061b31;
  font-weight: 400;
  font-size: 14px;
}

@media (max-width: 768px) {
  .admin-profile {
    padding: 16px;
  }

  .user-info-display {
    flex-direction: column;
    text-align: center;
    gap: 24px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .user-name {
    font-size: 20px;
  }

  .info-item {
    padding: 12px;
  }
}
</style>
