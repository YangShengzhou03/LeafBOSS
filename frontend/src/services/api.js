import Server from '../utils/Server'

const AdminService = {
  login(data, autoLogin = false) {
    return Server.post('/api/auth/login', data, { autoLogin })
  },

  register(data) {
    return Server.post('/api/auth/register', data)
  },

  sendResetCode(data) {
    return Server.post('/api/admins/send-reset-code', data)
  },

  resetPassword(data) {
    return Server.post('/api/admins/reset-password', data)
  },

  getDashboardStats() {
    return Server.get('/api/admin/stats')
  },

  getTodaySalesDistribution() {
    return Server.get('/api/admin/today-sales-distribution')
  },

  getDailyRevenueTrend(params) {
    return Server.get('/api/admin/daily-revenue-trend', { days: params?.days || 30 })
  },

  getUserList(params) {
    return Server.get('/api/admins', {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      status: params.status
    })
  },

  getLogList(params) {
    const queryParams = {
      page: params.page || 1,
      size: params.size || 10,
      startDate: params.startDate,
      endDate: params.endDate
    }
    if (params.operationTypes) {
      queryParams.operationTypes = params.operationTypes
    } else if (params.operationType) {
      queryParams.operationType = params.operationType
    }
    return Server.get('/api/operation-logs', queryParams)
  },

  clearLogs() {
    return Server.delete('/api/operation-logs')
  },

  getCardKeyListWithDetails(params) {
    return Server.get('/api/card-keys/with-details', {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      specId: params.specId,
      status: params.status
    })
  },

  batchCreateCardKeys(data) {
    return Server.post('/api/card-keys/batch', data)
  },

  toggleCardKeyStatus(cardKey, status) {
    return Server.post('/api/card-keys/status', { cardKey, status })
  },

  deleteCardKey(cardKey) {
    return Server.delete(`/api/card-keys/by-card-key/${cardKey}`)
  },

  batchDeleteUsedCardKeys() {
    return Server.delete('/api/card-keys/batch-delete-used')
  },

  getProductList(params) {
    return Server.get('/api/products', {
      page: params.page || 1,
      size: params.size || 10,
      name: params.keyword || params.name || null,
      status: params.status
    })
  },

  createProduct(data) {
    return Server.post('/api/products', data)
  },

  editProduct(id, data) {
    return Server.put(`/api/products/${id}`, data)
  },

  deleteProduct(id) {
    return Server.delete(`/api/products/${id}`)
  },

  getSpecListDTO(params) {
    return Server.get('/api/specifications/dto/pagination', {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      productId: params.productId
    })
  },

  createSpec(data) {
    return Server.post('/api/specifications', data)
  },

  editSpec(id, data) {
    return Server.put(`/api/specifications/${id}`, data)
  },

  deleteSpec(id) {
    return Server.delete(`/api/specifications/${id}`)
  },

  getSpecificationDTOs() {
    return Server.get('/api/specifications/dto')
  },

  getCompanyList(params) {
    return Server.get('/api/companies', {
      page: params.page || 1,
      size: params.size || 10,
      name: params.name
    })
  },

  createCompany(data) {
    return Server.post('/api/companies', data)
  },

  editCompany(id, data) {
    return Server.put(`/api/companies/${id}`, data)
  },

  deleteCompany(id) {
    return Server.delete(`/api/companies/${id}`)
  },

  getBossReviewList(params) {
    return Server.get('/api/boss-reviews', {
      page: params.page || 1,
      size: params.size || 10,
      companyId: params.companyId,
      cardKey: params.cardKey,
      keyword: params.keyword
    })
  },

  createBossReview(data) {
    return Server.post('/api/boss-reviews', data)
  },

  deleteBossReview(id) {
    return Server.delete(`/api/boss-reviews/${id}`)
  },

  getCustomerUserList(params) {
    return Server.get('/api/users', {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      status: params.status
    })
  },

  updateCustomerUser(id, data) {
    return Server.put(`/api/users/${id}`, data)
  },

  deleteCustomerUser(id) {
    return Server.delete(`/api/users/${id}`)
  },

  resetCustomerUserPassword(data) {
    return Server.post('/api/users/reset-password', data)
  },

  verifyCardKey(cardKey) {
    return Server.post('/api/card-keys/verify', { cardKey })
  },

  getAgents() {
    return Server.get('/api/card-keys/agents')
  },

  // 代理授权管理（admin）
  getAgentAuthorizationList(params) {
    return Server.get('/api/agent-authorizations', {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      status: params.status
    })
  },
  createAgentAuthorization(data) {
    return Server.post('/api/agent-authorizations', data)
  },
  updateAgentAuthorization(id, data) {
    return Server.put(`/api/agent-authorizations/${id}`, data)
  },
  revokeAgentAuthorization(id) {
    return Server.put(`/api/agent-authorizations/${id}/revoke`)
  },
  restoreAgentAuthorization(id) {
    return Server.put(`/api/agent-authorizations/${id}/restore`)
  },
  deleteAgentAuthorization(id) {
    return Server.delete(`/api/agent-authorizations/${id}`)
  },

  getAuthorizationList(params) {
    return Server.get('/api/user-products', {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      status: params.status
    })
  },

  createAuthorization(data) {
    return Server.post('/api/user-products', data)
  },

  updateAuthorization(id, data) {
    return Server.put(`/api/user-products/${id}`, data)
  },

  revokeAuthorization(id) {
    return Server.put(`/api/user-products/${id}/revoke`)
  },

  restoreAuthorization(id) {
    return Server.put(`/api/user-products/${id}/restore`)
  },

  deleteAuthorization(id) {
    return Server.delete(`/api/user-products/${id}`)
  },

  getNotices() {
    return Server.get('/api/notices')
  },

  createNotice(data) {
    return Server.post('/api/notices', data)
  },

  updateNotice(id, data) {
    return Server.put(`/api/notices/${id}`, data)
  },

  deleteNotice(id) {
    return Server.delete(`/api/notices/${id}`)
  },

  getFeedbacks() {
    return Server.get('/api/feedbacks')
  },

  deleteFeedback(id) {
    return Server.delete(`/api/feedbacks/${id}`)
  }
}

const UserService = {
  login(data, autoLogin = false) {
    return Server.post('/api/user-auth/login', data, { autoLogin })
  },

  logout() {
    return Server.post('/api/user-auth/logout')
  },

  register(data) {
    return Server.post('/api/user-auth/register', data)
  },

  verifyCardKey(cardKey) {
    return Server.post('/api/card-keys/verify', { cardKey })
  },

  redeemCardKey(cardKey) {
    return Server.post('/api/card-keys/redeem', { cardKey })
  },

  createUser(data) {
    return Server.post('/api/admins', data)
  },

  deleteUser(id) {
    return Server.delete(`/api/admins/${id}`)
  },

  updateUser(id, data) {
    return Server.put(`/api/admins/${id}`, data)
  },

  adminResetPassword(data) {
    return Server.post('/api/admins/admin-reset-password', data)
  },

  sendResetCode(data) {
    return Server.post('/api/admins/send-reset-code', data)
  },

  userSendResetCode(data) {
    return Server.post('/api/user-auth/send-reset-code', data)
  },

  userSendRegisterCode(data) {
    return Server.post('/api/user-auth/send-register-code', data)
  },

  resetPassword(data) {
    return Server.post('/api/admins/reset-password', data)
  },

  userResetPassword(data) {
    return Server.post('/api/user-auth/reset-password', data)
  },

  getCurrentUser() {
    return Server.get('/api/auth/me')
  },

  userGetCurrentUser() {
    return Server.get('/api/user-auth/me')
  },

  updateMyProfile(data) {
    return Server.put('/api/user-auth/me', data)
  },

  sendEmailChangeCode(email) {
    return Server.post('/api/user-auth/send-email-change-code', { email })
  },

  getCardKeyList(params) {
    return Server.get('/api/card-keys/with-details', {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      specId: params.specId,
      status: params.status
    })
  },

  getRemainingGenerateCount(params) {
    return Server.get('/api/card-keys/remaining-generate-count', params)
  },

  userGetMyProducts() {
    return Server.get('/api/user-products/user/me')
  },

  getAuthorizedProducts() {
    return Server.get('/api/agent-authorizations/products')
  },

  getPublicNotices(productName) {
    return Server.get('/api/notices/public', { productName })
  },

  createCustomerUser(data) {
    return Server.post('/api/users', data)
  },

  updateUserInfo(data) {
    return Server.put('/api/auth/me', data)
  },

  getStorageInfo() {
    return Server.get('/api/auth/storage')
  }
}

export default {
  admin: AdminService,
  user: UserService
}