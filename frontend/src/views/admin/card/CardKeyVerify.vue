<template>
  <div class="cardkey-verify">
    <el-card class="verify-card" shadow="never" :body-style="{ padding: '24px' }">

      <div class="verify-content">
        <div class="input-section" :class="{ 'is-mobile': isMobile }">
          <el-input v-model="cardKeyInput" placeholder="请输入卡密代码" clearable :size="isMobile ? 'default' : 'large'" @keyup.enter="handleVerify"
            @clear="clearResult" class="cardkey-input" />
          <el-button color="#533afd" @click="handleVerify" :loading="verifying" class="verify-btn" :size="isMobile ? 'default' : 'large'">
            验证
          </el-button>
        </div>

        <div v-if="showResult" class="result-section">
          <el-divider content-position="left">验证结果</el-divider>

          <div class="result-card" :class="resultClass">
            <div class="result-header">
              <el-icon :size="24" :color="resultIconColor">
                <component :is="resultIcon" />
              </el-icon>
              <span class="result-title">{{ resultTitle }}</span>
            </div>

            <div class="result-content">
              <el-descriptions :column="isMobile ? 1 : 2" border>
                <el-descriptions-item label="卡密代码">{{ maskCardKey(cardKeyInfo.cardKey) }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="getStatusTagType(cardKeyInfo.status)">
                    {{ getStatusText(cardKeyInfo.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="商品">{{ cardKeyInfo.productName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="商品规格">{{ cardKeyInfo.productSpec || cardKeyInfo.specificationName ||
                  '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="价格">¥{{ cardKeyInfo.price || '0.00' }}</el-descriptions-item>
                <el-descriptions-item v-if="cardKeyInfo.agentId" label="代理商">
                  {{ cardKeyInfo.agentEmail ? `${cardKeyInfo.agentUsername}（${cardKeyInfo.agentEmail}）` : (cardKeyInfo.agentUsername || cardKeyInfo.agentId) }}
                </el-descriptions-item>
                <el-descriptions-item label="使用时间">{{ cardKeyInfo.activateTime ? formatDateTime(cardKeyInfo.activateTime) : '未使用' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ cardKeyInfo.createdAt ? formatDateTime(cardKeyInfo.createdAt) : '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { maskCardKey, formatDateTime } from '@/utils/utils.js'
import { useIsMobile } from '@/composables/useIsMobile.js'
import api from '@/services/api.js'

const cardKeyInput = ref('')

const verifying = ref(false)
const isMobile = useIsMobile()

const showResult = ref(false)
const cardKeyInfo = ref({})

const STATUS_CONFIG = {
  '未使用': { text: '未使用', tagType: 'success', class: 'result-success', icon: CircleCheck, color: '#67C23A' },
  '已使用': { text: '已使用', tagType: 'info', class: 'result-info', icon: CircleCheck, color: '#409EFF' },
  '已禁用': { text: '已禁用', tagType: 'danger', class: 'result-error', icon: CircleClose, color: '#F56C6C' },
  '已过期': { text: '已过期', tagType: 'danger', class: 'result-error', icon: CircleClose, color: '#F56C6C' }
}

const getStatusConfig = (status) => STATUS_CONFIG[status] || { text: status || '未知', tagType: 'info', class: 'result-error', icon: CircleClose, color: '#F56C6C' }

const getStatusText = (status) => getStatusConfig(status).text
const getStatusTagType = (status) => getStatusConfig(status).tagType

const resultClass = computed(() => getStatusConfig(cardKeyInfo.value.status).class)

const resultTitle = computed(() => {
  const status = cardKeyInfo.value.status
  const config = getStatusConfig(status)
  if (status === '已禁用' || status === '已过期' || status === '未知') return '卡密验证失败'
  return `卡密验证成功 - 卡密${config.text}`
})

const resultIcon = computed(() => getStatusConfig(cardKeyInfo.value.status).icon)

const resultIconColor = computed(() => getStatusConfig(cardKeyInfo.value.status).color)

const handleVerify = async () => {
  if (!cardKeyInput.value.trim()) {
    ElMessage.warning('请输入卡密代码')
    return
  }

  verifying.value = true

  try {
    const response = await api.admin.verifyCardKey(cardKeyInput.value.trim())

    if (response && response.code === 200) {
      if (response.data) {
        cardKeyInfo.value = response.data

        if (!cardKeyInfo.value.productSpec && cardKeyInfo.value.specificationName) {
          cardKeyInfo.value.productSpec = cardKeyInfo.value.specificationName
        }

        showResult.value = true
        
        // 使用 Notification 显示详细结果
        const status = cardKeyInfo.value.status
        const isSuccess = status === 'active' || status === '未使用'
        ElNotification({
          title: isSuccess ? '卡密验证成功' : (status === '未知' ? '卡密不存在' : '卡密验证成功'),
          message: `卡密: ${maskCardKey(cardKeyInfo.value.cardKey)}\n状态: ${getStatusText(status)}\n规格: ${cardKeyInfo.value.productSpec || '未设置'}`,
          type: isSuccess ? 'success' : (status === '未知' ? 'error' : 'warning'),
          duration: 5000
        })
      } else {
        cardKeyInfo.value = {
          cardKey: cardKeyInput.value.trim(),
          status: '未知'
        }
        showResult.value = true
        ElNotification({
          title: '卡密验证失败',
          message: '未找到该卡密信息',
          type: 'error',
          duration: 4000
        })
      }
    } else {
      cardKeyInfo.value = {
        cardKey: cardKeyInput.value.trim(),
        status: '未知'
      }
      showResult.value = true
      ElNotification({
        title: '卡密验证失败',
        message: response?.message || '验证失败，请重试',
        type: 'error',
        duration: 4000
      })
    }
  } catch (error) {
    cardKeyInfo.value = {
      cardKey: cardKeyInput.value.trim(),
      status: '未知'
    }
    showResult.value = true
    ElNotification({
      title: '卡密验证失败',
      message: error.message || '网络错误，请重试',
      type: 'error',
      duration: 4000
    })
  } finally {
    verifying.value = false
  }
}

const clearResult = () => {
  showResult.value = false
  cardKeyInfo.value = {}
}
</script>

<style scoped>
.cardkey-verify {
  padding: 0;
  background-color: transparent;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.verify-card {
  margin-bottom: 0;
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.verify-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-section {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

.input-section.is-mobile {
  flex-direction: column;
  width: 100%;
}

.cardkey-input {
  width: 400px;
}

.input-section.is-mobile .cardkey-input {
  width: 100%;
}

.verify-btn {
  padding: 0 24px;
  font-weight: 400;
}

.input-section.is-mobile .verify-btn {
  width: 100%;
}

.result-section {
  animation: fadeIn 0.5s ease-in-out;
}

.result-card {
  padding: 20px;
  border-radius: 6px;
  border: 1px solid;
  background-color: #ffffff;
}

.result-success {
  border-color: #d1f5f0;
  background-color: #eefbfa;
}

.result-info {
  border-color: #e2e8f0;
  background-color: #f1f5f9;
}

.result-error {
  border-color: #fde2eb;
  background-color: #fef0f0;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.result-title {
  font-size: 18px;
  font-weight: 300;
  color: #061b31;
  letter-spacing: -0.18px;
}

.result-content {
  margin-top: 16px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .cardkey-verify {
    padding: 0;
  }
}
</style>