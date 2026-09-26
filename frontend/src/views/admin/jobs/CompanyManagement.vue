<template>
  <div class="admin-company-management">
    <el-card class="company-card" shadow="never">

      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="公司名称" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="16" :md="18" class="button-group">
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="primary" @click="handleAddCompany">新增公司</el-button>
            <el-button @click="handleExportCompanies" :loading="exporting">导出公司</el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="companies" v-loading="loading" style="width: 100%" :key="tableKey"
          :reserve-selection="false" :row-key="row => row.id || Math.random()">
          <el-table-column prop="id" label="ID" width="100" align="center">
            <template #default="scope">
              <span class="id-display">{{ formatId(scope.row.id) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="公司名称" min-width="200" align="left" :show-overflow-tooltip="true" />
          <el-table-column prop="viewCount" label="浏览量" width="100" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              {{ scope.row.viewCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="commentCount" label="评论数" width="100" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              {{ scope.row.commentCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.createdAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="260" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link size="default" @click="handleWriteReview(row)">评论</el-button>
              <el-button link size="default" @click="handleViewReviews(row)">查看</el-button>
              <el-button link size="default" @click="handleEditCompany(row)">编辑</el-button>
              <el-popconfirm
                title="确定删除？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleDeleteCompany(row)">
                <template #reference>
                  <el-button link size="default" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无公司数据" :image-size="120" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          :total="total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="showAddDialog" :title="editingCompany ? '编辑公司' : '添加公司'" :width="isMobile ? '90%' : '500px'">
      <el-form :model="companyForm" :rules="companyRules" ref="companyFormRef" :label-width="isMobile ? '60px' : '80px'">
        <el-form-item label="公司名称" prop="name">
          <el-input v-model="companyForm.name" placeholder="请输入公司名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="saveCompany">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看评论弹窗 -->
    <el-dialog v-model="showReviewsDialog" :title="`${selectedCompany?.name || ''} 的评论`" :width="isMobile ? '95%' : '700px'">
      <el-table :data="companyReviews" v-loading="reviewsLoading" stripe max-height="400">
        <el-table-column prop="cardKey" label="卡密" min-width="180" align="center">
          <template #default="{ row }">
            <span>{{ maskCardKey(row.cardKey) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="200" align="left" :show-overflow-tooltip="true" />
        <el-table-column prop="createdAt" label="评论时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) || '-' }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无评论" :image-size="80" />
        </template>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showReviewsDialog = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 写评论弹窗 -->
    <el-dialog v-model="showWriteReviewDialog" :title="`写评论 - ${selectedCompany?.name || ''}`" :width="isMobile ? '90%' : '500px'">
      <el-form :model="reviewForm" :rules="reviewRules" ref="reviewFormRef" :label-width="isMobile ? '60px' : '80px'">
        <el-form-item label="卡密" prop="cardKey">
          <el-input v-model="reviewForm.cardKey" placeholder="请输入卡密" />
        </el-form-item>
        <el-form-item label="评论内容" prop="content">
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="请输入评论内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showWriteReviewDialog = false">取消</el-button>
          <el-button type="primary" @click="saveReview">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { maskCardKey, formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'
import api from '../../../services/api'

const loading = ref(false)
const isMobile = useIsMobile()

onMounted(() => {
  loadCompanies()
})

const companies = ref([])

const tableKey = ref(0)

const searchQuery = ref('')

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showAddDialog = ref(false)
const editingCompany = ref(null)

// 查看评论弹窗
const showReviewsDialog = ref(false)
const selectedCompany = ref(null)
const companyReviews = ref([])
const reviewsLoading = ref(false)

// 写评论弹窗
const showWriteReviewDialog = ref(false)
const reviewFormRef = ref(null)
const reviewForm = reactive({
  cardKey: '',
  content: ''
})

const reviewRules = {
  cardKey: [{ required: true, message: '请输入卡密', trigger: 'blur' }],
  content: [{ required: true, message: '请输入评论内容', trigger: 'blur' }]
}

const companyForm = reactive({
  name: ''
})

const formatId = (id) => {
  if (!id) return ''
  const idStr = id.toString()
  if (idStr.length > 8) {
    return `${idStr.substring(0, 8)}...`
  }
  return idStr
}

const companyRules = {
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }]
}

const loadCompanies = async () => {
  loading.value = true
  try {
    const response = await api.admin.getCompanyList({
      page: currentPage.value,
      size: pageSize.value,
      name: searchQuery.value
    })

    if (response && response.data) {
      companies.value = response.data.records || response.data.content || []
      total.value = response.data.total || response.data.totalElements || 0
    } else {
      companies.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载公司数据失败，请检查网络连接')
    companies.value = []
    total.value = 0
  } finally {
    loading.value = false
    tableKey.value += 1
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadCompanies()
}

const exporting = ref(false)

const handleExportCompanies = async () => {
  if (exporting.value) return
  exporting.value = true
  try {
    // 分页接口，循环拉取全部公司
    const all = []
    const size = 500
    for (let page = 1; ; page++) {
      const response = await api.admin.getCompanyList({ page, size })
      const records = response?.data?.records || response?.data?.content || []
      all.push(...records)
      if (records.length < size) break
    }
    if (all.length === 0) {
      ElMessage.warning('暂无公司数据可导出')
      return
    }
    const rows = all.map(c => ({
      'ID': c.id,
      '公司名称': c.name,
      '浏览量': c.viewCount || 0,
      '评论数': c.commentCount || 0,
      '创建时间': formatDateTime(c.createdAt) || '-'
    }))
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '公司数据')
    XLSX.writeFile(wb, `公司数据_${new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success(`已导出 ${all.length} 家公司`)
  } catch (error) {
    ElMessage.error(error.message || '导出公司数据失败，请检查网络连接')
  } finally {
    exporting.value = false
  }
}

const handleAddCompany = () => {
  showAddDialog.value = true
  editingCompany.value = null
  resetForm()
}

const handleEditCompany = (row) => {
  editingCompany.value = row
  Object.assign(companyForm, row)
  showAddDialog.value = true
}

const saveCompany = async () => {
  try {
    if (editingCompany.value) {
      const response = await api.admin.editCompany(companyForm.id, companyForm);
      if (response && response.code === 200) {
        ElMessage.success('公司更新成功');
        showAddDialog.value = false;
        loadCompanies();
      } else {
        ElMessage.error(response?.message || '保存公司失败');
      }
    } else {
      const response = await api.admin.createCompany(companyForm);
      if (response && response.code === 200) {
        ElMessage.success('公司添加成功');
        showAddDialog.value = false;
        loadCompanies();
      } else {
        ElMessage.error(response?.message || '保存公司失败');
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '保存公司失败，请检查网络连接');
  }
}

const resetForm = () => {
  editingCompany.value = null
  Object.assign(companyForm, {
    name: ''
  })
}

const handleDeleteCompany = async (row) => {
  const response = await api.admin.deleteCompany(row.id)
  if (response && response.code === 200) {
    ElMessage.success('删除成功')
    loadCompanies()
  } else {
    ElMessage.error(response?.message || '删除失败，请重试')
  }
}

const handleViewReviews = async (row) => {
  selectedCompany.value = row
  showReviewsDialog.value = true
  reviewsLoading.value = true
  try {
    const response = await api.admin.getBossReviewList({
      page: 1,
      size: 100,
      companyId: row.id
    })
    if (response && response.data) {
      companyReviews.value = response.data.records || response.data.content || []
    } else {
      companyReviews.value = []
    }
  } catch (error) {
    ElMessage.error(error.message || '加载评论数据失败')
    companyReviews.value = []
  } finally {
    reviewsLoading.value = false
  }
}

const handleWriteReview = (row) => {
  selectedCompany.value = row
  Object.assign(reviewForm, { cardKey: '', content: '' })
  showWriteReviewDialog.value = true
}

const saveReview = async () => {
  try {
    await reviewFormRef.value.validate()
  } catch {
    return
  }
  try {
    const response = await api.admin.createBossReview({
      cardKey: reviewForm.cardKey,
      companyId: selectedCompany.value.id,
      content: reviewForm.content
    })
    if (response && response.code === 200) {
      ElMessage.success('评论发布成功')
      showWriteReviewDialog.value = false
      loadCompanies()
    } else {
      ElMessage.error(response?.message || '评论发布失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '评论发布失败，请检查网络连接')
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadCompanies()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadCompanies()
}

</script>

<style scoped>
.admin-company-management {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.company-card {
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

.flex-grow {
  flex-grow: 1;
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
  gap: 10px;
  flex-wrap: wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
