import axios from 'axios'
import { getToken, saveToken, removeToken } from './utils.js'
import { ElMessage } from 'element-plus'
import router from '@/route'
import store from './store.js'

const Server = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || '',
  timeout: 10000,
  paramsSerializer: params => {
    const parts = []
    for (const [key, value] of Object.entries(params)) {
      if (value === null || value === undefined || value === '') continue
      if (Array.isArray(value)) {
        for (const v of value) {
          parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(v)}`)
        }
      } else {
        parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      }
    }
    return parts.join('&')
  }
})

Server.interceptors.request.use(config => {
  const role = store.state.isAdmin ? 'admin' : 'user';
  const token = getToken(role) || getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (config.url === '/api/auth/login' && config.autoLogin) {
    config._autoLogin = true
  }
  return config
})

Server.interceptors.response.use(
  response => {
    // 滑动续期：服务端返回新 token 时替换本地存储
    const newToken = response.headers['new-token']
    if (newToken) {
      const role = store.state.isAdmin ? 'admin' : 'user'
      saveToken(newToken, role)
    }

    if (response.data === null || response.data === undefined) {
      return { data: null, code: 500, message: '响应数据为空' }
    }

    if (response.data && response.data.code !== undefined) {
      if (response.data.code === 200) {
        return response.data
      } else {
        if (response.data.message && (response.data.message.includes('Token无效') ||
          response.data.message.includes('Token过期') ||
          response.data.message.includes('未授权访问') ||
          response.data.message.includes('账号已被禁用') ||
          response.data.message.includes('账号已被禁用或不存在'))) {
          const isLoginPage = router.currentRoute.value.path === '/login'
          if (!response.config._autoLogin && !response.config.url.includes('/api/auth/login') && !isLoginPage) {
            handleTokenExpiration(true, response.data.message)
          }
          return Promise.reject(new Error(response.data.message || '登录已过期，请重新登录'))
        }

        return Promise.reject(new Error(response.data.message || '请求失败'))
      }
    }
    return response
  },
  error => {
    if (!error.response) {
      return Promise.reject(new Error('网络连接失败，请检查网络连接'))
    }

    const status = error.response.status
    const url = error.config?.url || ''

    switch (status) {
      case 401: {
        const errorMessage = error.response.data?.message || '登录已过期，请重新登录'
        const isLoginPage = router.currentRoute.value.path === '/login'
        if (!error.config._autoLogin && !error.config.url.includes('/api/auth/login') && !isLoginPage) {
          handleTokenExpiration(true, errorMessage)
        }
        return Promise.reject(new Error(errorMessage))
      }
      case 403:
        return Promise.reject(new Error('权限不足，无法访问该资源'))
      case 404:
        if (url.includes('/api/card-keys/')) {
          return Promise.reject(new Error('卡密不存在或参数错误'))
        }
        return Promise.reject(new Error('请求的资源不存在'))
      case 500:
        return Promise.reject(new Error('服务器内部错误，请联系管理员'))
      default:
        return Promise.reject(new Error('请求失败，请稍后重试'))
    }
  }
)

function handleTokenExpiration(showMessage = true, message = '登录已过期，请重新登录') {
  removeToken()

  if (store && store.clearUser) {
    store.clearUser()
  }

  if (showMessage) {
    ElMessage.error(message)
  }

  const currentPath = router.currentRoute.value.path
  if (currentPath !== '/login' && !currentPath.includes('/login')) {
    router.replace('/login')
  }
}

const http = {
  get: (url, params = {}) => Server.get(url, { params }),
  post: (url, data = {}, config = {}) => Server.post(url, data, config),
  put: (url, data = {}, config = {}) => Server.put(url, data, config),
  delete: (url, params = {}) => Server.delete(url, { params })
}

export default http