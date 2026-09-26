import { reactive } from 'vue'
import * as utils from './utils.js'
import api from '@/services/api.js'

const state = reactive({
  user: null,
  isAuthenticated: utils.isLoggedIn(),
  loading: false,
  isAdmin: utils.getToken('admin') != null,
  storageInfo: {
    totalStorageGB: 0,
    usedStorageGB: 0
  }
})

const store = {
  state,

  setUser(user, role = null) {
    if (user) {
      state.user = user
      state.isAuthenticated = true
      // Only update isAdmin if role is explicitly provided
      if (role) {
        state.isAdmin = (role === 'admin')
      }
      if (user.storageInfo) {
        state.storageInfo = user.storageInfo
      }
    }
  },

  clearUser() {
    state.user = null
    state.isAuthenticated = false
    state.isAdmin = false
    utils.removeToken()
    state.storageInfo = {
      totalStorageGB: 0,
      usedStorageGB: 0
    }
  },

  clearAllStorage() {
    // 清除所有 localStorage
    localStorage.clear()
    // 清除所有 sessionStorage
    sessionStorage.clear()
    // 清除所有 cookies
    document.cookie.split(';').forEach(cookie => {
      const [name] = cookie.split('=')
      if (name.trim()) {
        document.cookie = `${name.trim()}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`
      }
    })
  },

  tokenRole() {
    return state.isAdmin ? 'admin' : 'user'
  },

  updateStorageInfo(storageInfo) {
    if (storageInfo) {
      state.storageInfo = storageInfo
    }
  },

  async login(credentials, silent = false) {
    return this._doLogin(credentials, silent, 'admin')
  },

  async userLogin(credentials, silent = false) {
    return this._doLogin(credentials, silent, 'user')
  },

  async _doLogin(credentials, silent, role) {
    state.loading = true
    try {
      const loginApi = role === 'admin' ? api.admin.login : api.user.login
      const response = await loginApi(credentials, silent)

      if (response && response.code === 200 && response.data) {
        const { token, user } = response.data

        if (token) {
          utils.saveToken(token, role)
        }

        if (user) {
          if (user.status === 'inactive') {
            this.clearUser()
            return { success: false, message: '账号已被禁用，请联系管理员' }
          }
          this.setUser(user)
          // Backend may not return role; derive from login context
          state.isAdmin = (role === 'admin')
        } else {
          await this.fetchCurrentUser(role)
        }

        return { success: true, message: response.message || '登录成功', user }
      }

      return { success: false, message: response?.message || '登录失败' }
    } catch (error) {
      return { success: false, message: error.message || '登录失败，请检查网络连接' }
    } finally {
      state.loading = false
    }
  },

  async fetchCurrentUser(role = 'admin') {
    if (!utils.isLoggedIn(role)) {
      this.clearUser()
      return
    }

    const fetchApi = role === 'admin' ? api.user.getCurrentUser : api.user.userGetCurrentUser
    const response = await fetchApi()

    if (response && response.code === 200 && response.data) {
      if (response.data.status === 'inactive') {
        this.clearUser()
        return
      }
      this.setUser(response.data, role)

      if (role === 'admin' && response.data.storageInfo) {
        this.updateStorageInfo(response.data.storageInfo)
      }
    }
  },

  async register(userData) {
    state.loading = true
    try {
      const response = await api.user.register(userData)
      if (response && response.code === 200) {
        const loginResult = await this.login({
          email: userData.email,
          password: userData.password
        }, true)
        return loginResult.success
          ? loginResult
          : { success: false, message: '注册成功但账号被禁用，请联系管理员' }
      }
      return { success: false, message: response?.message || '注册失败' }
    } catch (error) {
      return { success: false, message: error.message || '注册失败，请检查网络连接' }
    } finally {
      state.loading = false
    }
  },

  async adminRegister(userData) {
    state.loading = true
    try {
      const response = await api.admin.register(userData)
      if (response && response.code === 200) {
        return { success: true, message: response.message || '注册成功，请等待管理员审核' }
      }
      return { success: false, message: response?.message || '注册失败' }
    } catch (error) {
      return { success: false, message: error.message || '注册失败，请检查网络连接' }
    } finally {
      state.loading = false
    }
  },

  async resetPassword(data) {
    state.loading = true
    try {
      const response = await api.user.resetPassword(data)
      return { success: !!(response && response.code === 200), message: response?.message || '密码重置失败' }
    } catch (error) {
      return { success: false, message: error.message || '密码重置失败，请检查网络连接' }
    } finally {
      state.loading = false
    }
  },

  async checkAuthStatus() {
    const role = this.tokenRole()
    const token = utils.getToken(role)
    if (!token) {
      this.clearUser()
      return false
    }

    try {
      const decoded = utils.parseJWT(token)

      if (!decoded || !decoded.exp || decoded.exp * 1000 < Date.now()) {
        this.clearUser()
        return false
      }

      if (!state.user) {
        await this.fetchCurrentUser(role)
      }

      return true
    } catch (error) {
      this.clearUser()
      return false
    }
  },

  async fetchStorageInfo() {
    if (!utils.isLoggedIn()) {
      return null
    }

    try {
      const response = await api.user.getStorageInfo()
      const storageData = response.data || response

      if (storageData) {
        state.storageInfo = {
          totalStorageGB: storageData.storageQuota ? (storageData.storageQuota / (1024 * 1024 * 1024)) : 0,
          usedStorageGB: storageData.usedStorage ? (storageData.usedStorage / (1024 * 1024 * 1024)) : 0,
          availableStorageGB: storageData.availableStorage ? (storageData.availableStorage / (1024 * 1024 * 1024)) : 0,
          usagePercentage: storageData.usagePercentage || 0
        }
      }
      return storageData
    } catch (error) {
      return null
    }
  },

  async logout() {
    try {
      await api.user.logout()
    } catch (error) {
      // ignore
    } finally {
      this.clearUser()
      this.clearAllStorage()
    }
  },

  async init() {
    const role = utils.isLoggedIn('admin') ? 'admin' : 'user'
    if (utils.isLoggedIn(role)) {
      try {
        await this.fetchCurrentUser(role)
        if (role === 'admin') await this.fetchStorageInfo()
        return true
      } catch (error) {
        this.clearUser()
        return false
      }
    } else {
      this.clearUser()
      return false
    }
  }
}

export default store
