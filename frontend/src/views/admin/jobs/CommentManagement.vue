<template>
  <div class="admin-comment-management">
    <el-card class="comment-card" shadow="never">

      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="searchQuery" placeholder="公司名称" clearable @clear="handleSearch"
            @keyup.enter="handleSearch" style="width: 240px">
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button @click="handleExport">导出评论</el-button>
          <el-button type="primary" @click="showImportDialog = true">导入评论</el-button>
        </div>
      </div>

      <div class="table-container">
        <el-table :data="comments" v-loading="loading" style="width: 100%"
          :row-key="row => row.id">
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column prop="companyName" label="公司" min-width="160" :show-overflow-tooltip="true" />
          <el-table-column prop="content" label="评论内容" min-width="300" :show-overflow-tooltip="true" />
          <el-table-column prop="userId" label="用户ID" min-width="180" :show-overflow-tooltip="true" />
          <el-table-column label="投票" width="140" align="center">
            <template #default="scope">
              <span class="vote-cell">
                <span class="like">👍 {{ scope.row.likeCount || 0 }}</span>
                <span class="dislike">👎 {{ scope.row.dislikeCount || 0 }}</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="评论时间" width="180" align="center">
            <template #default="scope">
              {{ formatDateTime(scope.row.createdAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-popconfirm
                title="确定删除该评论？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleDelete(row)">
                <template #reference>
                  <el-button link size="default" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无评论数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="导入评论" width="480px" :close-on-click-modal="false">
      <div class="import-dialog">
        <div class="download-template">
          <el-link type="primary" @click="handleDownloadTemplate" :underline="false">
            <el-icon><Download /></el-icon>下载模板
          </el-link>
        </div>

        <el-upload drag :show-file-list="false" :before-upload="handleImport" accept=".xlsx,.xls"
          :http-request="(() => {})" :disabled="importing">
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 .xlsx 或 .xls 文件</div>
          </template>
        </el-upload>
      </div>
    </el-dialog>

    <!-- 导入结果 -->
    <el-dialog v-model="showImportResult" title="导入结果" width="450px">
      <div v-if="importResult">
        <el-result icon="success" :title="`成功导入 ${importResult.successCount} 条`"
          v-if="importResult.skipCount === 0">
          <template #extra>
            <el-button @click="showImportResult = false; showImportDialog = false">关闭</el-button>
          </template>
        </el-result>
        <template v-else>
          <el-alert :closable="false" type="warning" show-icon>
            <template #title>
              成功 <strong>{{ importResult.successCount }}</strong> 条，跳过 <strong>{{ importResult.skipCount }}</strong> 条
            </template>
          </el-alert>
          <div v-if="importResult.errors && importResult.errors.length" class="error-list">
            <p v-for="(err, idx) in importResult.errors" :key="idx" class="error-item">{{ err }}</p>
          </div>
          <div style="margin-top: 16px; text-align: right;">
            <el-button @click="showImportResult = false; showImportDialog = false">关闭</el-button>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Download } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { formatDateTime } from '@/utils/utils.js'
import api from '../../../services/api'

const loading = ref(false)
const importing = ref(false)

onMounted(() => {
  loadComments()
})

const comments = ref([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showImportDialog = ref(false)
const showImportResult = ref(false)
const importResult = ref(null)

const loadComments = async () => {
  loading.value = true
  try {
    const response = await api.admin.getAdminReviewList({
      page: currentPage.value,
      size: pageSize.value,
      companyName: searchQuery.value
    })

    if (response && response.data) {
      comments.value = response.data.records || response.data.content || []
      total.value = response.data.total || response.data.totalElements || 0
    } else {
      comments.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载评论数据失败')
    comments.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadComments()
}

const handleDelete = async (row) => {
  try {
    const response = await api.admin.deleteAdminReview(row.id)
    if (response && response.code === 200) {
      ElMessage.success('删除成功')
      loadComments()
    } else {
      ElMessage.error(response?.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleExport = async () => {
  try {
    const blob = await api.admin.exportAdminReviews(searchQuery.value || null)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `reviews_${new Date().toISOString().slice(0, 19).replace(/[:-]/g, '')}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

const handleDownloadTemplate = () => {
  const data = [
    ['公司名', '评论内容'],
    ['阿里巴巴', '公司氛围不错，福利好'],
    ['字节跳动', '加班严重，慎入'],
    ['腾讯', '鹅厂待遇还行']
  ]
  const ws = XLSX.utils.aoa_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '评论模板')
  XLSX.writeFile(wb, '评论导入模板.xlsx')
}

const handleImport = async (file) => {
  importing.value = true
  try {
    const response = await api.admin.importAdminReviews(file.file)
    if (response && response.code === 200) {
      importResult.value = response.data
      showImportDialog.value = false
      showImportResult.value = true
      loadComments()
    } else {
      ElMessage.error(response?.message || '导入失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importing.value = false
  }
  return false
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadComments()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadComments()
}
</script>

<style scoped>
.admin-comment-management {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.comment-card {
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #e5edf5;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.table-container {
  padding: 0;
}

.vote-cell {
  display: flex;
  gap: 12px;
  justify-content: center;
  font-size: 13px;
}

.vote-cell .like {
  color: #67c23a;
}

.vote-cell .dislike {
  color: #f56c6c;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 0;
  padding: 16px 20px;
  border-top: 1px solid #e5edf5;
}

.import-dialog {
  padding: 8px 0;
}

.download-template {
  text-align: center;
  margin-bottom: 12px;
  font-size: 13px;
}

.error-list {
  max-height: 200px;
  overflow-y: auto;
  margin-top: 12px;
}

.error-item {
  margin: 4px 0;
  padding: 6px 10px;
  background: #fef0f0;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 12px;
}
</style>
