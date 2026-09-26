<template>
  <div class="user-cardkey-management">
    <el-card class="cardkey-card" shadow="never" :body-style="{ padding: '0' }">

      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="搜索卡密" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="6" :md="4" class="mb-10">
            <el-select v-model="specificationFilter" placeholder="商品规格" clearable @change="handleSearch"
              style="width: 100%" size="default">
              <el-option v-for="spec in specifications" :key="spec.id" :label="spec.name" :value="spec.id" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="6" :md="4" class="mb-10">
            <el-select v-model="statusFilter" placeholder="卡密状态" clearable @change="handleSearch"
              style="width: 100%" size="default">
              <el-option label="全部" :value="null" />
              <el-option label="未使用" value="未使用" />
              <el-option label="已使用" value="已使用" />
              <el-option label="已禁用" value="已禁用" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="24" :md="10" class="button-group">
            <el-button type="success" @click="handleExport">导出卡密</el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="cardKeys" style="width: 100%" stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="100" align="center">
            <template #default="scope">
              <span class="id-display">{{ formatId(scope.row.id) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="cardKey" label="卡密代码" min-width="200" align="left" :show-overflow-tooltip="true">
            <template #default="scope">
              <span class="cardkey-code" @click="copyCardKey(scope.row.cardKey)" style="cursor: pointer;">{{
                maskCardKey(scope.row.cardKey) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120" align="center">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品" width="130" align="left" :show-overflow-tooltip="true" />
          <el-table-column prop="specificationName" label="规格" width="120" align="left" :show-overflow-tooltip="true" />
          <el-table-column prop="userEmail" label="使用用户" width="160" align="left" :show-overflow-tooltip="true">
            <template #default="scope">
              <span>{{ scope.row.userEmail || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="activateTime" label="使用时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              <span class="time-text">{{ scope.row.activateTime || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="expireTime" label="过期时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              <span class="time-text" :class="{ 'text-danger': isExpired(scope.row.expireTime) && scope.row.status === '已使用' }">
                {{ scope.row.expireTime || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              <span class="time-text">{{ scope.row.createTime }}</span>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无卡密数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/services/api.js'
import { maskCardKey, formatDateTime } from '@/utils/utils.js'

const loading = ref(false)

onMounted(() => {
  loadCardKeys()
  loadSpecifications()
})

const cardKeys = ref([])
const specifications = ref([])
const searchQuery = ref('')
const specificationFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const formatId = (id) => {
  if (!id) return ''
  const idStr = id.toString()
  if (idStr.length > 8) {
    return `${idStr.substring(0, 8)}...`
  }
  return idStr
}

const getStatusTagType = (status) => {
  const typeMap = {
    '未使用': 'success',
    '已使用': 'info',
    '已禁用': 'danger'
  }
  return typeMap[status] || 'info'
}

const isExpired = (expireTime) => {
  if (!expireTime) return false
  return new Date(expireTime) < new Date()
}

const loadCardKeys = async () => {
  loading.value = true
  try {
    const response = await api.admin.getCardKeyListWithDetails({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value,
      specId: specificationFilter.value,
      status: statusFilter.value
    })

    if (response && response.data) {
      const cardKeyList = response.data.records || response.data.content || response.data || []

      const newCardKeys = cardKeyList.map(cardKey => ({
        id: cardKey.id,
        cardKey: cardKey.cardKey,
        status: cardKey.status,
        productName: cardKey.productName || '-',
        specificationName: cardKey.specificationName || '-',
        userEmail: cardKey.userEmail || '',
        activateTime: cardKey.activateTime ? formatDateTime(cardKey.activateTime) : '',
        expireTime: cardKey.expireTime ? formatDateTime(cardKey.expireTime) : '',
        createTime: cardKey.createdAt ? formatDateTime(cardKey.createdAt) : '',
        updatedAt: cardKey.updatedAt ? formatDateTime(cardKey.updatedAt) : ''
      }))

      cardKeys.value = newCardKeys
      total.value = response.data.total || response.data.totalElements || cardKeyList.length
    } else {
      cardKeys.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载卡密数据失败')
    cardKeys.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadCardKeys()
}

const copyCardKey = async (cardKey) => {
  try {
    await navigator.clipboard.writeText(cardKey)
    ElMessage.success('卡密已复制到剪贴板')
  } catch (err) {
    const textArea = document.createElement('textarea')
    textArea.value = cardKey
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('卡密已复制到剪贴板')
  }
}

const handleExport = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要导出卡密吗？${specificationFilter.value ? '将导出当前筛选规格的所有卡密。' : '将导出所有卡密。'}`,
      '确认导出',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    const loadingMessage = ElMessage({
      message: '正在获取卡密数据...',
      type: 'info',
      duration: 0
    })

    try {
      const response = await api.admin.getCardKeyListWithDetails({
        page: 1,
        size: 10000,
        keyword: searchQuery.value,
        specId: specificationFilter.value,
        status: statusFilter.value
      })

      if (response && response.data) {
        const cardKeyList = response.data.records || response.data.content || response.data || []

        if (cardKeyList.length === 0) {
          loadingMessage.close()
          ElMessage.warning('没有找到可导出的卡密数据')
          return
        }

        const cardKeyContent = cardKeyList.map(cardKey => cardKey.cardKey).join('\n')

        const timestamp = new Date().toISOString().slice(0, 19).replace(/:/g, '-')
        const specName = specifications.value.find(spec => spec.id === specificationFilter.value)?.name || '全部'
        const fileName = `卡密导出_${specName}_${timestamp}.txt`

        const blob = new Blob([cardKeyContent], { type: 'text/plain;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = fileName
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)

        loadingMessage.close()
        ElMessage.success(`成功导出 ${cardKeyList.length} 个卡密`)
      } else {
        loadingMessage.close()
        ElMessage.error(response?.message || '获取卡密数据失败')
      }
    } catch (error) {
      loadingMessage.close()
      ElMessage.error(error.message || '导出卡密失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '导出卡密失败')
    }
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadCardKeys()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadCardKeys()
}

const loadSpecifications = async () => {
  try {
    const response = await api.admin.getSpecificationDTOs()
    if (response && response.data) {
      specifications.value = response.data.map(spec => ({
        id: spec.id,
        name: spec.productName ? `${spec.productName} - ${spec.name}` : spec.name
      }))
    }
  } catch (error) {
    // eslint-disable-next-line no-empty
  }
}

</script>

<style scoped>
.user-cardkey-management {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.cardkey-card {
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.search-bar {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #e5edf5;
}

.mb-10 {
  margin-bottom: 10px;
}

.table-container {
  padding: 0;
}

.id-display {
  font-family: 'SourceCodePro', 'Courier New', monospace;
  font-size: 12px;
  color: #64748d;
  font-weight: 500;
}

.cardkey-code {
  font-family: 'SourceCodePro', 'Courier New', monospace;
  font-weight: 500;
  color: #533afd;
  letter-spacing: 0.5px;
  font-size: 13px;
}

.cardkey-code:hover {
  color: #4434d4;
}

.remaining-count {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 32px;
}

.text-danger {
  color: #f56c6c;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 0;
  padding: 16px 20px;
  border-top: 1px solid #e5edf5;
}

.button-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}
</style>
