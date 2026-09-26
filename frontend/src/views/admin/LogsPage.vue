<template>
  <div class="admin-logs-page">
    <el-card class="logs-card" shadow="never" :body-style="{ padding: 0 }">

      <div class="filter-section">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="10" :md="6" class="mb-10">
            <el-date-picker v-model="filter.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
              end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-col>
          <el-col :xs="24" :sm="6" :md="4" class="mb-10">
            <el-select v-model="filter.operationType" placeholder="操作类型" clearable style="width: 100%">
              <el-option label="登录" value="登录" />
              <el-option label="管理" value="管理" />
              <el-option label="授权" value="授权" />
              <el-option label="卡密" value="卡密" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="8" :md="14" class="button-group">
            <el-button type="primary" @click="handleFilter">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="danger" @click="clearLogs" :loading="clearing">
              清空日志
            </el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table v-loading="loading" :data="logs" style="width: 100%" @row-click="viewLogDetail">
          <el-table-column prop="operationType" label="操作类型" width="150" align="center">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.operationType)" size="default">
                {{ getOperationTypeName(row.operationType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="操作内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="ipAddress" label="IP地址" width="160" align="center" :show-overflow-tooltip="true" />
          <el-table-column prop="createdAt" label="操作时间" width="180" align="center">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) || '-' }}
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无日志数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          :total="totalLogs" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="showLogDetail" title="日志详情" :width="isMobile ? '90%' : '500px'">
      <div v-if="selectedLog">
        <el-descriptions :column="isMobile ? 1 : 2" border>
          <el-descriptions-item label="日志ID">{{ selectedLog.id }}</el-descriptions-item>
          <el-descriptions-item label="操作">
            <el-tag :type="getLevelType(selectedLog.operationType)">
              {{ getOperationTypeName(selectedLog.operationType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ selectedLog.ipAddress }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ formatDateTime(selectedLog.createdAt) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作内容" :span="isMobile ? 1 : 2">{{ selectedLog.description }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../services/api'
import { formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

const loading = ref(false)
const clearing = ref(false)
const isMobile = useIsMobile()

onMounted(() => {
  loadLogs()
})

const logs = ref([])
const totalLogs = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const showLogDetail = ref(false)
const selectedLog = ref(null)

const filter = reactive({
  dateRange: [],
  operationType: ''
})

const getLevelType = (operationType) => {
  switch (operationType) {
    case '登录': return 'success'
    case '管理': return 'danger'
    case '授权': return 'warning'
    case '卡密': return 'info'
    default: return 'info'
  }
}

const getOperationTypeName = (operationType) => {
  switch (operationType) {
    case '登录': return '登录'
    case '管理': return '管理'
    case '授权': return '授权'
    case '卡密': return '卡密'
    default: return operationType
  }
}

const resetFilter = () => {
  filter.dateRange = []
  filter.operationType = ''
  currentPage.value = 1
  loadLogs()
}

const loadLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }

    if (filter.dateRange && filter.dateRange.length === 2) {
      params.startDate = filter.dateRange[0]
      params.endDate = filter.dateRange[1]
    }

    if (filter.operationType) {
      params.operationTypes = [filter.operationType]
    }

    const response = await api.admin.getLogList(params)

    if (response && response.data) {
      const data = response.data
      if (data.records) {
        logs.value = data.records
        totalLogs.value = data.total || 0
      } else if (data.content) {
        logs.value = data.content
        totalLogs.value = data.totalElements || 0
      } else {
        logs.value = []
        totalLogs.value = 0
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '加载日志数据失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadLogs()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadLogs()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadLogs()
}

const viewLogDetail = (log) => {
  selectedLog.value = log
  showLogDetail.value = true
}

const clearLogs = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有日志吗？此操作不可恢复！',
      '确认清空',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    clearing.value = true

    await api.admin.clearLogs()

    logs.value = []
    totalLogs.value = 0
    ElMessage.success('日志清空成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清空日志失败')
    }
  } finally {
    clearing.value = false
  }
}

</script>

<style scoped>
.admin-logs-page {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.logs-card {
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.filter-section {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #e5edf5;
}

.button-group {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.mb-10 {
  margin-bottom: 10px;
}

.flex-grow {
  flex-grow: 1;
}

.table-container {
  padding: 0 20px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 0;
  padding: 16px 20px;
  border-top: 1px solid #e5edf5;
}
</style>
