<template>
  <div class="admin-cardkey-management">
    <el-card class="cardkey-card" shadow="never" :body-style="{ padding: '0' }">

      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="搜索卡密或邮箱" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="6" :md="3" class="mb-10">
            <el-select v-model="specificationFilter" placeholder="商品规格" clearable @change="handleSearch"
              style="width: 100%" size="default">
              <el-option v-for="spec in specs" :key="spec.id" :label="spec.name" :value="spec.id" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="6" :md="3" class="mb-10">
            <el-select v-model="statusFilter" placeholder="卡密状态" clearable @change="handleSearch"
              style="width: 100%" size="default">
              <el-option label="全部" :value="null" />
              <el-option label="未使用" value="未使用" />
              <el-option label="已使用" value="已使用" />
              <el-option label="已禁用" value="已禁用" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="6" :md="3" class="mb-10" v-if="isAdmin">
            <el-select v-model="agentFilter" placeholder="代理商" clearable filterable @change="handleSearch"
              style="width: 100%" size="default">
              <el-option label="全部" :value="null" />
              <el-option v-for="agent in agents" :key="agent.id" :label="agent.username" :value="agent.id" />
            </el-select>
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
          <el-table-column prop="productSpec" label="商品规格" min-width="160" align="left" :show-overflow-tooltip="true">
            <template #default="scope">
              <span class="product-spec">{{ scope.row.productSpec || '未设置' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="activateTime" label="使用时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              <span class="time-text">{{ scope.row.activateTime || '未使用' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              <span class="time-text">{{ scope.row.createTime }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="180" fixed="right" align="center" v-if="isAdmin">
            <template #default="scope">
              <el-button link size="default" :type="scope.row.status === '已禁用' ? 'primary' : 'warning'"
                @click="handleToggleCardKey(scope.row)">
                {{ scope.row.status === '已禁用' ? '启用' : '禁用' }}
              </el-button>
              <el-popconfirm
                title="确定删除该卡密吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleDeleteCardKey(scope.row)">
                <template #reference>
                  <el-button link size="default" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/services/api.js'
import { maskCardKey, formatDateTime } from '@/utils/utils.js'
import store from '@/utils/store.js'

const loading = ref(false)
const isAdmin = computed(() => store.state.isAdmin)

onMounted(() => {
  loadCardKeys()
  loadSpecifications()
  loadAgents()
})

const cardKeys = ref([])
const specifications = ref([])
const agents = ref([])
const searchQuery = ref('')
const specificationFilter = ref('')
const statusFilter = ref('')
const agentFilter = ref(null)
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

const loadCardKeys = async () => {
  loading.value = true
  try {
    const response = await api.admin.getCardKeyListWithDetails({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value,
      specId: specificationFilter.value,
      status: statusFilter.value,
      agentId: agentFilter.value
    })

    if (response && response.data) {
      const cardKeyList = response.data.records || response.data.content || response.data || []

      const newCardKeys = cardKeyList.map(cardKey => ({
        id: cardKey.id,
        cardKey: cardKey.cardKey,
        status: cardKey.status,
        productSpec: cardKey.productName && cardKey.specificationName
          ? `${cardKey.productName} - ${cardKey.specificationName}`
          : '未设置',
        userEmail: cardKey.userEmail || '',
        activateTime: cardKey.activateTime ? formatDateTime(cardKey.activateTime) : '',
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
    ElMessage.error(error.message || '加载卡密数据失败，请检查网络连接')
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

const handleToggleCardKey = async (row) => {
  const isDisabling = row.status !== '已禁用'
  const actionText = isDisabling ? '禁用' : '启用'

  try {
    await ElMessageBox.confirm(
      `确定要${actionText}卡密"${maskCardKey(row.cardKey)}"吗？`,
      `确认${actionText}`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    let response
    if (isDisabling) {
      response = await api.admin.toggleCardKeyStatus(row.cardKey, '已禁用')
    } else {
      response = await api.admin.toggleCardKeyStatus(row.cardKey, '未使用')
    }

    if (response && response.code === 200) {
      ElMessage.success(`${actionText}成功`)
      loadCardKeys()
    } else {
      ElMessage.error(response?.message || `${actionText}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${actionText}卡密失败，请检查网络连接`)
    }
  }
}

const handleDeleteCardKey = async (row) => {
  const response = await api.admin.deleteCardKey(row.cardKey)

  if (response && response.code === 200) {
    ElMessage.success('删除成功')
    loadCardKeys()
  } else {
    ElMessage.error(response?.message || '删除失败')
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

const loadAgents = async () => {
  try {
    const response = await api.admin.getAgents()
    if (response && response.data) {
      agents.value = response.data
    }
  } catch (error) {
    // eslint-disable-next-line no-empty
  }
}

</script>

<style scoped>
.admin-cardkey-management {
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

.product-spec {
  font-size: 13px;
  color: #64748d;
}

.time-text {
  font-size: 13px;
  color: #64748d;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 0;
  padding: 16px 20px;
  border-top: 1px solid #e5edf5;
}


</style>
