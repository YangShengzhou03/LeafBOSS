<template>
  <div class="admin-boss-review-management">
    <el-card class="review-card" shadow="never">

      <div class="search-bar">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="8" :md="6" class="mb-10">
            <el-input v-model="searchQuery" placeholder="搜索卡密/公司/评论内容" clearable @clear="handleSearch"
              @keyup.enter="handleSearch">
              <template #append>
                <el-button @click="handleSearch">
                  搜索
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="14" :md="16" class="button-group">
            <div class="flex-grow" v-if="!isMobile"></div>
            <el-button type="primary" @click="handleAddReview">新增评论</el-button>
            <el-button @click="handleImportReviews">导入评论</el-button>
          </el-col>
        </el-row>
      </div>

      <div class="table-container">
        <el-table :data="filteredReviews" v-loading="loading" style="width: 100%" :key="tableKey"
          :reserve-selection="false" :row-key="row => row.id || Math.random()">

          <el-table-column prop="cardKey" label="卡密" width="200" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              <span>{{ maskCardKey(scope.row.cardKey) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="companyName" label="公司" min-width="150" align="left" :show-overflow-tooltip="true" />
          <el-table-column prop="content" label="评论内容" min-width="250" align="left" :show-overflow-tooltip="true" />
          <el-table-column label="赞" width="80" align="center">
            <template #default="scope">
              {{ scope.row.likeCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="踩" width="80" align="center">
            <template #default="scope">
              {{ scope.row.dislikeCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="评论时间" width="180" align="center" :show-overflow-tooltip="true">
            <template #default="scope">
              {{ formatDateTime(scope.row.createdAt) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-popconfirm
                title="确定删除该评论吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleDeleteReview(row)">
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

    <el-dialog v-model="showAddDialog" title="新增评论" :width="isMobile ? '90%' : '500px'">
      <el-form :model="reviewForm" :rules="reviewRules" ref="reviewFormRef" :label-width="isMobile ? '60px' : '80px'">
        <el-form-item label="卡密" prop="cardKey">
          <el-input v-model="reviewForm.cardKey" placeholder="请输入卡密" />
        </el-form-item>
        <el-form-item label="公司" prop="companyId">
          <el-select v-model="reviewForm.companyId" placeholder="输入公司名称搜索" style="width: 100%;"
            filterable remote :remote-method="searchCompanies" :loading="companiesLoading">
            <el-option v-for="company in companies" :key="company.id" :label="company.name" :value="company.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评论内容" prop="content">
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="请输入评论内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="saveReview">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 导入评论弹窗 -->
    <el-dialog v-model="showImportDialog" title="导入评论" :width="isMobile ? '90%' : '500px'" @closed="resetImport">
      <div style="text-align: center; margin-bottom: 15px;">
        <el-link type="primary" :underline="false" @click="downloadImportTemplate">
          下载模板
        </el-link>
      </div>
      <el-upload ref="importUploadRef" drag action="#" :auto-upload="false" :limit="1" accept=".xlsx,.xls"
        :on-change="handleImportFileChange" :on-remove="() => (importFile = null)"
        :on-exceed="() => ElMessage.warning('只能选择一个文件')" style="margin-top: 15px;">
        <el-icon style="font-size: 40px; color: #909399; margin-bottom: 8px;">
          <UploadFilled />
        </el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls，模板列：卡密、公司名称、评论内容</div>
        </template>
      </el-upload>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showImportDialog = false">取消</el-button>
          <el-button type="primary" :loading="importing" :disabled="!importFile" @click="importReviews">开始导入</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import api from '../../../services/api'
import { maskCardKey, formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'

const loading = ref(false)
const isMobile = useIsMobile()

onMounted(() => {
  loadReviews()
  loadCompanies()
})

const reviews = ref([])
const companies = ref([])

const tableKey = ref(0)

const searchQuery = ref('')

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showAddDialog = ref(false)

const reviewForm = reactive({
  cardKey: '',
  companyId: null,
  content: ''
})



const reviewRules = {
  cardKey: [{ required: true, message: '请输入卡密', trigger: 'blur' }],
  companyId: [{ required: true, message: '请选择公司', trigger: 'change' }],
  content: [{ required: true, message: '请输入评论内容', trigger: 'blur' }]
}

const filteredReviews = computed(() => {
  return reviews.value
})

const loadReviews = async () => {
  loading.value = true
  try {
    const response = await api.admin.getBossReviewList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value
    })

    if (response && response.data) {
      reviews.value = response.data.records || response.data.content || []
      total.value = response.data.total || response.data.totalElements || 0
    } else {
      reviews.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '加载评论数据失败，请检查网络连接')
    reviews.value = []
    total.value = 0
  } finally {
    loading.value = false
    tableKey.value += 1
  }
}

const loadCompanies = async () => {
  try {
    const response = await api.admin.getCompanyList({ page: 1, size: 50 })
    if (response && response.data) {
      companies.value = response.data.records || response.data.content || []
    }
  } catch (error) {
    ElMessage.error(error.message || '加载公司数据失败')
  }
}

const companiesLoading = ref(false)

const searchCompanies = async (query) => {
  try {
    companiesLoading.value = true
    const response = await api.admin.getCompanyList({ page: 1, size: 50, name: query })
    if (response && response.data) {
      const list = response.data.records || response.data.content || []
      const selectedId = reviewForm.companyId
      if (selectedId && !list.some(c => c.id === selectedId)) {
        const selected = companies.value.find(c => c.id === selectedId)
        if (selected) list.unshift(selected)
      }
      companies.value = list
    }
  } catch (error) {
    ElMessage.error(error.message || '搜索公司失败')
  } finally {
    companiesLoading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadReviews()
}

const handleAddReview = () => {
  showAddDialog.value = true
  resetForm()
}

const showImportDialog = ref(false)
const importing = ref(false)
const importFile = ref(null)
const importUploadRef = ref(null)

const handleImportReviews = () => {
  importFile.value = null
  importUploadRef.value?.clearFiles()
  showImportDialog.value = true
}

const resetImport = () => {
  importFile.value = null
  importUploadRef.value?.clearFiles()
}

const handleImportFileChange = (file) => {
  importFile.value = file.raw
}

const downloadImportTemplate = () => {
  const ws = XLSX.utils.aoa_to_sheet([['卡密', '公司名称', '评论内容']])
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '评论导入')
  XLSX.writeFile(wb, '评论导入模板.xlsx')
}

const importReviews = async () => {
  if (!importFile.value) return
  importing.value = true
  try {
    const buffer = await importFile.value.arrayBuffer()
    const wb = XLSX.read(buffer)
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
    if (rows.length === 0) {
      ElMessage.warning('文件中没有数据')
      return
    }

    // 拉取全部公司，构建名称 -> ID 映射
    const companyMap = new Map()
    const dupNames = new Set()
    const size = 500
    for (let page = 1; ; page++) {
      const response = await api.admin.getCompanyList({ page, size })
      const records = response?.data?.records || response?.data?.content || []
      for (const c of records) {
        if (companyMap.has(c.name)) dupNames.add(c.name)
        else companyMap.set(c.name, c.id)
      }
      if (records.length < size) break
    }

    let success = 0
    const failures = []
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i]
      const cardKey = String(row['卡密'] ?? '').trim()
      const companyName = String(row['公司名称'] ?? '').trim()
      const content = String(row['评论内容'] ?? '').trim()
      const rowNo = i + 2 // 表头占模板第1行
      if (!cardKey || !companyName || !content) {
        failures.push(`第${rowNo}行：存在空缺必填列`)
        continue
      }
      const companyId = dupNames.has(companyName) ? null : companyMap.get(companyName)
      if (!companyId) {
        failures.push(`第${rowNo}行：公司「${companyName}」${dupNames.has(companyName) ? '名称重复，无法定位' : '不存在'}`)
        continue
      }
      try {
        const response = await api.admin.createBossReview({ cardKey, companyId, content })
        if (response && response.code === 200) {
          success++
        } else {
          failures.push(`第${rowNo}行：${response?.message || '创建失败'}`)
        }
      } catch {
        failures.push(`第${rowNo}行：创建失败`)
      }
    }

    if (failures.length === 0) {
      ElMessage.success(`导入完成，成功 ${success} 条`)
      showImportDialog.value = false
    } else {
      ElMessageBox.alert(
        `成功 ${success} 条，失败 ${failures.length} 条。失败明细：${failures.slice(0, 10).join('；')}${failures.length > 10 ? '；…' : ''}`,
        '导入结果',
        { confirmButtonText: '知道了' }
      )
      if (success > 0) showImportDialog.value = false
    }
    loadReviews()
  } catch (error) {
    ElMessage.error(error.message || '解析文件失败，请使用下载的模板填写后上传')
  } finally {
    importing.value = false
  }
}

const saveReview = async () => {
  try {
    const response = await api.admin.createBossReview(reviewForm)
    if (response && response.code === 200) {
      ElMessage.success('评论添加成功')
      showAddDialog.value = false
      loadReviews()
    } else {
      ElMessage.error(response?.message || '保存评论失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '保存评论失败，请检查网络连接')
  }
}

const resetForm = () => {
  Object.assign(reviewForm, {
    cardKey: '',
    companyId: null,
    content: ''
  })
}

const handleDeleteReview = async (row) => {
  const response = await api.admin.deleteBossReview(row.id)
  if (response && response.code === 200) {
    ElMessage.success('删除成功')
    loadReviews()
  } else {
    ElMessage.error(response?.message || '删除失败，请重试')
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
.admin-boss-review-management {
  padding: 0;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.review-card {
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
