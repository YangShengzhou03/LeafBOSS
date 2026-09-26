<template>
  <div class="login-container">
    <div class="card-inner">
      <div class="logo-section">
        <div class="logo">
          <span class="logo-text">L</span>
        </div>
        <h1 class="app-title">LEAF-BOSS</h1>
        <p class="subtitle">业务运营支撑平台</p>
      </div>

      <div class="form-section">
        <div class="view-header">
          <h2 class="view-title">{{ currentView === 'login' ? '登录' : currentView === 'register' ? '注册' : '重置密码' }}
          </h2>
          <div class="view-indicator">
            <div class="indicator-dot" :class="{ active: currentView === 'login' }"></div>
            <div class="indicator-dot" :class="{ active: currentView === 'register' }"></div>
            <div class="indicator-dot" :class="{ active: currentView === 'forgot' }"></div>
          </div>
        </div>

        <div v-if="currentView === 'login'" class="view-content">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0" size="large"
            class="form-content" style="--el-form-item-margin-bottom: 16px">
            <el-form-item prop="email">
              <el-input v-model="loginForm.email" placeholder="邮箱" prefix-icon="Message" style="width: 100%" />
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock"
                show-password style="width: 100%" @keyup.enter="handleLogin" />
            </el-form-item>

            <el-form-item>
              <div class="remember-admin-row">
                <el-checkbox v-model="loginForm.rememberPassword">
                  记住密码
                </el-checkbox>
                <el-link type="info" @click="goToAdminLogin">管理员入口</el-link>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handleLogin" style="width: 100%">
                登录
              </el-button>
            </el-form-item>

            <div class="form-footer">
              <el-link type="primary" @click="currentView = 'register'">没有账号？立即注册</el-link>
              <el-link type="info" @click="currentView = 'forgot'">忘记密码？</el-link>
            </div>
          </el-form>
        </div>

        <div v-else-if="currentView === 'register'" class="view-content">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0"
            size="large" class="form-content" style="--el-form-item-margin-bottom: 16px">
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱" prefix-icon="Message" style="width: 100%" />
            </el-form-item>

            <el-form-item prop="verificationCode">
              <div class="verification-code-container">
                <el-input v-model="registerForm.verificationCode" placeholder="验证码" prefix-icon="Key" style="flex: 1" />
                <el-button :disabled="registerCodeSending || registerCountdown > 0" :loading="registerCodeSending"
                  @click="sendRegisterVerificationCode" class="send-code-btn">
                  {{ registerCountdown > 0 ? `${registerCountdown}s 后重发` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock"
                show-password style="width: 100%" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handleRegister" style="width: 100%">
                注册
              </el-button>
            </el-form-item>

            <div class="form-footer">
              <el-link type="primary" @click="currentView = 'login'">已有账号？立即登录</el-link>
            </div>
          </el-form>
        </div>

        <div v-else-if="currentView === 'forgot'" class="view-content">
          <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" label-width="0"
            size="large" class="form-content" style="--el-form-item-margin-bottom: 16px">
            <el-form-item prop="email">
              <el-input v-model="forgotForm.email" placeholder="邮箱" prefix-icon="Message" style="width: 100%" />
            </el-form-item>

            <el-form-item prop="verificationCode">
              <div class="verification-code-container">
                <el-input v-model="forgotForm.verificationCode" placeholder="验证码" prefix-icon="Key" style="flex: 1" />
                <el-button :disabled="forgotCodeSending || forgotCountdown > 0" :loading="forgotCodeSending"
                  @click="sendForgotVerificationCode" class="send-code-btn">
                  {{ forgotCountdown > 0 ? `${forgotCountdown}s 后重发` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item prop="newPassword">
              <el-input v-model="forgotForm.newPassword" type="password" placeholder="新密码" prefix-icon="Lock"
                show-password style="width: 100%" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handleForgotPassword" style="width: 100%">
                重置密码
              </el-button>
            </el-form-item>

            <div class="form-footer">
              <el-link type="primary" @click="currentView = 'login'">返回登录</el-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import store from '@/utils/store.js'
import api from '../services/api'
import * as utils from '@/utils/utils.js'

const router = useRouter()

const currentView = ref('login')

const loginFormRef = ref()
const loginForm = reactive({
  email: '',
  password: '',
  rememberPassword: false
})

const registerFormRef = ref()
const registerForm = reactive({
  email: '',
  verificationCode: '',
  password: ''
})

const forgotFormRef = ref()
const forgotForm = reactive({
  email: '',
  verificationCode: '',
  newPassword: ''
})

const registerCodeSending = ref(false)
const registerCountdown = ref(0)
const forgotCodeSending = ref(false)
const forgotCountdown = ref(0)

const loginRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const registerRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}


const goToAdminLogin = () => {
  router.push('/admin-login')
}

const handleLogin = async () => {
  try {
    const response = await store.userLogin(loginForm);
    if (response.success) {
      if (loginForm.rememberPassword) {
        utils.saveRememberedUsername(loginForm.email);
      } else {
        utils.removeRememberedUsername();
      }
      ElMessage.success('登录成功');
      router.replace('/user');
    } else {
      ElMessage.error(response.message || '登录失败');
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请检查网络连接');
  }
};

const sendRegisterVerificationCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱');
    return;
  }
  if (!/^[\w-]+(\.[\w-]+)*@([\w-]+\.)+[a-zA-Z]{2,7}$/.test(registerForm.email)) {
    ElMessage.warning('请输入正确的邮箱格式');
    return;
  }
  try {
    registerCodeSending.value = true;
    const response = await api.user.userSendRegisterCode({ email: registerForm.email });
    if (response && response.code === 200) {
      ElMessage.success('验证码已发送，请查收邮箱');
      startRegisterCountdown();
    } else {
      ElMessage.error(response?.message || '验证码发送失败');
    }
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败，请检查网络连接');
  } finally {
    registerCodeSending.value = false;
  }
};

const startRegisterCountdown = () => {
  registerCountdown.value = 60;
  const timer = setInterval(() => {
    registerCountdown.value--;
    if (registerCountdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
};

const handleRegister = async () => {
  try {
    // 前端校验：验证码必须为6位纯数字
    if (!/^\d{6}$/.test(registerForm.verificationCode)) {
      ElMessage.error('验证码错误或过期');
      return;
    }

    const registerData = {
      email: registerForm.email,
      password: registerForm.password,
      verificationCode: registerForm.verificationCode,
      role: 'user'
    };

    const response = await api.user.register(registerData);
    if (response && response.code === 200) {
      ElMessage.success('注册成功，正在登录');
      const loginResult = await store.userLogin({
        email: registerForm.email,
        password: registerForm.password
      });
      if (loginResult.success) {
        router.replace('/user');
      }
    } else {
      ElMessage.error(response?.message || '注册失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '注册失败，请检查网络连接')
  }
};

const sendForgotVerificationCode = async () => {
  if (!forgotForm.email) {
    ElMessage.warning('请先输入邮箱');
    return;
  }

  if (!/^[\w-]+(\.[\w-]+)*@([\w-]+\.)+[a-zA-Z]{2,7}$/.test(forgotForm.email)) {
    ElMessage.warning('请输入正确的邮箱格式');
    return;
  }

  try {
    forgotCodeSending.value = true;
    const response = await api.user.userSendResetCode({ email: forgotForm.email });

    if (response && response.code === 200) {
      ElMessage.success('验证码已发送，请查收邮箱');
      startForgotCountdown();
    } else {
      ElMessage.error(response?.message || '验证码发送失败');
    }
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败，请检查网络连接');
  } finally {
    forgotCodeSending.value = false;
  }
};

const startForgotCountdown = () => {
  forgotCountdown.value = 60;
  const timer = setInterval(() => {
    forgotCountdown.value--;
    if (forgotCountdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
};

const handleForgotPassword = async () => {
  try {
    // 前端校验：验证码必须为6位纯数字
    if (!/^\d{6}$/.test(forgotForm.verificationCode)) {
      ElMessage.error('验证码错误或过期');
      return;
    }

    const response = await api.user.userResetPassword({
      email: forgotForm.email,
      verificationCode: forgotForm.verificationCode,
      newPassword: forgotForm.newPassword
    });
    if (response && response.code === 200) {
      ElMessage.success('密码重置成功');
      currentView.value = 'login';
    } else {
      ElMessage.error(response?.message || '密码重置失败');
    }
  } catch (error) {
    ElMessage.error(error.message || '密码重置失败，请检查网络连接');
  }
};

onMounted(() => {
  const rememberedUsername = utils.getRememberedUsername()
  if (rememberedUsername) {
    loginForm.email = rememberedUsername
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 24px;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.card-inner {
  width: 100%;
  max-width: 600px;
  background: #ffffff;
  border-radius: 6px;
  padding: 32px;
  box-shadow: rgba(50, 50, 93, 0.25) 0px 30px 45px -30px, rgba(0, 0, 0, 0.1) 0px 18px 36px -18px;
  border: 1px solid #e5edf5;
  display: flex;
  gap: 24px;
}

.logo-section {
  text-align: center;
  flex: 0 0 160px;
  padding: 24px 12px;
  border-right: 1px solid #e5edf5;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.logo {
  background: #533afd;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  width: 60px;
  height: 60px;
  margin-bottom: 16px;
}

.logo-text {
  font-size: 28px;
  font-weight: 300;
  color: white;
}

.app-title {
  font-size: 22px;
  font-weight: 300;
  color: #061b31;
  margin: 8px 0 4px;
  letter-spacing: -0.22px;
}

.subtitle {
  font-size: 14px;
  font-weight: 300;
  color: #64748d;
  margin: 0;
  line-height: 1.4;
}

.form-section {
  flex: 1;
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 360px;
}

.view-header {
  text-align: center;
  margin-bottom: 24px;
  position: relative;
}

.view-title {
  color: #061b31;
  margin: 0 0 12px 0;
  font-size: 22px;
  font-weight: 300;
  letter-spacing: -0.22px;
}

.view-indicator {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}

.indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e5edf5;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.indicator-dot.active {
  background: #533afd;
  transform: scale(1.4);
}

.view-content {
  animation: fadeInUp 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.form-content {
  margin-top: 8px;
}

.verification-code-container {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.send-code-btn {
  min-width: 132px;
  flex-shrink: 0;
  background: transparent;
  border: 1px solid #b9b9f9;
  color: #533afd;
  font-weight: 400;
  border-radius: 4px;
}

.send-code-btn:hover:not(:disabled) {
  background: rgba(83, 58, 253, 0.05);
  border-color: #533afd;
  color: #533afd;
}

.send-code-btn:disabled {
  color: #64748d;
  border-color: #e5edf5;
}

.submit-btn {
  height: 40px;
  font-size: 16px;
  font-weight: 400;
  border-radius: 4px;
  margin-top: 8px;
}

.remember-admin-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding: 0 4px;
}

@media (max-width: 480px) {
  .login-container {
    padding: 16px;
  }

  .card-inner {
    padding: 24px 16px;
    flex-direction: column;
    gap: 24px;
  }

  .logo-section {
    border-right: none;
    border-bottom: 1px solid #e5edf5;
    padding-bottom: 24px;
    flex: none;
  }

  .app-title {
    font-size: 20px;
  }

  .logo {
    width: 50px;
    height: 50px;
  }

  .logo-text {
    font-size: 24px;
  }

  .form-footer {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  .verification-code-container {
    flex-direction: column;
    gap: 8px;
  }

  .send-code-btn {
    min-width: 100%;
  }
}
</style>
