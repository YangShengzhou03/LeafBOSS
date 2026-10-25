<template>
  <div class="version-management">
    <el-card shadow="never" :body-style="{ padding: '20px' }">
      <div class="header">
        <el-button type="primary" @click="openCreate">发布新版本</el-button>
      </div>

      <el-table :data="versions" stripe style="width: 100%">
        <el-table-column prop="productName" label="应用" width="140" :show-overflow-tooltip="true" />
        <el-table-column prop="version" label="版本号" width="110" />
        <el-table-column prop="downloadUrl" label="下载链接" show-overflow-tooltip>
          <template #default="{ row }">
            <a :href="row.downloadUrl" target="_blank" rel="noopener">{{ row.downloadUrl }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="releaseNotes" label="更新说明" show-overflow-tooltip />
        <el-table-column label="强制更新" width="100">
          <template #default="{ row }">
            <el-tag :type="row.forceUpdate ? 'danger' : 'info'" effect="plain">
              {{ row.forceUpdate ? '强制' : '可选' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="删除该版本？" confirm-button-text="删除" cancel-button-text="取消" @confirm="deleteVersion(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑版本' : '发布新版本'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="应用" prop="productName">
          <el-select v-model="form.productName" placeholder="请选择应用" style="width: 100%">
            <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="如 1.2.0" />
        </el-form-item>
        <el-form-item label="下载链接" prop="downloadUrl">
          <el-input v-model="form.downloadUrl" placeholder="https:// 开头的 OSS 安装包地址" />
        </el-form-item>
        <el-form-item label="更新说明" prop="releaseNotes">
          <el-input v-model="form.releaseNotes" type="textarea" :rows="4" :placeholder="DEFAULT_RELEASE_NOTES" />
        </el-form-item>
        <el-form-item label="强制更新">
          <el-switch v-model="form.forceUpdate" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveVersion">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/services/api.js'

const versions = ref([])
const productList = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const DEFAULT_RELEASE_NOTES = '修复已知问题，优化功能体验，提升运行稳定性'
const form = ref({ productName: '', version: '', downloadUrl: '', releaseNotes: DEFAULT_RELEASE_NOTES, forceUpdate: false })

// 语义化版本比较：逐段按数字比较，相等返回 0
const compareVersion = (a, b) => {
  const s1 = String(a).replace(/^v/i, '').split('.').map(Number)
  const s2 = String(b).replace(/^v/i, '').split('.').map(Number)
  const len = Math.max(s1.length, s2.length)
  for (let i = 0; i < len; i++) {
    const x = s1[i] || 0
    const y = s2[i] || 0
    if (x !== y) return x - y
  }
  return 0
}

const validateVersion = (rule, value, callback) => {
  if (!value) return callback(new Error('请填写版本号'))
  if (!/^\d+\.\d+\.\d+$/.test(value.trim())) return callback(new Error('版本号格式应为 x.y.z，如 1.2.0'))
  // 不得低于该应用已发布的最高版本（编辑时排除自身）
  const product = form.value.productName
  if (!product) return callback()
  const others = versions.value.filter(v => v.productName === product && v.id !== editingId.value)
  const highest = others.reduce((max, v) => (compareVersion(v.version, max) > 0 ? v.version : max), null)
  if (highest && compareVersion(value, highest) < 0) {
    return callback(new Error(`不得低于该应用已发布的最高版本 ${highest}`))
  }
  callback()
}

const validateDownloadUrl = (rule, value, callback) => {
  if (!value) return callback(new Error('请填写下载链接'))
  if (!/^https:\/\/.+/i.test(value.trim())) return callback(new Error('下载链接必须以 https:// 开头'))
  callback()
}

const rules = {
  productName: [{ required: true, message: '请选择应用', trigger: 'change' }],
  version: [{ required: true, validator: validateVersion, trigger: 'blur' }],
  downloadUrl: [{ required: true, validator: validateDownloadUrl, trigger: 'blur' }]
}

const loadVersions = async () => {
  try {
    const res = await api.admin.getAppVersions()
    versions.value = res.data?.records || []
  } catch (e) {
    versions.value = []
  }
}

const loadProducts = async () => {
  try {
    const response = await api.admin.getProductList({ page: 1, size: 1000, status: 'active' })
    productList.value = response?.data?.records || response?.data?.content || []
  } catch (e) {
    productList.value = []
  }
}

const resetForm = () => {
  editingId.value = null
  form.value = { productName: '', version: '', downloadUrl: '', releaseNotes: DEFAULT_RELEASE_NOTES, forceUpdate: false }
  formRef.value?.clearValidate()
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  editingId.value = row.id
  form.value = {
    productName: row.productName,
    version: row.version,
    downloadUrl: row.downloadUrl,
    releaseNotes: row.releaseNotes || DEFAULT_RELEASE_NOTES,
    forceUpdate: !!row.forceUpdate
  }
  dialogVisible.value = true
}

const saveVersion = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (editingId.value) {
      await api.admin.updateAppVersion(editingId.value, form.value)
      ElMessage.success('版本已更新')
    } else {
      await api.admin.createAppVersion(form.value)
      ElMessage.success('版本已发布')
    }
    dialogVisible.value = false
    resetForm()
    loadVersions()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

const deleteVersion = async (id) => {
  try {
    await api.admin.deleteAppVersion(id)
    ElMessage.success('版本已删除')
    loadVersions()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadVersions()
  loadProducts()
})
</script>

<style scoped>
.version-management .header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 16px;
}
</style>
