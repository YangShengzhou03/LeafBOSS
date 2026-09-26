<template>
  <div class="user-dashboard">
    <!-- 用户端仪表盘 -->
    <template v-if="!isAdmin">
      <!-- 欢迎横幅 -->
      <div class="welcome-banner">
        <h2 class="welcome-title">{{ displayName }}，{{ timeGreeting }}</h2>
        <p class="welcome-subtitle">欢迎使用 LEAF-BOSS 业务运营系统</p>
      </div>

      <!-- 系统通知 -->
      <div v-if="myNotices.length" class="notice-list">
        <el-alert
          v-for="n in myNotices"
          :key="n.id"
          :title="n.content"
          :type="alertType(n.level)"
          show-icon
          closable
        />
      </div>

      <!-- 统计卡片（仅代理商显示） -->
      <el-row v-if="isAgent" :gutter="16" class="stats-row">
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col">
          <el-statistic title="我的卡密总数" :value="userStats.totalCardKeys" />
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col">
          <el-statistic title="未使用卡密" :value="userStats.unusedCardKeys" />
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col">
          <el-statistic title="已使用卡密" :value="userStats.usedCardKeys" />
        </el-col>
      </el-row>

      <!-- 代理商品（仅代理商显示） -->
      <el-card v-if="isAgent" shadow="never" class="product-card">
        <template #header>
          <div class="card-header">
            <span>代理商品</span>
          </div>
        </template>
        <el-table :data="agentProducts" stripe>
          <el-table-column prop="productName" label="商品" width="200" :show-overflow-tooltip="true" />
          <el-table-column prop="specNames" label="规格" width="360" :show-overflow-tooltip="true">
            <template #default="{ row }">
              <span v-if="row.specNames">{{ row.specNames }}</span>
              <span v-else style="color: var(--el-text-color-secondary)">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="remaining" label="余额" width="150" align="center" />
          <el-table-column prop="grantedAt" label="代理时间" width="160">
            <template #default="{ row }">
              {{ row.grantedAt ? (row.grantedAt || '').replace('T', ' ') : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="expiresAt" label="到期时间" width="160">
            <template #default="{ row }">
              {{ row.expiresAt ? (row.expiresAt || '').replace('T', ' ') : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="150">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '已授权' : '已吊销' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 我拥有的商品 -->
      <el-card shadow="never" class="product-card">
        <template #header>
          <div class="card-header">
            <span>我拥有的商品</span>
          </div>
        </template>
        <el-table :data="myProducts" stripe v-loading="loading">
          <el-table-column prop="productName" label="商品" width="200" :show-overflow-tooltip="true" />
          <el-table-column label="规格" width="200" :show-overflow-tooltip="true">
            <template #default="{ row }">
              <span v-if="row.specName">{{ row.specName }}</span>
              <span v-else style="color: var(--el-text-color-secondary)">-</span>
            </template>
          </el-table-column>
          <el-table-column label="有效期（天）" width="150">
            <template #default="{ row }">
              <span v-if="row.validDays">{{ row.validDays }}</span>
              <span v-else style="color: var(--el-text-color-secondary)">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="cardKey" label="卡密" width="200" :show-overflow-tooltip="true">
            <template #default="{ row }">
              <span v-if="row.cardKey" class="card-key-text" @click="copyCardKey(row.cardKey)">{{ row.cardKey }}</span>
              <span v-else style="color: var(--el-text-color-secondary)">管理员直授</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="150">
            <template #default="{ row }">
              <el-tag :type="productStatusType(row)">
                {{ productStatusText(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="授权时间" width="160">
            <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ') }}</template>
          </el-table-column>
          <el-table-column prop="expiresAt" label="到期时间" width="160">
            <template #default="{ row }">{{ (row.expiresAt || '').replace('T', ' ') }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 管理员仪表盘（保持不变） -->
    <el-card v-else shadow="never" :body-style="{ padding: '20px' }">
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col" id="tour-stat-remaining">
          <el-statistic title="剩余卡密" :value="stats.cardKeyCount" />
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col" id="tour-stat-total">
          <el-statistic title="总卡密数" :value="stats.totalCardKeys" />
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col" id="tour-stat-monthly">
          <el-statistic title="当月收入" :value="stats.monthlyRevenue" :precision="2" prefix="¥">
            <template #suffix>
              <span class="trend-text">上月 ¥{{ formatCurrency(stats.lastMonthRevenue) }}</span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col" id="tour-stat-daily-sales">
          <el-statistic title="日销售数量" :value="stats.dailySales">
            <template #suffix>
              <span class="trend-text">昨日 {{ stats.yesterdaySales }}</span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col" id="tour-stat-stock">
          <el-statistic title="仓库总值" :value="stats.stockValue" :precision="2" prefix="¥" />
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col" id="tour-stat-weekly">
          <el-statistic title="本周收入" :value="stats.weeklyRevenue" :precision="2" prefix="¥">
            <template #suffix>
              <span class="trend-text">上周 ¥{{ formatCurrency(stats.lastWeekRevenue) }}</span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col" id="tour-stat-daily">
          <el-statistic title="日收入" :value="stats.dailyRevenue" :precision="2" prefix="¥">
            <template #suffix>
              <span class="trend-text">昨日 ¥{{ formatCurrency(stats.yesterdayRevenue) }}</span>
            </template>
          </el-statistic>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="charts-row">
        <el-col :xs="24" :lg="12" class="chart-col" id="tour-chart-revenue">
          <el-card>
            <template #header>
              <div class="chart-header">
                <span>收入趋势</span>
                <el-segmented v-model="trendDays" :options="trendOptions" @change="loadDailyRevenueTrend" />
              </div>
            </template>
            <div class="chart-container">
              <el-empty v-if="dailyTrend.length === 0" description="暂无收入数据" :image-size="80" />
              <v-chart v-else :option="revenueTrendOption" autoresize class="chart-content" />
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12" class="chart-col" id="tour-chart-sales">
          <el-card :body-style="{ padding: '16px' }">
            <template #header>
              <div class="chart-header">
                <span>销量趋势</span>
                <el-segmented v-model="trendDays" :options="trendOptions" @change="loadDailyRevenueTrend" />
              </div>
            </template>
            <div class="chart-container">
              <el-empty v-if="dailyTrend.length === 0" description="暂无销量数据" :image-size="80" />
              <v-chart v-else :option="salesTrendOption" autoresize class="chart-content" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="charts-row">
        <el-col :xs="24" :lg="12" class="chart-col" id="tour-chart-stock">
          <el-card>
            <template #header>
              <div class="chart-header"><span>库存卡密</span></div>
            </template>
            <div class="chart-container">
              <el-empty v-if="specDistribution.length === 0" description="暂无库存数据" :image-size="80" />
              <v-chart v-else :option="pieChartOption" autoresize class="chart-content" />
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12" class="chart-col" id="tour-chart-today">
          <el-card :body-style="{ padding: '16px' }">
            <template #header>
              <div class="chart-header"><span>当日售出</span></div>
            </template>
            <div class="chart-container">
              <el-empty v-if="todaySalesDistribution.length === 0" description="暂无售出数据" :image-size="80" />
              <v-chart v-else :option="salesPieChartOption" autoresize class="chart-content" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 仪表盘漫游引导 -->
    <el-tour v-model="showDashboardTour" @finish="onDashboardTourFinish">
      <el-tour-step
        target="#tour-stat-remaining"
        title="剩余卡密"
        description="显示当前未使用的卡密数量，是可销售库存。"
      />
      <el-tour-step
        target="#tour-stat-monthly"
        title="当月收入"
        description="本月的卡密销售收入，对比上月数据可查看增长趋势。"
      />
      <el-tour-step
        target="#tour-stat-daily-sales"
        title="日销售数量"
        description="今日激活的卡密数量，对比昨日数据了解销售波动。"
      />
      <el-tour-step
        target="#tour-chart-revenue"
        title="收入趋势图"
        description="折线图展示近期收入变化趋势，可切换查看7天、15天、30天数据。"
      />
    </el-tour>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import api from '../../services/api'
import store from '../../utils/store.js'

const isAdmin = computed(() => store.state.isAdmin)
const isAgent = computed(() => store.state.user?.role === 'agent')
const displayName = computed(() => {
  const user = store.state.user
  return user?.nickname || user?.username || user?.email || '用户'
})
const timeGreeting = computed(() => {
  const h = new Date().getHours()
  if (h >= 5 && h < 8) return '早上好'
  if (h >= 8 && h < 11) return '上午好'
  if (h >= 11 && h < 13) return '中午好'
  if (h >= 13 && h < 18) return '下午好'
  if (h >= 18 && h < 22) return '晚上好'
  return '夜深了'
})

use([
  CanvasRenderer,
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

// 用户端数据
const userStats = ref({
  totalCardKeys: 0,
  unusedCardKeys: 0,
  usedCardKeys: 0
})
const myProducts = ref([])
const myNotices = ref([])

const alertType = (level) => ['success', 'warning', 'info', 'error'].includes(level) ? level : 'info'

const copyCardKey = async (key) => {
  try {
    await navigator.clipboard.writeText(key)
    ElMessage.success('卡密已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const isProductExpired = (row) => row.expiresAt && new Date(row.expiresAt) < new Date()
const productStatusType = (row) => {
  if (row.status !== 1) return 'danger'
  return isProductExpired(row) ? 'warning' : 'success'
}
const productStatusText = (row) => {
  if (row.status !== 1) return '已吊销'
  return isProductExpired(row) ? '已过期' : '使用中'
}
const agentProducts = ref([])

const loadUserDashboard = async () => {
  try {
    // 卡密统计（仅代理商需要，避免普通用户拉取全量卡密）
    if (isAgent.value) {
      const all = []
      const size = 500
      for (let page = 1; ; page++) {
        const listRes = await api.user.getCardKeyList({ page, size })
        if (listRes && listRes.data) {
          const records = listRes.data.records || []
          all.push(...records)
          if (records.length < size) {
            userStats.value.totalCardKeys = listRes.data.total || all.length
            break
          }
        } else {
          break
        }
      }
      userStats.value.unusedCardKeys = all.filter(k => k.status === '未使用').length
      userStats.value.usedCardKeys = all.filter(k => k.status === '已使用' || k.status === 'used').length
    }
    // 获取代理商品（后端返回 productName/specNames/remaining/expiresAt/grantedAt/status）
    const agentRes = await api.user.getAuthorizedProducts()
    if (agentRes && agentRes.code === 200 && agentRes.data) {
      agentProducts.value = agentRes.data
    }
    // 获取我拥有的商品
    const productsRes = await api.user.userGetMyProducts()
    if (productsRes && productsRes.code === 200 && productsRes.data) {
      myProducts.value = productsRes.data
    }
    // 获取面向当前角色的通知
    const noticesRes = await api.user.getPublicNotices(isAgent.value ? '代理商' : '普通用户')
    if (noticesRes && noticesRes.code === 200) {
      myNotices.value = noticesRes.data || []
    }
  } catch (error) {
    ElMessage.error(error.message || '加载用户仪表盘失败')
  }
}

// 管理员数据（保持不变）
const stats = ref({
  cardKeyCount: 0, totalCardKeys: 0, activationRate: 0,
  stockValue: 0, monthlyRevenue: 0, weeklyRevenue: 0,
  dailySales: 0, dailyRevenue: 0,
  lastMonthRevenue: 0, lastWeekRevenue: 0,
  yesterdaySales: 0, yesterdayRevenue: 0
})
const loading = ref(false)
const specDistribution = ref([])
const todaySalesDistribution = ref([])
const dailyTrend = ref([])
const specTrend = ref([])
const trendDays = ref(7)
const trendOptions = [
  { label: '近7天', value: 7 },
  { label: '近15天', value: 15 },
  { label: '近30天', value: 30 }
]

const showDashboardTour = ref(false)

const checkDashboardTour = () => {
  const tourCompleted = localStorage.getItem('leafboss_tour_completed')
  if (!tourCompleted) {
    setTimeout(() => {
      showDashboardTour.value = true
    }, 800)
  }
}

const onDashboardTourFinish = () => {
  localStorage.setItem('leafboss_tour_completed', 'true')
}

const colors = ['#533afd', '#15be53', '#9b6829', '#ea2261', '#64748d', '#f96bee', '#061b31']
const getSpecColor = (index) => colors[index % colors.length]

const revenueTrendOption = computed(() => {
  const dates = dailyTrend.value.map(d => d.date.slice(5))
  const revenues = dailyTrend.value.map(d => d.revenue)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 16, bottom: 30, top: 16 },
    xAxis: {
      type: 'category', data: dates,
      axisLabel: { fontSize: 10, color: '#94a3b8' },
      axisLine: { lineStyle: { color: '#e5edf5' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 10, color: '#94a3b8', formatter: '¥{value}' },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series: [{
      name: '收入 (¥)', type: 'line', smooth: true,
      symbol: 'circle', symbolSize: 4,
      lineStyle: { width: 2, color: '#533afd' },
      itemStyle: { color: '#533afd' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(83,58,253,0.15)' },
            { offset: 1, color: 'rgba(83,58,253,0.01)' }
          ]
        }
      },
      data: revenues
    }]
  }
})

const salesTrendOption = computed(() => {
  const dates = dailyTrend.value.map(d => d.date.slice(5))
  const totalCounts = dailyTrend.value.map(d => d.count)
  const series = [{
    name: '总销量', type: 'line', smooth: true,
    symbol: 'circle', symbolSize: 5,
    lineStyle: { width: 2, color: '#533afd' },
    itemStyle: { color: '#533afd' },
    data: totalCounts
  }]
  const specColors = ['#15be53', '#9b6829', '#ea2261', '#64748d', '#f96bee', '#061b31']
  specTrend.value.forEach((spec, i) => {
    series.push({
      name: spec.name, type: 'line', smooth: true,
      symbol: 'circle', symbolSize: 4,
      lineStyle: { width: 1.5, color: specColors[i % specColors.length] },
      itemStyle: { color: specColors[i % specColors.length] },
      data: spec.data
    })
  })
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0, right: 0,
      textStyle: { fontSize: 11, color: '#64748d' }
    },
    grid: { left: 40, right: 16, bottom: 30, top: 36 },
    xAxis: {
      type: 'category', data: dates,
      axisLabel: { fontSize: 10, color: '#94a3b8' },
      axisLine: { lineStyle: { color: '#e5edf5' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 10, color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    series
  }
})

const pieChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c}张 ({d}%)' },
  series: [{
    name: '规格', type: 'pie', radius: ['50%', '80%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 4, borderColor: '#ffffff', borderWidth: 2 },
    label: { show: true, formatter: '{b}\n{c}张 ({d}%)', fontSize: 12, color: '#061b31' },
    emphasis: { label: { show: true, fontSize: 14 } },
    labelLine: { show: true, length: 10, length2: 5 },
    data: specDistribution.value.map((item, index) => ({
      value: item.count, name: item.name,
      itemStyle: { color: getSpecColor(index) }
    }))
  }]
}))

const salesPieChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c}张 ({d}%)' },
  series: [{
    name: '售出规格', type: 'pie', radius: ['50%', '80%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 4, borderColor: '#ffffff', borderWidth: 2 },
    label: { show: true, formatter: '{b}\n{c}张 ({d}%)', fontSize: 12, color: '#061b31' },
    emphasis: { label: { show: true, fontSize: 14 } },
    labelLine: { show: true, length: 10, length2: 5 },
    data: todaySalesDistribution.value.map((item, index) => ({
      value: item.count, name: item.name,
      itemStyle: { color: getSpecColor(index + 3) }
    }))
  }]
}))

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '0.00'
  return parseFloat(value).toFixed(2)
}

const loadDashboardData = async () => {
  try {
    loading.value = true
    const response = await api.admin.getDashboardStats()
    if (response && response.data) {
      const data = response.data
      stats.value = {
        cardKeyCount: data.cardKeyCount || 0,
        totalCardKeys: data.totalCardKeys || 0,
        activationRate: data.activationRate || 0,
        stockValue: data.stockValue || 0,
        monthlyRevenue: data.monthlyRevenue || 0,
        weeklyRevenue: data.weeklyRevenue || 0,
        dailySales: data.dailySales || 0,
        dailyRevenue: data.dailyRevenue || 0,
        lastMonthRevenue: data.lastMonthRevenue || 0,
        lastWeekRevenue: data.lastWeekRevenue || 0,
        yesterdaySales: data.yesterdaySales || 0,
        yesterdayRevenue: data.yesterdayRevenue || 0
      }
      await Promise.all([
        loadSpecDistribution(),
        loadTodaySalesDistribution(),
        loadDailyRevenueTrend()
      ])
    } else {
      resetAllData()
      ElMessage.warning('仪表盘数据为空，请检查数据配置')
    }
  } catch (error) {
    if (error.response && error.response.status === 401) {
      ElMessage.warning('登录过期，请重新登录')
    } else {
      ElMessage.error(error.message || '加载仪表盘数据失败')
    }
    resetAllData()
  } finally {
    loading.value = false
  }
}

const resetAllData = () => {
  stats.value = { cardKeyCount: 0, totalCardKeys: 0, activationRate: 0, stockValue: 0, monthlyRevenue: 0, weeklyRevenue: 0, dailySales: 0, dailyRevenue: 0, lastMonthRevenue: 0, lastWeekRevenue: 0, yesterdaySales: 0, yesterdayRevenue: 0 }
  specDistribution.value = []
  todaySalesDistribution.value = []
  dailyTrend.value = []
  specTrend.value = []
}

const loadDailyRevenueTrend = async () => {
  try {
    const response = await api.admin.getDailyRevenueTrend({ days: trendDays.value })
    if (response && response.code === 200 && response.data) {
      dailyTrend.value = response.data.trend || []
      specTrend.value = response.data.specTrend || []
    } else {
      dailyTrend.value = []
      specTrend.value = []
    }
  } catch {
    dailyTrend.value = []
    specTrend.value = []
  }
}

const loadTodaySalesDistribution = async () => {
  try {
    const response = await api.admin.getTodaySalesDistribution()
    if (response && response.code === 200 && response.data) {
      todaySalesDistribution.value = response.data
    } else {
      todaySalesDistribution.value = []
    }
  } catch {
    todaySalesDistribution.value = []
  }
}

const loadSpecDistribution = async () => {
  try {
    const response = await api.admin.getSpecificationDTOs()
    if (response && response.code === 200 && response.data) {
      const specifications = response.data
      const totalUnusedCards = specifications.reduce((total, spec) => total + (spec.unusedKeys || 0), 0)
      stats.value.cardKeyCount = totalUnusedCards
      specDistribution.value = specifications
        .filter(spec => spec.unusedKeys > 0)
        .map(spec => ({
          name: spec.name,
          count: spec.unusedKeys || 0
        }))
        .sort((a, b) => b.count - a.count)
    } else {
      specDistribution.value = []
    }
  } catch {
    specDistribution.value = []
  }
}

onMounted(() => {
  if (isAdmin.value) {
    loadDashboardData()
    checkDashboardTour()
  } else {
    loadUserDashboard()
  }
})
</script>

<style scoped>
/* 用户端样式 */
.welcome-banner {
  padding: 24px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
  margin: 0;
}

.card-key-text {
  cursor: pointer;
  font-family: monospace;
}
.card-key-text:hover {
  color: var(--el-color-primary);
}

.welcome-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #061b31;
}

.welcome-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #94a3b8;
}

.stats-row {
  margin-bottom: 16px;
}

.product-card {
  margin-bottom: 16px;
}

.product-card + .product-card {
  margin-top: 16px;
}

/* 管理员端样式 */
.stat-col {
  margin: 24px;
}

.chart-col {
  margin-bottom: 8px;
}

.trend-text {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 8px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.charts-row {
  margin-top: 12px;
}

.chart-container {
  width: 100%;
  height: 300px;
  position: relative;
}

.chart-content {
  width: 100%;
  height: 100%;
}

@media (max-width: 768px) {
  .chart-container {
    height: 250px;
  }

  .welcome-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .welcome-actions {
    width: 100%;
  }

  .welcome-actions .el-button {
    flex: 1;
  }
}
</style>
