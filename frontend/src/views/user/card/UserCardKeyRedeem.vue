<template>
  <div class="cardkey-redeem">
    <el-card class="redeem-card" shadow="never" :body-style="{ padding: '24px' }">
      <div class="redeem-content">
        <div class="input-section" :class="{ 'is-mobile': isMobile }">
          <el-input v-model="cardKeyInput" placeholder="请输入卡密代码" clearable :size="isMobile ? 'default' : 'large'" @keyup.enter="handleRedeem"
            @clear="clearResult" class="cardkey-input" />
          <el-button color="#533afd" @click="handleRedeem" :loading="redeeming" class="redeem-btn" :size="isMobile ? 'default' : 'large'">
            兑换
          </el-button>
        </div>

        <div v-if="showResult" class="result-section">
          <el-divider content-position="left">兑换结果</el-divider>

          <div class="result-card" :class="resultClass">
            <div class="result-header">
              <el-icon :size="24" :color="resultIconColor">
                <component :is="resultIcon" />
              </el-icon>
              <span class="result-title">{{ resultTitle }}</span>
            </div>

            <div class="result-content">
              <el-descriptions :column="isMobile ? 1 : 2" border>
                <el-descriptions-item label="卡密代码">{{ maskCardKey(redeemResult.cardKey) }}</el-descriptions-item>
                <el-descriptions-item label="商品名称">{{ redeemResult.productName || '未知商品' }}</el-descriptions-item>
                <el-descriptions-item label="规格名称">{{ redeemResult.specificationName || '未知规格' }}</el-descriptions-item>
                <el-descriptions-item label="有效天数">{{ redeemResult.validDays ? `${redeemResult.validDays} 天` : '永久有效' }}</el-descriptions-item>
                <el-descriptions-item label="到期时间">{{ redeemResult.expiresAt ? formatDateTime(redeemResult.expiresAt) : '永久有效' }}</el-descriptions-item>
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

const redeeming = ref(false)
const isMobile = useIsMobile()

const showResult = ref(false)
const redeemResult = ref({})

const resultClass = computed(() => redeemResult.value.success ? 'result-success' : 'result-error')

const resultTitle = computed(() => redeemResult.value.success ? '卡密兑换成功' : '卡密兑换失败')

const resultIcon = computed(() => redeemResult.value.success ? CircleCheck : CircleClose)

const resultIconColor = computed(() => redeemResult.value.success ? '#67C23A' : '#F56C6C')

const handleRedeem = async () => {
  if (!cardKeyInput.value.trim()) {
    ElMessage.warning('请输入卡密代码')
    return
  }

  redeeming.value = true

  try {
    const response = await api.user.redeemCardKey(cardKeyInput.value.trim())

    if (response && response.code === 200) {
      if (response.data && response.data.success) {
        redeemResult.value = {
          ...response.data,
          cardKey: cardKeyInput.value.trim()
        }
        showResult.value = true
        ElNotification({
          title: '兑换成功',
          message: `商品: ${redeemResult.value.productName || '未知'}\n规格: ${redeemResult.value.specificationName || '未知'}`,
          type: 'success',
          duration: 5000
        })
      } else {
        redeemResult.value = {
          success: false,
          message: response.data?.message || '兑换失败',
          cardKey: cardKeyInput.value.trim()
        }
        showResult.value = true
        ElNotification({
          title: '兑换失败',
          message: response.data?.message || '兑换失败，请重试',
          type: 'error',
          duration: 4000
        })
      }
    } else {
      redeemResult.value = {
        success: false,
        message: response?.message || '兑换失败',
        cardKey: cardKeyInput.value.trim()
      }
      showResult.value = true
      ElNotification({
        title: '兑换失败',
        message: response?.message || '兑换失败，请重试',
        type: 'error',
        duration: 4000
      })
    }
  } catch (error) {
    redeemResult.value = {
      success: false,
      message: error.message || '网络错误，请重试',
      cardKey: cardKeyInput.value.trim()
    }
    showResult.value = true
    ElNotification({
      title: '兑换失败',
      message: error.message || '网络错误，请重试',
      type: 'error',
      duration: 4000
    })
  } finally {
    redeeming.value = false
  }
}

const clearResult = () => {
  showResult.value = false
  redeemResult.value = {}
}
</script>

<style scoped>
.cardkey-redeem {
  padding: 0;
  background-color: transparent;
  font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-feature-settings: "ss01";
}

.redeem-card {
  margin-bottom: 0;
  border-radius: 6px;
  border: 1px solid #e5edf5;
  box-shadow: rgba(23, 23, 23, 0.06) 0px 3px 6px;
}

.redeem-content {
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

.redeem-btn {
  padding: 0 24px;
  font-weight: 400;
}

.input-section.is-mobile .redeem-btn {
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
  .cardkey-redeem {
    padding: 0;
  }
}
</style>
