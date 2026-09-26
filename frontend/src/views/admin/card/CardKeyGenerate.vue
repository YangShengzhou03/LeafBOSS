<template>
  <div class="admin-cardkey-generate">
    <el-card class="generate-card" shadow="never" :body-style="{ padding: '20px' }">

      <el-form :model="generateForm" label-width="80px" :label-position="isMobile ? 'top' : 'left'">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="生成数量">
              <el-input-number v-model="generateForm.count" :min="1" :max="100" controls-position="right"
                placeholder="请输入数量" class="form-input" />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="商品">
              <el-select v-model="generateForm.productId" placeholder="请选择商品" clearable class="form-input"
                @change="handleProductChange">
                <el-option v-for="product in productList" :key="product.id" :label="product.name"
                  :value="product.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4">
            <el-form-item label="规格">
              <el-select v-model="generateForm.specId" placeholder="请选择规格" clearable class="form-input">
                <el-option v-for="spec in specList" :key="spec.id" :label="spec.name" :value="spec.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4" v-if="isAdmin">
            <el-form-item label="代理商">
              <el-select v-model="generateForm.agentId" placeholder="官方" clearable filterable class="form-input">
                <el-option label="官方" :value="null" />
                <el-option v-for="agent in agentList" :key="agent.id" :label="agent.username" :value="agent.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="4" v-if="!isAdmin">
            <el-form-item label="剩余额度">
              <span class="remaining-count">{{ remainingGenerateCount }}</span>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="4">
            <el-form-item label="操作">
              <el-button type="primary" @click="generateCardKeys" :loading="generating" class="generate-btn" style="width: 100%">
                生成卡密
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="字符集">
              <el-checkbox-group v-model="generateForm.charset" class="charset-group">
                <el-checkbox label="numbers" class="charset-checkbox">数字 (0-9)</el-checkbox>
                <el-checkbox label="uppercase" class="charset-checkbox">大写字母 (A-Z)</el-checkbox>
                <el-checkbox label="lowercase" class="charset-checkbox">小写字母 (a-z)</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div v-if="generatedKeys.length > 0" class="result-section">
        <div class="result-header-content" :class="{ 'is-mobile': isMobile }">
          <span class="result-title">生成结果 ({{ generatedKeys.length }} 条)</span>

          <div v-if="generateForm.productId && generateForm.specId" class="product-spec-info">
            <span class="info-label">商品：</span>
            <span class="info-value">{{ getProductName(generateForm.productId) }}</span>
            <span class="info-label">规格：</span>
            <span class="info-value">{{ getSpecName(generateForm.specId) }}</span>
            <span class="info-label">代理商：</span>
            <span class="info-value">{{ getAgentName(generateForm.agentId) }}</span>
          </div>

          <div class="action-buttons" :class="{ 'is-mobile': isMobile }">
            <el-button type="primary" @click="copyAndImport" :disabled="!generateForm.productId || !generateForm.specId" class="action-btn" :class="{ 'mobile-action-btn': isMobile }">
              复制并导入
            </el-button>
            <el-button type="success" @click="copyCardKeys" :class="{ 'mobile-action-btn': isMobile }">
              复制
            </el-button>
            <el-button type="primary" @click="exportCardKeys" class="export-btn" :class="{ 'mobile-action-btn': isMobile }">
              导出
            </el-button>
          </div>
        </div>

        <el-table :data="paginatedKeys" border stripe class="result-table">
          <el-table-column type="index" label="序号" width="80" align="center" />
          <el-table-column prop="key" label="卡密代码" min-width="220" align="center">
            <template #default="{ row }">
              <span class="key-text clickable" @click="copyCardKey(row.key)" :title="'点击复制: ' + row.key">
                {{ maskCardKey(row.key) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品" width="120" align="center" :show-overflow-tooltip="true" />
          <el-table-column prop="specName" label="规格" width="120" align="center" :show-overflow-tooltip="true" />
        </el-table>

        <div class="pagination-container" v-if="generatedKeys.length > 10">
          <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
            :total="generatedKeys.length" layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange" @current-change="handleCurrentChange" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { maskCardKey } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'
import api from '@/services/api.js'
import store from '@/utils/store.js'

const generating = ref(false)
const isAdmin = computed(() => store.state.isAdmin)
const addingToStock = ref(false)
const isMobile = useIsMobile()
const remainingGenerateCount = ref(0)


onMounted(async () => {
  await loadAllSpecs()
  await loadProducts()
  loadAgents()
  
  // 非管理员获取剩余次数
  if (!isAdmin.value) {
    fetchRemainingGenerateCount()
  }

  // 默认选中第一个商品及其第一个规格
  if (productList.value.length > 0) {
    generateForm.productId = productList.value[0].id
    loadSpecsByProduct(generateForm.productId)
    if (specList.value.length > 0) {
      generateForm.specId = specList.value[0].id
    }
  }
})

const generateForm = reactive({
  count: 1,
  length: 32,
  productId: '',
  specId: '',
  agentId: null,
  charset: ['numbers', 'uppercase', 'lowercase']
})

const productList = ref([])
const allSpecList = ref([])
const specList = ref([])
const agentList = ref([])
const generatedKeys = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

const paginatedKeys = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  const endIndex = startIndex + pageSize.value
  return generatedKeys.value.slice(startIndex, endIndex)
})

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

const loadProducts = async () => {
  try {
    const response = await api.admin.getProductList({ page: 1, size: 1000 })

    if (response.code === 200) {
      if (response.data && response.data.records) {
        productList.value = response.data.records || []
      } else if (Array.isArray(response.data)) {
        productList.value = response.data || []
      } else {
        productList.value = []
      }
    } else {
      ElMessage.error(response?.message || '加载商品列表失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载商品列表失败，请检查网络连接')
  }
}

const loadAllSpecs = async () => {
  try {
    const response = await api.admin.getSpecificationDTOs()

    if (response.code === 200) {
      if (response.data && response.data.records) {
        allSpecList.value = response.data.records || []
      } else if (Array.isArray(response.data)) {
        allSpecList.value = response.data || []
      } else {
        allSpecList.value = []
      }

      specList.value = []
    } else {
      ElMessage.error(response?.message || '加载规格列表失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载规格列表失败，请检查网络连接')
  }
}

const loadAgents = async () => {
  try {
    const response = await api.admin.getAgents()
    if (response && response.data) {
      agentList.value = response.data
    }
  } catch (error) {
    // eslint-disable-next-line no-empty
  }
}

const loadSpecsByProduct = (productId) => {
  if (!productId) {
    specList.value = []
    generateForm.specId = ''
    return
  }

  const filteredSpecs = allSpecList.value.filter(spec => spec.productId === productId)
  specList.value = filteredSpecs

  if (generateForm.specId && !filteredSpecs.some(spec => spec.id === generateForm.specId)) {
    generateForm.specId = ''
  }
}

const addToStock = async () => {
  if (generatedKeys.value.length === 0) {
    ElMessage.warning('请先生成卡密')
    return
  }

  if (!generateForm.productId || !generateForm.specId) {
    ElMessage.warning('请选择商品和规格')
    return
  }

  // 检查剩余次数
  if (!isAdmin.value && remainingGenerateCount.value < generatedKeys.value.length) {
    ElMessage.warning(`额度不足，还可生成 ${remainingGenerateCount.value} 个`)
    return
  }

  addingToStock.value = true

  try {
    await ElMessageBox.confirm(
      `确定要将 ${generatedKeys.value.length} 个卡密添加到库存吗？`,
      '确认添加库存',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    let successCount = 0

    try {
      const response = await api.admin.batchCreateCardKeys({
        keys: generatedKeys.value.map(k => k.key),
        specId: generateForm.specId,
        agentId: generateForm.agentId ? String(generateForm.agentId) : null
      })

      if (response.code === 200) {
        successCount = generatedKeys.value.length
      } else {
        ElMessage.warning(response.message || '添加库存失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '添加库存失败，请检查网络连接')
    }

    if (successCount > 0) {
      ElMessage.success(`成功添加 ${successCount} 个卡密到库存`)
      generatedKeys.value = []
      // 刷新剩余次数
      if (!isAdmin.value) {
        fetchRemainingGenerateCount()
      }
    }

  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '添加库存失败，请检查网络连接')
    }
  } finally {
    addingToStock.value = false
  }
}

const copyAndImport = async () => {
  await copyCardKeys()
  await addToStock()
}

const generateRandomKey = () => {
  let charset = ''

  if (generateForm.charset.includes('numbers')) {
    charset += '0123456789'
  }
  if (generateForm.charset.includes('uppercase')) {
    charset += 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  }
  if (generateForm.charset.includes('lowercase')) {
    charset += 'abcdefghijklmnopqrstuvwxyz'
  }

  if (charset === '') {
    charset = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  }

  let result = ''
  const charsetLength = charset.length

  for (let i = 0; i < 32; i++) {
    result += charset.charAt(Math.floor(Math.random() * charsetLength))
  }

  return result
}

const generateCardKeys = async () => {
  if (generateForm.count <= 0) {
    ElMessage.warning('请设置生成数量')
    return
  }

  if (!generateForm.productId || !generateForm.specId) {
    ElMessage.warning('请选择商品和规格')
    return
  }

  generating.value = true

  try {
    generatedKeys.value = []

    const product = productList.value.find(p => p.id === generateForm.productId)
    const spec = specList.value.find(s => s.id === generateForm.specId)

    const maxAttempts = generateForm.count * 10
    let attempts = 0
    const keySet = new Set()

    while (keySet.size < generateForm.count && attempts < maxAttempts) {
      keySet.add(generateRandomKey())
      attempts++
    }

    generatedKeys.value = [...keySet].map(key => ({
      key,
      productName: product ? product.name : '',
      specName: spec ? spec.name : ''
    }))

    if (generatedKeys.value.length < generateForm.count) {
      ElMessage.warning(`生成了 ${generatedKeys.value.length} 个唯一卡密（尝试 ${attempts} 次后达到上限）`)
    } else {
      ElMessage.success(`成功生成 ${generateForm.count} 个唯一卡密`)
    }
  } catch (error) {
    ElMessage.error(error.message || '生成卡密失败')
  } finally {
    generating.value = false
  }
}

const getProductName = (productId) => {
  const product = productList.value.find(p => p.id === productId)
  return product ? product.name : ''
}

const getSpecName = (specId) => {
  const spec = specList.value.find(s => s.id === specId)
  return spec ? spec.name : ''
}

const getAgentName = (agentId) => {
  if (!agentId) return '官方'
  const agent = agentList.value.find(a => a.id === agentId)
  return agent ? agent.username : ''
}

const handleProductChange = (productId) => {
  loadSpecsByProduct(productId)
  if (!isAdmin.value) {
    fetchRemainingGenerateCount(productId)
  }
}

const fetchRemainingGenerateCount = async (productId) => {
  try {
    const params = productId ? { productId } : {}
    const response = await api.user.getRemainingGenerateCount(params)
    if (response.code === 200) {
      remainingGenerateCount.value = response.data.remaining || 0
    }
  } catch (error) {
    // eslint-disable-next-line no-empty
  }
}

const exportCardKeys = () => {
  if (generatedKeys.value.length === 0) {
    ElMessage.warning('没有可导出的卡密')
    return
  }

  const content = generatedKeys.value.map(key => key.key).join('\n')
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `cardkeys_${new Date().getTime()}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('卡密已导出为TXT文件')
}

const copyCardKey = async (key) => {
  try {
    await navigator.clipboard.writeText(key)
    ElMessage.success('卡密已复制到剪贴板')
  } catch (error) {
    const textArea = document.createElement('textarea')
    textArea.value = key
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('卡密已复制到剪贴板')
  }
}

const copyCardKeys = async () => {
  if (generatedKeys.value.length === 0) {
    ElMessage.warning('没有可复制的卡密')
    return
  }

  try {
    const allKeys = generatedKeys.value.map(key => key.key).join('\n')
    await navigator.clipboard.writeText(allKeys)
    ElMessage.success(`已复制 ${generatedKeys.value.length} 个卡密到剪贴板`)
  } catch (error) {
    const textArea = document.createElement('textarea')
    textArea.value = generatedKeys.value.map(key => key.key).join('\n')
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success(`已复制 ${generatedKeys.value.length} 个卡密到剪贴板`)
  }
}
</script>

<style scoped>
.admin-cardkey-generate {
  padding: 0;
  background-color: transparent;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.generate-card {
  margin-bottom: 0;
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.form-input {
  width: 100%;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
}

.generate-btn {
  min-width: 80px;
  padding: 8px 16px;
}

.remaining-count {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 32px;
}

.result-section {
  border-top: 1px solid #e5edf5;
  padding-top: 20px;
  margin-top: 20px;
}

.result-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.result-header-content.is-mobile {
  flex-direction: column;
  align-items: flex-start;
}

.result-title {
  font-weight: 300;
  font-size: 18px;
  color: #061b31;
  letter-spacing: -0.18px;
  white-space: nowrap;
}

.product-spec-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background-color: #f8fafc;
  border-radius: 4px;
  border: 1px solid #e5edf5;
  white-space: nowrap;
  max-width: 100%;
  overflow-x: auto;
}

.info-label {
  font-size: 13px;
  color: #64748d;
  font-weight: 400;
}

.info-value {
  font-size: 13px;
  color: #061b31;
  font-weight: 400;
}

.charset-group {
  display: flex;
  gap: 24px;
  align-items: center;
  flex-wrap: wrap;
}

.charset-checkbox {
  margin-right: 0;
}

.action-buttons.is-mobile {
  width: 100%;
  justify-content: space-between;
}

.mobile-action-btn {
  flex: 1;
  padding: 8px 4px;
  font-size: 12px;
}

.result-table {
  margin-top: 16px;
}

.key-text {
  font-family: 'SourceCodePro', 'Courier New', monospace;
  font-size: 13px;
  color: #061b31;
  font-weight: 500;
  user-select: all;
}

.key-text.clickable {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.key-text.clickable:hover {
  background-color: rgba(83, 58, 253, 0.05);
  color: #533afd;
}

.key-text.clickable:active {
  background-color: rgba(83, 58, 253, 0.1);
  transform: scale(0.98);
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}
</style>