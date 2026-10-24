<template>
  <div class="index-layout">
    <header class="header" role="banner">
      <div class="container">
        <h1 class="logo" aria-label="LEAF-BOSS 业务运营系统">LEAF-BOSS 业务运营系统</h1>
        <nav class="auth-section" role="navigation" aria-label="用户导航">
          <div class="auth-buttons" v-if="!isAuthenticated" role="group" aria-label="登录注册选项">
            <el-button type="default" class="ghost-btn" @click="handleRegister" aria-label="注册新账户">
              注册
            </el-button>
            <el-button type="primary" class="primary-btn" @click="handleLogin" aria-label="登录系统">
              登录
            </el-button>
          </div>
          <div class="user-info" v-else role="group" aria-label="用户菜单">
            <el-dropdown @command="handleUserCommand" trigger="click" aria-label="用户操作菜单">
              <span class="user-dropdown" role="button" tabindex="0">
                <el-avatar :size="32" :src="userAvatar" :alt="`${displayName}的头像`">
                  <el-icon>
                    <User />
                  </el-icon>
                </el-avatar>
                <span class="username">{{ displayName }}</span>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu role="menu">
                  <el-dropdown-item command="dashboard" role="menuitem">
                    进入后台
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout" role="menuitem">
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </nav>
      </div>
    </header>

    <main class="main-content" role="main">
      <section class="hero-section" aria-labelledby="hero-title">
        <div class="container">
          <div class="hero-content">
            <h1 id="hero-title" class="hero-title">LEAF-BOSS 全栈技术学习</h1>
            <p class="hero-description">
              高效的业务运营支撑平台
            </p>
            <div class="cta-buttons" role="group" aria-label="主要操作">
              <el-button type="primary" size="large" class="start-btn" @click="handleStart"
                :aria-label="isAuthenticated ? '进入后台' : '登录系统'">
                {{ isAuthenticated ? '进入后台' : '登录系统' }}
              </el-button>
            </div>
          </div>
        </div>
      </section>

      <section id="features" class="features-section" aria-labelledby="features-title">
        <div class="container">
          <h2 id="features-title" class="section-title">核心功能</h2>
          <div class="features-grid" role="list">
            <article class="feature-card" v-for="feature in features" :key="feature.id" role="listitem"
              :aria-labelledby="`feature-${feature.id}`">
              <div class="feature-icon" :aria-label="`${feature.title}图标`">
                <el-icon :size="40">
                  <component :is="feature.icon" />
                </el-icon>
              </div>
              <h3 :id="`feature-${feature.id}`">{{ feature.title }}</h3>
              <p>{{ feature.description }}</p>
            </article>
          </div>
        </div>
      </section>


    </main>

    <footer class="footer" role="contentinfo">
      <div class="container">
        <div class="footer-content">
          <section class="footer-section" aria-labelledby="footer-about">
            <h3 id="footer-about">LEAF-BOSS</h3>
            <p>高效的业务运营支撑平台</p>
          </section>
          <nav class="footer-section" aria-labelledby="footer-about-site">
            <h4 id="footer-about-site">关于系统</h4>
            <ul role="list">
              <li role="listitem">
                <a href="/author-info" target="_blank" rel="noopener noreferrer" aria-label="查看关于作者">
                  关于作者
                </a>
              </li>
              <li role="listitem">
                <a href="/privacy-policy" target="_blank" rel="noopener noreferrer" aria-label="查看隐私政策">
                  隐私政策
                </a>
              </li>
            </ul>
          </nav>
          <nav class="footer-section" aria-labelledby="footer-tech">
            <h4 id="footer-tech">功能模块</h4>
            <ul role="list">
              <li role="listitem">
                <a href="#features" @click.prevent="scrollToSection('features')" aria-label="查看核心功能">
                  核心功能
                </a>
              </li>
              <li role="listitem">
                <a href="/user-guide" target="_blank" rel="noopener noreferrer" aria-label="查看使用指南">
                  使用指南
                </a>
              </li>
            </ul>
          </nav>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2025-{{ currentYear }} LEAF-BOSS 业务运营系统</p>
          <div class="icp-info">
            <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer"
              aria-label="查看ICP备案信息：赣ICP备2025075576号">
              赣ICP备2025075576号
            </a>
            <a href="https://beian.mps.gov.cn/#/query/webSearch?code=36010802001254" rel="noreferrer"
              target="_blank">赣公网安备36010802001254号</a>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref, onUnmounted, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, ArrowDown, Key, Box, DataLine } from '@element-plus/icons-vue'
import store from '@/utils/store.js'

const router = useRouter()

const currentYear = new Date().getFullYear()

const isAuthenticated = computed(() => store.state.isAuthenticated)
const currentUser = computed(() => store.state.user)
const displayName = computed(() => {
  const user = currentUser.value
  if (!user) return '管理员'
  return user.nickname || user.username || user.email || '管理员'
})
const userAvatar = computed(() => currentUser.value?.avatar || '')

const FEATURES_DATA = [
  {
    id: 1,
    icon: Key,
    title: '前端开发',
    description: 'Vue、React、TypeScript 等前端框架与工程化实践学习笔记。'
  },
  {
    id: 2,
    icon: Box,
    title: '后端开发',
    description: 'Java、Spring Boot、Node.js 等后端技术栈的学习与项目实践。'
  },
  {
    id: 3,
    icon: DataLine,
    title: '数据库与设计',
    description: 'MySQL、Redis 数据库技术及系统架构设计思路总结。'
  }
]

const features = ref(FEATURES_DATA)

let scrollTimeout = null
const scrollToSection = (sectionId) => {
  if (scrollTimeout) clearTimeout(scrollTimeout)

  scrollTimeout = setTimeout(() => {
    const element = document.getElementById(sectionId)
    if (element) {
      element.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      })
    }
  }, 50)
}

onUnmounted(() => {
  if (scrollTimeout) clearTimeout(scrollTimeout)
})

const handleError = (message, error) => {
  const msg = error.response?.data?.message || error.message || message
  ElMessage.error(msg)
}

const handleUserCommand = async (command) => {
  try {
    if (command === 'logout') {
      await ElMessageBox.confirm(
        '确定要退出登录吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
          lockScroll: false
        }
      )

      await store.logout()
      ElMessage.success('已退出登录')
      router.push('/')
    } else if (command === 'dashboard') {
      handleStart()
    }
  } catch (error) {
    if (error !== 'cancel') {
      handleError('操作失败，请重试', error)
    }
  }
}

const handleLogin = () => {
  try {
    router.push('/login')
  } catch (error) {
    handleError('页面跳转失败', error)
  }
}

const handleRegister = () => {
  try {
    router.push('/login?mode=register')
  } catch (error) {
    handleError('页面跳转失败', error)
  }
}

const handleStart = () => {
  try {
    if (!isAuthenticated.value) {
      router.push('/login')
      return
    }

    const targetRoute = store.state.isAdmin ? '/admin' : '/user'
    router.push(targetRoute)
  } catch (error) {
    handleError('页面跳转失败，请重试', error)
  }
}

const handleKeydown = (event) => {
  if (event.key === 'Enter' || event.key === ' ') {
    const target = event.target
    if (target.classList.contains('user-dropdown')) {
      event.preventDefault()
    }
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

</script>

<style scoped>

.index-layout {
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
  color: #061b31;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
  line-height: 1.4;
  background-color: #ffffff;
}

.container {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 24px;
  box-sizing: border-box;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  background-color: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid #e5edf5;
}

.header .container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  height: 64px;
}

.logo {
  color: #533afd;
  font-size: 22px;
  font-weight: 400;
  margin: 0;
  letter-spacing: -0.22px;
}

.auth-section {
  display: flex;
  align-items: center;
}

.auth-buttons {
  display: flex;
  gap: 12px;
}

.ghost-btn {
  background: transparent;
  border: 1px solid #b9b9f9;
  color: #533afd;
  font-weight: 400;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.ghost-btn:hover {
  background: rgba(83, 58, 253, 0.05);
  border-color: #533afd;
}

.primary-btn {
  background-color: #533afd;
  border-color: #533afd;
  color: #ffffff;
  font-weight: 400;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.primary-btn:hover {
  background-color: #4434d4;
  border-color: #4434d4;
  color: #ffffff;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #061b31;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
  gap: 8px;
}

.user-dropdown:hover {
  background-color: rgba(83, 58, 253, 0.05);
}

.username {
  font-weight: 400;
  font-size: 14px;
  color: #061b31;
}

.main-content {
  flex: 1;
}

.hero-section {
  background: #ffffff;
  padding: 120px 0 80px;
  position: relative;
}

.hero-section .container {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.hero-content {
  max-width: 600px;
}

.hero-title {
  font-size: 56px;
  font-weight: 300;
  line-height: 1.03;
  margin-bottom: 24px;
  color: #061b31;
  letter-spacing: -1.4px;
}

.hero-description {
  font-size: 18px;
  font-weight: 300;
  margin-bottom: 40px;
  color: #64748d;
  line-height: 1.4;
}

.cta-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.start-btn {
  background-color: #533afd;
  border-color: #533afd;
  color: #ffffff;
  padding: 14px 32px;
  font-size: 16px;
  font-weight: 400;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.start-btn:hover {
  background-color: #4434d4;
  border-color: #4434d4;
  color: #ffffff;
  box-shadow: rgba(50, 50, 93, 0.25) 0px 30px 45px -30px, rgba(0, 0, 0, 0.1) 0px 18px 36px -18px;
}

.features-section {
  padding: 80px 0;
  background-color: #ffffff;
}

.section-title {
  font-size: 32px;
  font-weight: 300;
  line-height: 1.1;
  margin-bottom: 48px;
  color: #061b31;
  text-align: center;
  letter-spacing: -0.64px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
}

.feature-card {
  background: #ffffff;
  border: 1px solid #e5edf5;
  padding: 32px 24px;
  border-radius: 6px;
  transition: all 0.3s ease;
  text-align: center;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.feature-card:hover {
  box-shadow: rgba(50, 50, 93, 0.25) 0px 30px 45px -30px, rgba(0, 0, 0, 0.1) 0px 18px 36px -18px;
  border-color: #b9b9f9;
}

.feature-icon {
  color: #533afd;
  margin-bottom: 20px;
}

.feature-card h3 {
  font-size: 22px;
  font-weight: 300;
  line-height: 1.1;
  margin-bottom: 12px;
  color: #061b31;
  letter-spacing: -0.22px;
}

.feature-card p {
  color: #64748d;
  line-height: 1.4;
  font-size: 16px;
  font-weight: 300;
}

.footer {
  background-color: #1c1e54;
  color: rgba(255, 255, 255, 0.7);
  padding: 64px 0 32px;
  margin-top: auto;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
  margin-bottom: 48px;
}

.footer-section h3 {
  margin-bottom: 16px;
  color: #ffffff;
  font-weight: 300;
  font-size: 22px;
  letter-spacing: -0.22px;
}

.footer-section h4 {
  margin-bottom: 16px;
  color: #ffffff;
  font-weight: 400;
  font-size: 14px;
}

.footer-section p {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-weight: 300;
  line-height: 1.4;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-section ul li {
  margin-bottom: 8px;
}

.footer-section a {
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  transition: color 0.2s ease;
  font-size: 14px;
  font-weight: 400;
}

.footer-section a:hover {
  color: #ffffff;
}

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 24px;
  text-align: center;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  font-weight: 300;
}

.footer-bottom p {
  margin: 0;
}

.icp-info {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.icp-info a {
  color: rgba(255, 255, 255, 0.5);
  text-decoration: none;
  transition: color 0.2s ease;
  font-size: 12px;
  font-weight: 300;
}

.icp-info a:hover {
  color: rgba(255, 255, 255, 0.8);
}

@media (max-width: 1024px) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .footer-content {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .container {
    padding: 0 16px;
  }

  .header .container {
    padding: 12px 16px;
    height: 56px;
  }

  .logo {
    font-size: 18px;
  }

  .hero-section {
    padding: 80px 0 60px;
  }

  .hero-title {
    font-size: 32px;
    letter-spacing: -0.64px;
  }

  .hero-description {
    font-size: 16px;
  }

  .cta-buttons {
    flex-direction: column;
    align-items: center;
  }

  .features-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .section-title {
    font-size: 26px;
    letter-spacing: -0.26px;
  }

  .footer-content {
    grid-template-columns: 1fr;
    text-align: center;
  }
}
</style>
