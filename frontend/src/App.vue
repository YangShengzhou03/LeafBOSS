<template>
  <div id="app" v-loading="isLoading" :element-loading-text="loadingText"
    :element-loading-background="loadingBackground">
    <div v-if="hasError" class="app-error">
      <el-result icon="error" title="应用加载失败" :sub-title="errorMessage">
        <template #extra>
          <el-button type="primary" @click="retryLoading">重试</el-button>
        </template>
      </el-result>
    </div>
    <router-view v-else />
  </div>
</template>

<script setup>
import { ref, onMounted, onErrorCaptured } from 'vue'
import store from './utils/store.js'

const isLoading = ref(true)
const hasError = ref(false)
const errorMessage = ref('')
const loadingText = ref('正在加载应用...')
const loadingBackground = ref('rgba(255, 255, 255, 0.8)')

onErrorCaptured((err) => {
  hasError.value = true
  errorMessage.value = err.message || '未知错误'
  return false
})

const retryLoading = async () => {
  hasError.value = false
  isLoading.value = true

  try {
    await store.init()
    isLoading.value = false
  } catch (error) {
    hasError.value = true
    errorMessage.value = error.message || '初始化失败'
    isLoading.value = false
  }
}

onMounted(async () => {
  try {
    if (typeof store.init === 'function') {
      await store.init()
    }
    isLoading.value = false
  } catch (error) {
    hasError.value = true
    errorMessage.value = error.message || '初始化失败'
    isLoading.value = false
  }
})
</script>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html {
  font-size: 16px;
  line-height: 1.5;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #303133;
  background-color: #f8fafc;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-error {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
}

:root {
  --el-color-primary: #533afd;
  --el-color-primary-light-3: #7c6bfe;
  --el-color-primary-light-5: #a59bfe;
  --el-color-primary-light-7: #cec9fe;
  --el-color-primary-light-8: #e0ddfe;
  --el-color-primary-light-9: #f0efff;
  --el-color-primary-dark-2: #4434d4;
}

/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background-color: rgba(100, 116, 141, 0.25);
  border-radius: 3px;
  transition: background-color 0.2s ease;
}

::-webkit-scrollbar-thumb:hover {
  background-color: rgba(100, 116, 141, 0.4);
}

/* Firefox 滚动条 */
* {
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 141, 0.25) transparent;
}
</style>