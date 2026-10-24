<template>
  <div class="user-profile">
    <el-card class="profile-card" shadow="never" :body-style="{ padding: '24px' }">

      <div class="profile-actions">
        <el-button type="primary" @click="editProfile">
          修改资料
        </el-button>
      </div>
      <div class="user-info-display">
        <el-avatar :size="100" class="user-avatar">
          {{ userInfo.username ? userInfo.username.charAt(0) : 'U' }}
        </el-avatar>

        <div class="info-section">
          <h2 class="user-name">{{ userInfo.username || '未设置用户名' }}</h2>
          <p class="user-email">{{ userInfo.email || '未设置邮箱' }}</p>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">角色：</span>
              <el-tag :type="roleTagType">{{ roleText }}</el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">注册时间：</span>
              <span class="info-value">{{ userInfo.registeredAt ? formatDateTime(userInfo.registeredAt) : '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">最后登录：</span>
              <span class="info-value">{{ userInfo.lastLoginTime ? formatDateTime(userInfo.lastLoginTime) : '未知' }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="profileDialogVisible" title="修改资料" width="500px">
      <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="profileForm.username" placeholder="请输入用户名" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" type="email" style="flex:1" />
        </el-form-item>
        <el-form-item label="验证码" prop="verificationCode" v-if="emailChanged">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="profileForm.verificationCode" placeholder="请输入邮箱验证码" maxlength="6" style="flex:1" />
            <el-button @click="sendEmailCode" :loading="codeSending" :disabled="codeCountdown > 0" style="flex-shrink:0">
              {{ codeCountdown > 0 ? codeCountdown + 's后重发' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="新密码" prop="password">
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/services/api'
import store from '@/utils/store.js'
import { formatDateTime } from '@/utils/utils.js'

const profileFormRef = ref()

const profileDialogVisible = ref(false)
const saving = ref(false)

const userInfo = ref({
  username: '',
  email: '',
  role: '',
  registeredAt: '',
  lastLoginTime: ''
})

const profileForm = reactive({
  username: '',
  email: '',
  password: '',
  verificationCode: ''
})

const codeSending = ref(false)
const codeCountdown = ref(0)
let countdownTimer = null

const emailChanged = computed(() => {
  return profileForm.email !== userInfo.value.email
})

const profileRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const roleText = computed(() => {
  const map = { admin: '管理员', agent: '代理商', user: '普通用户' }
  return map[userInfo.value.role] || '普通用户'
})

const roleTagType = computed(() => {
  const map = { admin: 'danger', agent: 'warning', user: 'info' }
  return map[userInfo.value.role] || 'info'
})

const loadUserInfo = async () => {
  try {
    const response = await api.user.userGetCurrentUser()

    if (response && response.code === 200 && response.data) {
      const userData = response.data
      userInfo.value = {
        username: userData.username || '',
        email: userData.email || '',
        role: userData.role || '',
        registeredAt: userData.registeredAt || '',
        lastLoginTime: userData.lastLoginTime || ''
      }
    } else {
      ElMessage.error(response?.message || '获取用户信息失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载用户信息失败，请检查网络连接')
  }
}

const editProfile = () => {
  profileForm.username = userInfo.value.username
  profileForm.email = userInfo.value.email
  profileForm.password = ''
  profileForm.verificationCode = ''
  profileDialogVisible.value = true
}

const sendEmailCode = async () => {
  if (!profileForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  try {
    codeSending.value = true
    const response = await api.user.sendEmailChangeCode(profileForm.email)
    if (response && response.code === 200) {
      ElMessage.success('验证码已发送到 ' + profileForm.email)
      codeCountdown.value = 60
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = setInterval(() => {
        codeCountdown.value--
        if (codeCountdown.value <= 0) {
          clearInterval(countdownTimer)
          countdownTimer = null
        }
      }, 1000)
    } else {
      ElMessage.error(response?.message || '发送失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '发送失败，请重试')
  } finally {
    codeSending.value = false
  }
}

const saveProfile = async () => {
  if (!profileFormRef.value) return

  try {
    await profileFormRef.value.validate()
    saving.value = true

    const submitData = {
      username: profileForm.username,
      email: profileForm.email
    }
    if (profileForm.password) {
      submitData.password = profileForm.password
    }
    if (emailChanged.value) {
      // 前端校验：验证码必须为6位纯数字
      if (!/^\d{6}$/.test(profileForm.verificationCode)) {
        ElMessage.error('验证码错误或过期');
        return;
      }
      submitData.verificationCode = profileForm.verificationCode
    }

    const response = await api.user.updateMyProfile(submitData)

    if (response && response.code === 200) {
      profileDialogVisible.value = false
      ElMessage.success('个人资料保存成功')
      await loadUserInfo()
      // 同步刷新全局用户状态（顶部用户名、水印等）
      await store.fetchCurrentUser('user')
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

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.user-profile {
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
  .user-profile {
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
