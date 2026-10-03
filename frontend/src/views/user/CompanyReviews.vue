<template>
  <div class="company-reviews">
    <el-card shadow="never" class="reviews-card">
      <template #header>
        <div class="card-header">
          <span>公司评论</span>
          <el-button type="primary" @click="showSubmitDialog = true">提交评论</el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="searchQuery" placeholder="公司名称" clearable @clear="handleSearch"
          @keyup.enter="handleSearch" style="max-width: 300px">
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="reviews" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="content" label="评论内容" min-width="250" :show-overflow-tooltip="true" />
        <el-table-column prop="companyName" label="公司" width="140" :show-overflow-tooltip="true" />
        <el-table-column label="投票" width="160" align="center">
          <template #default="{ row }">
            <div class="vote-cell">
              <el-button link :type="row.myVote === 'like' ? 'primary' : ''" @click="handleVote(row, 'like')">
                👍 {{ row.likeCount || 0 }}
              </el-button>
              <el-button link :type="row.myVote === 'dislike' ? 'danger' : ''" @click="handleVote(row, 'dislike')">
                👎 {{ row.dislikeCount || 0 }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="评论时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-popconfirm v-if="row.canDelete" title="确定删除该评论？"
              confirm-button-text="确定" cancel-button-text="取消" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无评论" :image-size="120" />
        </template>
      </el-table>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
          :total="total" layout="total, sizes, prev, pager, next" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 提交评论弹窗 -->
    <el-dialog v-model="showSubmitDialog" title="提交评论" :width="isMobile ? '90%' : '500px'">
      <el-form :model="reviewForm" :rules="reviewRules" ref="reviewFormRef" label-width="80px">
        <el-form-item label="公司名称" prop="company_name">
          <el-input v-model="reviewForm.company_name" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="评论内容" prop="content">
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="请输入评论内容（最多500字）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubmitDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'
import api from '../../services/api'

const loading = ref(false)
const isMobile = useIsMobile()

const reviews = ref([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showSubmitDialog = ref(false)
const reviewFormRef = ref(null)
const reviewForm = reactive({
  company_name: '',
  content: ''
})

const reviewRules = {
  company_name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  content: [
    { required: true, message: '请输入评论内容', trigger: 'blur' },
    { max: 500, message: '评论内容不能超过500字符', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadReviews()
})

const loadReviews = async () => {
  loading.value = true
  try {
    const response = await api.user.getCompanyReviews({
      companyName: searchQuery.value,
      page: currentPage.value,
      size: pageSize.value
    })
    if (response && response.data) {
      reviews.value = response.data.records || []
      total.value = response.data.total || 0
    } else {
      reviews.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载评论失败')
    reviews.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadReviews()
}

const handleSubmit = async () => {
  if (!reviewFormRef.value) return
  try {
    await reviewFormRef.value.validate()
    const response = await api.user.createCompanyReview({
      company_name: reviewForm.company_name,
      content: reviewForm.content
    })
    if (response && response.code === 200) {
      ElMessage.success('评论提交成功')
      showSubmitDialog.value = false
      reviewForm.company_name = ''
      reviewForm.content = ''
      loadReviews()
    } else {
      ElMessage.error(response?.message || '提交失败')
    }
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '提交失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    const response = await api.user.deleteCompanyReview(row.id)
    if (response && response.code === 200) {
      ElMessage.success('删除成功')
      loadReviews()
    } else {
      ElMessage.error(response?.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleVote = async (row, vote) => {
  try {
    const response = await api.user.voteCompanyReview(row.id, vote)
    if (response && response.code === 200) {
      row.likeCount = response.data.likeCount
      row.dislikeCount = response.data.dislikeCount
      row.myVote = response.data.myVote
    } else {
      ElMessage.error(response?.message || '投票失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '投票失败')
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadReviews()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadReviews()
}
</script>

<style scoped>
.company-reviews {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.reviews-card {
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 16px;
}

.vote-cell {
  display: flex;
  gap: 4px;
  justify-content: center;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
