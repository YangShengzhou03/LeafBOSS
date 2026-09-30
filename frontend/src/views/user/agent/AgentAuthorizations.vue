<template>
  <div class="agent-authorizations">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>授权列表</span>
        </div>
      </template>

      <el-table :data="cardKeys" stripe v-loading="loading" empty-text="暂无授权记录">
        <el-table-column prop="cardKey" label="卡密" min-width="180" :show-overflow-tooltip="true" />
        <el-table-column prop="productName" label="商品" width="200" :show-overflow-tooltip="true">
          <template #default="{ row }">
            {{ row.productName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="specificationName" label="规格" width="200" :show-overflow-tooltip="true">
          <template #default="{ row }">
            {{ row.specificationName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="使用用户" width="220" :show-overflow-tooltip="true">
          <template #default="{ row }">
            <span v-if="row.userEmail">
              {{ row.username ? `${row.username}（${row.userEmail}）` : row.userEmail }}
            </span>
            <span v-else style="color: var(--el-text-color-secondary)">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusDisplay(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="activateTime" label="授权时间" width="160">
          <template #default="{ row }">
            {{ row.activateTime ? formatTime(row.activateTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="到期时间" width="160">
          <template #default="{ row }">
            <span v-if="row.expireTime" :class="{ 'text-danger': isExpired(row.expireTime) }">
              {{ formatTime(row.expireTime) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" link size="small" @click="handleRevoke(row)" v-if="row.status === '已使用'">
              吊销
            </el-button>
            <el-button type="success" link size="small" @click="handleRestore(row)" v-if="row.status === '已吊销'">
              恢复
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/services/api'
import store from '@/utils/store.js'

const cardKeys = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const formatTime = (time) => (time || '').replace('T', ' ')

const isExpired = (time) => new Date(time) < new Date()

const getStatusDisplay = (status) => {
  switch (status) {
    case '未使用': return '待授权'
    case '已使用': return '已授权'
    case '已吊销': return '已吊销'
    default: return status || '-'
  }
}

const getStatusType = (status) => {
  switch (status) {
    case '未使用': return 'success'
    case '已使用': return 'primary'
    case '已吊销': return 'danger'
    default: return 'info'
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const response = await api.user.getCardKeyList({
      page: currentPage.value,
      size: pageSize.value
    })
    if (response && response.data) {
      cardKeys.value = response.data.records || []
      total.value = response.data.total || 0
    } else {
      ElMessage.error('加载授权列表失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载授权列表失败')
  } finally {
    loading.value = false
  }
}

const handleRevoke = async (row) => {
  try {
    await ElMessageBox.confirm('确定要吊销该卡密吗？吊销后使用用户将无法继续使用。', '确认吊销', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.user.revokeAgentCardKey(row.cardKey)
    ElMessage.success('已吊销')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '吊销失败')
    }
  }
}

const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm('确定要恢复该卡密吗？恢复后使用用户可继续正常使用。', '确认恢复', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await api.user.restoreAgentCardKey(row.cardKey)
    ElMessage.success('已恢复')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '恢复失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该卡密吗？此操作不可恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'danger'
    })
    await api.user.deleteAgentCardKey(row.cardKey)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  if (store.state.user?.role === 'agent') {
    loadData()
  }
})
</script>

<style scoped>
.agent-authorizations {
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.text-danger {
  color: #f56c6c;
}
</style>
