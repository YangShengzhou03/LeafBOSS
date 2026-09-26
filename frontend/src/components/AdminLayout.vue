<template>
  <el-watermark :content="watermarkText" :font="{ color: 'rgba(0, 0, 0, 0.15)', fontSize: 16 }" :z-index="9"
    :rotate="-15" :gap="[100, 100]">
    <div class="admin-layout">
      <header class="admin-header">
        <div class="header-left">
          <el-button class="mobile-toggle" @click="showDrawer = true" v-if="isMobile">
            <el-icon><Menu /></el-icon>
          </el-button>
          <h1 class="logo">LEAF-BOSS 管理后台</h1>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="user-info">
              <el-avatar :size="32" :src="userAvatar">
                <el-icon>
                  <User />
                </el-icon>
              </el-avatar>
              <span class="username">{{ store.state.user?.username || '管理员' }}</span>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="admin-container">
        <aside class="admin-sidebar" v-if="!isMobile">
          <el-menu :default-active="activeMenu" class="admin-menu" router unique-opened
            background-color="#ffffff" text-color="#061b31" active-text-color="#533afd">
            <template v-for="item in menuItems" :key="item.index">
              <el-sub-menu v-if="item.children" :index="item.index">
                <template #title>
                  <el-icon><component :is="item.icon" /></el-icon>
                  <span>{{ item.title }}</span>
                </template>
                <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index">
                  <el-icon><component :is="child.icon" /></el-icon>
                  <template #title>{{ child.title }}</template>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else :index="item.index">
                <el-icon><component :is="item.icon" /></el-icon>
                <template #title>{{ item.title }}</template>
              </el-menu-item>
            </template>
          </el-menu>
        </aside>

        <el-drawer v-model="showDrawer" direction="ltr" size="260px" :with-header="false" class="mobile-drawer"
          :body-style="{ padding: 0, backgroundColor: '#ffffff' }" v-if="isMobile">
          <div class="drawer-header">
            <h2 class="drawer-logo">LEAF-BOSS</h2>
          </div>
          <el-menu :default-active="activeMenu" class="admin-menu" router unique-opened @select="showDrawer = false"
            background-color="#ffffff" text-color="#061b31" active-text-color="#533afd">
            <template v-for="item in menuItems" :key="item.index">
              <el-sub-menu v-if="item.children" :index="item.index">
                <template #title>
                  <el-icon><component :is="item.icon" /></el-icon>
                  <span>{{ item.title }}</span>
                </template>
                <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index">
                  <el-icon><component :is="child.icon" /></el-icon>
                  <template #title>{{ child.title }}</template>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else :index="item.index">
                <el-icon><component :is="item.icon" /></el-icon>
                <template #title>{{ item.title }}</template>
              </el-menu-item>
            </template>
          </el-menu>
        </el-drawer>

        <main class="admin-main">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </main>
      </div>
    </div>
  </el-watermark>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, UserFilled, ArrowDown, Monitor, Document, Key, Goods, List, Operation, Check, Plus, Briefcase, OfficeBuilding, ChatDotRound, Menu, Stamp, Bell } from '@element-plus/icons-vue'
import store from '@/utils/store.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

const router = useRouter()
const route = useRoute()

const isMobile = useIsMobile()
const showDrawer = ref(false)

watch(isMobile, (mobile) => {
  if (!mobile) {
    showDrawer.value = false
  }
})

const activeMenu = computed(() => route.path)

const menuItems = [
  { index: '/admin', icon: Monitor, title: '管理员仪表盘' },
  {
    index: 'personnel-management', icon: User, title: '用户操作',
    children: [
      { index: '/admin/admins', icon: UserFilled, title: '管理员列表' },
      { index: '/admin/users', icon: User, title: '用户列表' },
      { index: '/admin/agent-authorizations', icon: Stamp, title: '代理授权' },
      { index: '/admin/authorizations', icon: Document, title: '用户授权' }
    ]
  },
  {
    index: 'product-management', icon: Goods, title: '商品操作',
    children: [
      { index: '/admin/products', icon: List, title: '商品列表' },
      { index: '/admin/product-specs', icon: Operation, title: '规格管理' }
    ]
  },
  {
    index: 'card-management', icon: Key, title: '卡密操作',
    children: [
      { index: '/admin/card-keys', icon: List, title: '卡密列表' },
      { index: '/admin/card-verify', icon: Check, title: '卡密验证' },
      { index: '/admin/card-generate', icon: Plus, title: '卡密生成' }
    ]
  },
  {
    index: 'jobs-management', icon: Briefcase, title: '海投操作',
    children: [
      { index: '/admin/jobs/companies', icon: OfficeBuilding, title: '公司操作' },
      { index: '/admin/jobs/boss-reviews', icon: ChatDotRound, title: '评论操作' }
    ]
  },
  {
    index: 'product-operations', icon: Operation, title: '商品运营',
    children: [
      { index: '/admin/notices', icon: Bell, title: '通知管理' }
    ]
  },
  { index: '/admin/logs', icon: Document, title: '系统操作' }
]

const userAvatar = computed(() => {
  return store.state.user?.avatar || ''
})

const watermarkText = computed(() => {
  const user = store.state.user
  if (user?.email) {
    return user.email
  }
  return 'LEAF-BOSS'
})

const handleCommand = async (command) => {
  try {
    if (command === 'logout') {
      await ElMessageBox.confirm(
        '确定要退出登录吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      await store.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } else if (command === 'profile') {
      router.push('/admin/profile')
    }
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error.response?.data?.message || error.message || '操作失败'
      ElMessage.error(msg)
    }
  }
}

onMounted(async () => {
  try {
    if (!store.state.user) {
      await store.fetchCurrentUser()
    }
  } catch (error) {
    ElMessage.error(error.message || '初始化失败，请刷新页面重试')
  }
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.admin-header {
  height: 48px;
  background-color: #ffffff;
  color: #061b31;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: relative;
  z-index: 1001;
  border-bottom: 1px solid #e5edf5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-toggle {
  background: transparent;
  border: none;
  color: #061b31;
  font-size: 24px;
  padding: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: background-color 0.2s ease;
}

.mobile-toggle:hover {
  background-color: rgba(83, 58, 253, 0.06);
}

.header-left .logo {
  margin: 0;
  font-size: 20px;
  font-weight: 400;
  color: #533afd;
  letter-spacing: -0.2px;
}

.header-right .user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #061b31;
  padding: 6px 12px;
  border-radius: 6px;
  transition: background-color 0.2s ease;
}

.header-right .user-info:hover {
  background-color: rgba(83, 58, 253, 0.06);
}

.username {
  margin: 0 10px;
  font-weight: 400;
  font-size: 14px;
  color: #061b31;
}

.admin-container {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.admin-sidebar {
  width: 200px;
  background-color: #ffffff;
  border-right: 1px solid #e5edf5;
  transition: width 0.3s;
  flex-shrink: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}

/* 滚动条样式 */
.admin-sidebar::-webkit-scrollbar {
  width: 6px;
}

.admin-sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.admin-sidebar::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.admin-sidebar::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

.admin-menu {
  height: 100%;
  padding: 8px;
  border-right: none;
}

/* 菜单项圆角优化 */
.admin-menu :deep(.el-menu-item) {
  margin: 2px 4px;
  border-radius: 8px !important;
}

.admin-menu :deep(.el-sub-menu__title) {
  margin: 2px 4px;
  border-radius: 8px !important;
}

/* 悬停效果 */
.admin-menu :deep(.el-menu-item:hover),
.admin-menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(83, 58, 253, 0.06) !important;
}

/* 激活状态优化 */
.admin-menu :deep(.el-menu-item.is-active) {
  background-color: rgba(83, 58, 253, 0.1) !important;
}

/* 子菜单容器圆角 */
.admin-menu :deep(.el-sub-menu .el-menu) {
  margin: 4px 8px;
  border-radius: 8px;
  background: transparent !important;
}

/* 子菜单项样式 */
.admin-menu :deep(.el-sub-menu .el-menu-item) {
  margin: 1px 4px;
  border-radius: 6px !important;
  background-color: transparent !important;
}

/* 子菜单项悬停 */
.admin-menu :deep(.el-sub-menu .el-menu-item:hover) {
  background-color: rgba(83, 58, 253, 0.06) !important;
}

/* 子菜单项激活 */
.admin-menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background-color: rgba(83, 58, 253, 0.1) !important;
}

.mobile-drawer {
  --el-drawer-padding-primary: 0;
}

.drawer-header {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5edf5;
}

.drawer-logo {
  margin: 0;
  font-size: 18px;
  color: #533afd;
  font-weight: 400;
  letter-spacing: -0.18px;
}

.admin-main {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
  background-color: #f8fafc;
  width: 100%;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 1024px) {
  .admin-sidebar {
    width: 180px;
  }
}

@media (max-width: 768px) {
  .admin-header {
    height: 60px;
    padding: 0 16px;
  }

  .header-left .logo {
    font-size: 18px;
  }

  .username {
    display: none;
  }

  .admin-main {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .admin-header {
    padding: 0 12px;
  }

  .header-left .logo {
    font-size: 16px;
  }

  .admin-main {
    padding: 8px;
  }
}
</style>
