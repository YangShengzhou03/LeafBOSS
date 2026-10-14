<template>
  <div class="admin-dashboard">
    <el-card shadow="never" :body-style="{ padding: '20px' }">
      <!-- 管理员通知 -->
      <div v-if="adminNotices.length" class="notice-list">
        <el-alert
          v-for="n in adminNotices"
          :key="n.id"
          :title="n.content"
          :type="alertType(n.level)"
          show-icon
          closable
        />
      </div>

      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :lg="4" class="stat-col" id="tour-stat-users">
          <el-statistic title="用户数" :value="stats.userCount" />
        </el-col>
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

use([
  CanvasRenderer,
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const stats = ref({
  cardKeyCount: 0, totalCardKeys: 0, activationRate: 0,
  stockValue: 0, monthlyRevenue: 0, weeklyRevenue: 0,
  dailySales: 0, dailyRevenue: 0,
  lastMonthRevenue: 0, lastWeekRevenue: 0,
  yesterdaySales: 0, yesterdayRevenue: 0,
  userCount: 0
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

// 仪表盘漫游引导
const showDashboardTour = ref(false)

const checkDashboardTour = () => {
  const tourCompleted = localStorage.getItem('leafboss_tour_completed')
  
  if (!tourCompleted) {
    // 首次登录，启动仪表盘引导
    setTimeout(() => {
      showDashboardTour.value = true
    }, 800)
  }
}

const onDashboardTourFinish = () => {
  // 引导完成，设置标识
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
  // 总销量用紫色
  const series = [{
    name: '总销量', type: 'line', smooth: true,
    symbol: 'circle', symbolSize: 5,
    lineStyle: { width: 2, color: '#533afd' },
    itemStyle: { color: '#533afd' },
    data: totalCounts
  }]
  // 规格商品使用其他颜色（跳过紫色）
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
        yesterdayRevenue: data.yesterdayRevenue || 0,
        userCount: data.userCount || 0
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
  stats.value = { cardKeyCount: 0, totalCardKeys: 0, activationRate: 0, stockValue: 0, monthlyRevenue: 0, weeklyRevenue: 0, dailySales: 0, dailyRevenue: 0, lastMonthRevenue: 0, lastWeekRevenue: 0, yesterdaySales: 0, yesterdayRevenue: 0, userCount: 0 }
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

// 管理员通知
const adminNotices = ref([])
const alertType = (level) => ['success', 'warning', 'info', 'error'].includes(level) ? level : 'info'

const loadAdminNotices = async () => {
  try {
    const res = await api.user.getPublicNotices('管理员')
    if (res && res.code === 200) {
      adminNotices.value = res.data || []
    }
  } catch (e) {
    // 通知加载失败不影响仪表盘
  }
}

onMounted(() => {
  loadDashboardData()
  loadAdminNotices()
  checkDashboardTour()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 0;
}

.stat-col {
  margin: 24px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
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
}
</style>
