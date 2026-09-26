<template>
  <el-watermark :content="watermarkText" :font="{ color: 'rgba(0, 0, 0, 0.15)', fontSize: 16 }" :z-index="9"
    :rotate="-15" :gap="[100, 100]">
    <div class="admin-layout">
      <header class="admin-header">
        <div class="header-left">
          <el-button class="mobile-toggle" @click="showDrawer = true" v-if="isMobile">
            <el-icon><Menu /></el-icon>
          </el-button>
          <h1 class="logo">LEAF-BOSS</h1>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="user-info">
              <el-avatar :size="32" :src="userAvatar">
                <el-icon>
                  <User />
                </el-icon>
              </el-avatar>
              <span class="username">{{ store.state.user?.username || '用户' }}</span>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
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
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, ArrowDown, Menu, HomeFilled, List, Check, Key, SwitchButton, Lock, ShoppingCart } from '@element-plus/icons-vue'
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

const isAgent = computed(() => store.state.user?.role === 'agent')

const menuItems = computed(() => {
  const items = [
    { index: '/user', icon: HomeFilled, title: '我的主页' }
  ]
  if (isAgent.value) {
    items.push({
      index: 'cardkey-management', icon: Key, title: '卡密操作',
      children: [
        { index: '/user/card-keys', icon: List, title: '卡密列表' },
        { index: '/user/card-verify', icon: Check, title: '卡密验证' },
        { index: '/user/card-generate', icon: Key, title: '卡密生成' }
      ]
    })
    items.push({ index: '/user/agent-authorizations', icon: Lock, title: '授权列表' })
  }
  items.push({ index: '/user/card-redeem', icon: ShoppingCart, title: '商品兑换' })
  return items
})

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
    if (command === 'profile') {
      router.push('/user/profile')
      return
    }
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
    }
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error.response?.data?.message || error.message || '操作失败'
      ElMessage.error(msg)
    }
  }
}
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

.admin-menu :deep(.el-menu-item) {
  margin: 2px 4px;
  border-radius: 8px !important;
}

.admin-menu :deep(.el-sub-menu__title) {
  margin: 2px 4px;
  border-radius: 8px !important;
}

.admin-menu :deep(.el-menu-item:hover),
.admin-menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(83, 58, 253, 0.06) !important;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background-color: rgba(83, 58, 253, 0.1) !important;
}

.admin-menu :deep(.el-sub-menu .el-menu) {
  margin: 4px 8px;
  border-radius: 8px;
  background: transparent !important;
}

.admin-menu :deep(.el-sub-menu .el-menu-item) {
  margin: 1px 4px;
  border-radius: 6px !important;
  background-color: transparent !important;
}

.admin-menu :deep(.el-sub-menu .el-menu-item:hover) {
  background-color: rgba(83, 58, 253, 0.06) !important;
}

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
