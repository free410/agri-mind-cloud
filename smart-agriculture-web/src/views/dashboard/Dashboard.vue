<template>
  <div class="dashboard-page page-shell" v-loading="loading">
    <section class="dashboard-hero page-banner">
      <div class="hero-main page-banner__main">
        <span class="page-banner__badge">运营总览</span>
        <p class="page-banner__desc">聚焦监测趋势、预警状态与闭环处置的核心信息。</p>
      </div>

      <div class="hero-actions page-banner__actions">
        <span class="hero-pill success">比赛演示模式</span>
        <span class="hero-pill">本地环境</span>
        <span class="hero-user">{{ displayName }}</span>
        <el-button type="primary" :icon="Refresh" @click="handleRefresh">刷新数据</el-button>
      </div>
    </section>

    <section class="core-grid">
      <article class="page-card core-card trend-main">
        <header class="card-head">
          <div>
            <h3>最近7天环境趋势</h3>
            <p>展示温度、湿度、土壤湿度趋势与异常识别结果。</p>
          </div>
          <div class="metric-tabs">
            <button
              v-for="item in metricOptions"
              :key="item.key"
              type="button"
              :class="{ active: selectedMetricKey === item.key }"
              @click="selectedMetricKey = item.key"
            >
              {{ item.label }}
            </button>
          </div>
        </header>

        <div class="trend-metrics">
          <div class="trend-metric-box">
            <span>当前值</span>
            <strong>{{ formatMetric(currentMetricValue, selectedMetric.unit) }}</strong>
          </div>
          <div class="trend-metric-box">
            <span>区间均值</span>
            <strong>{{ formatMetric(currentMetricAverage, selectedMetric.unit) }}</strong>
          </div>
          <div class="trend-metric-box warning">
            <span>异常点</span>
            <strong>{{ currentMetricAnomalyCount }} 个</strong>
          </div>
        </div>

        <div ref="trendChartRef" class="trend-chart"></div>

        <div class="trend-summary-row">
          <span class="summary-chip">峰值 {{ formatMetric(currentMetricPeak, selectedMetric.unit) }}</span>
          <span class="summary-chip">谷值 {{ formatMetric(currentMetricValley, selectedMetric.unit) }}</span>
          <span class="summary-chip">{{ currentMetricDirection }}</span>
        </div>
      </article>

      <article class="page-card core-card insight-main">
        <header class="card-head">
          <div>
            <h3>智能闭环分析</h3>
            <p>统一呈现风险概览、AI建议与巡检结果，避免碎片化卡片堆叠。</p>
          </div>
          <el-tag :type="riskLevelTag" effect="plain">{{ riskLevelText }}</el-tag>
        </header>

        <div class="insight-body">
          <section class="insight-section score-section">
            <div class="score-left">
              <span>综合风险分</span>
              <strong>{{ riskScore }}</strong>
            </div>
            <div class="score-right">
              <div class="score-progress-track">
                <div class="score-progress-bar" :style="{ width: `${riskScore}%` }"></div>
              </div>
              <div class="score-meta">
                <span>高风险 {{ summary.highRiskWarningCount || 0 }} 条</span>
                <span>待处理 {{ summary.pendingWarningCount || 0 }} 条</span>
                <span>闭环率 {{ formatPercent(summary.closureRate) }}</span>
              </div>
            </div>
          </section>

          <section class="insight-section">
            <h4>关键结论</h4>
            <ul class="bullet-list">
              <li v-for="item in riskConclusions" :key="item">{{ item }}</li>
            </ul>
          </section>

          <section class="insight-section">
            <h4>AI建议摘要</h4>
            <ul class="suggest-list">
              <li v-for="(item, index) in aiSuggestions" :key="`${index}-${item}`">
                <span class="suggest-index">{{ index + 1 }}</span>
                <span class="suggest-text">{{ item }}</span>
              </li>
            </ul>
          </section>

          <section class="insight-section inspection-section">
            <h4>最新巡检结果</h4>
            <div class="inspection-grid">
              <div class="inspection-item">
                <span>最近巡检</span>
                <strong>{{ latestInspectionText }}</strong>
              </div>
              <div class="inspection-item">
                <span>今日巡检次数</span>
                <strong>{{ inspectionSummary.todayInspectionCount || 0 }}</strong>
              </div>
              <div class="inspection-item">
                <span>今日新增预警</span>
                <strong>{{ inspectionSummary.todayTriggeredWarningCount || 0 }}</strong>
              </div>
              <div class="inspection-item">
                <span>任务状态</span>
                <strong>{{ inspectionStatusText }}</strong>
              </div>
            </div>
          </section>
        </div>
      </article>
    </section>

    <section class="bottom-grid">
      <article class="page-card bottom-card">
        <header class="card-head">
          <div>
            <h3>预警状态分布</h3>
            <p>查看待确认、处理中、已处理、已闭环的实时结构。</p>
          </div>
        </header>
        <div ref="warningStatusChartRef" class="sub-chart"></div>
        <div class="status-chip-grid">
          <div v-for="item in warningStatusPercentList" :key="item.name" class="status-chip-item">
            <span>{{ item.name }}</span>
            <strong>{{ item.percent }}%</strong>
          </div>
        </div>
      </article>

      <article class="page-card bottom-card">
        <header class="card-head">
          <div>
            <h3>最近7天闭环趋势</h3>
            <p>展示新增预警、闭环数量和闭环率变化。</p>
          </div>
        </header>
        <div ref="closureTrendChartRef" class="sub-chart"></div>
        <div class="status-chip-grid">
          <div class="status-chip-item">
            <span>今日预警</span>
            <strong>{{ summary.todayWarningCount || 0 }}</strong>
          </div>
          <div class="status-chip-item">
            <span>已闭环</span>
            <strong>{{ summary.closedWarningCount || 0 }}</strong>
          </div>
          <div class="status-chip-item">
            <span>平均闭环时长</span>
            <strong>{{ summary.avgClosureMinutes || 0 }} 分钟</strong>
          </div>
        </div>
      </article>

      <article class="page-card bottom-card">
        <header class="card-head">
          <div>
            <h3>设备状态概览</h3>
            <p>结合预警与巡检结果，呈现设备在线与稳定情况。</p>
          </div>
        </header>
        <div ref="deviceStatusChartRef" class="sub-chart"></div>
        <div class="status-chip-grid">
          <div class="status-chip-item">
            <span>设备总数</span>
            <strong>{{ summary.deviceCount || 0 }}</strong>
          </div>
          <div class="status-chip-item">
            <span>在线设备</span>
            <strong>{{ deviceOverview.onlineCount }}</strong>
          </div>
          <div class="status-chip-item">
            <span>离线关注</span>
            <strong>{{ deviceOverview.offlineCount }}</strong>
          </div>
        </div>
      </article>

      <article class="page-card bottom-card warning-list-card">
        <header class="card-head">
          <div>
            <h3>最近预警列表</h3>
            <p>仅展示最新重点预警，便于比赛讲解闭环流程。</p>
          </div>
        </header>
        <div class="warning-list">
          <div
            v-for="(item, index) in recentWarningRows"
            :key="`${item.id || 'empty'}-${index}`"
            class="warning-row"
            :class="{ placeholder: item.placeholder }"
          >
            <template v-if="!item.placeholder">
              <div class="warning-row-main">
                <strong>{{ item.warningTypeName || '预警' }}</strong>
                <span>{{ item.farmlandName || '-' }}</span>
              </div>
              <div class="warning-row-meta">
                <el-tag size="small" :type="getLevelTag(item.levelName)" effect="plain">
                  {{ item.levelName || '未知' }}
                </el-tag>
                <span>{{ item.statusName || '-' }}</span>
                <span>{{ formatDateTime(item.triggerTime) }}</span>
              </div>
            </template>
            <template v-else>
              <div class="warning-row-main">
                <strong>暂无新增预警</strong>
                <span>当前状态稳定</span>
              </div>
            </template>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getDashboardHomeApi } from '@/api/dashboard'

const loading = ref(false)
const trendChartRef = ref(null)
const warningStatusChartRef = ref(null)
const closureTrendChartRef = ref(null)
const deviceStatusChartRef = ref(null)
const selectedMetricKey = ref('temperature')

const metricOptions = [
  { key: 'temperature', label: '平均温度', index: 0, unit: '°C', color: '#1f8f5f' },
  { key: 'humidity', label: '平均湿度', index: 1, unit: '%', color: '#4c8bf5' },
  { key: 'soilMoisture', label: '平均土壤湿度', index: 2, unit: '%', color: '#f2b544' }
]

const defaultHomeData = () => ({
  summary: {
    farmlandCount: 0,
    cropCount: 0,
    deviceCount: 0,
    todayWarningCount: 0,
    highRiskWarningCount: 0,
    pendingWarningCount: 0,
    closedWarningCount: 0,
    closureRate: 0,
    avgClosureMinutes: 0
  },
  environmentTrend: { xAxis: [], series: [] },
  warningStatusList: [],
  closureTrend: { xAxis: [], series: [] },
  aiSuggestionList: [],
  highRiskWarningQueue: [],
  inspectionSummary: {
    latestInspectionTime: null,
    todayInspectionCount: 0,
    todayTriggeredWarningCount: 0,
    taskStatus: 'STOPPED'
  }
})

const homeData = ref(defaultHomeData())
const userStore = useUserStore()
const chartInstances = new Map()
const summary = computed(() => homeData.value.summary || defaultHomeData().summary)
const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统管理员')

const selectedMetric = computed(() => {
  return metricOptions.find((item) => item.key === selectedMetricKey.value) || metricOptions[0]
})

const envSeriesList = computed(() => {
  return Array.isArray(homeData.value.environmentTrend?.series) ? homeData.value.environmentTrend.series : []
})

const selectedSeriesData = computed(() => {
  const rawSeries = envSeriesList.value[selectedMetric.value.index]
  if (!rawSeries || !Array.isArray(rawSeries.data)) {
    return []
  }
  return rawSeries.data.map((item) => Number(item ?? 0)).filter((item) => Number.isFinite(item))
})

const currentMetricValue = computed(() => {
  const data = selectedSeriesData.value
  return data.length ? data[data.length - 1] : 0
})

const currentMetricAverage = computed(() => {
  const data = selectedSeriesData.value
  if (!data.length) {
    return 0
  }
  const total = data.reduce((sum, item) => sum + item, 0)
  return total / data.length
})

const currentMetricAnomalyCount = computed(() => {
  const data = selectedSeriesData.value
  if (data.length < 2) {
    return 0
  }
  const avg = currentMetricAverage.value
  const variance = data.reduce((sum, item) => sum + (item - avg) ** 2, 0) / data.length
  const std = Math.sqrt(variance)
  const threshold = std * 1.2
  return data.filter((item) => Math.abs(item - avg) > threshold).length
})

const currentMetricPeak = computed(() => {
  const data = selectedSeriesData.value
  return data.length ? Math.max(...data) : 0
})

const currentMetricValley = computed(() => {
  const data = selectedSeriesData.value
  return data.length ? Math.min(...data) : 0
})

const currentMetricDirection = computed(() => {
  const data = selectedSeriesData.value
  if (data.length < 2) {
    return '趋势稳定'
  }
  const first = data[0]
  const last = data[data.length - 1]
  if (last > first + 0.2) {
    return '趋势上行'
  }
  if (last < first - 0.2) {
    return '趋势下行'
  }
  return '趋势平稳'
})

const riskScore = computed(() => {
  const today = Number(summary.value.todayWarningCount || 0)
  const highRisk = Number(summary.value.highRiskWarningCount || 0)
  const pending = Number(summary.value.pendingWarningCount || 0)
  const closureRate = Number(summary.value.closureRate || 0)
  const raw = 45 + highRisk * 9 + pending * 6 + Math.max(today - highRisk - pending, 0) * 2 - closureRate * 0.35
  return Math.max(0, Math.min(100, Math.round(raw)))
})

const riskLevelText = computed(() => {
  if (riskScore.value >= 75) {
    return '高风险'
  }
  if (riskScore.value >= 45) {
    return '中风险'
  }
  return '低风险'
})

const riskLevelTag = computed(() => {
  if (riskScore.value >= 75) {
    return 'danger'
  }
  if (riskScore.value >= 45) {
    return 'warning'
  }
  return 'success'
})

const riskConclusions = computed(() => {
  return [
    `今日预警 ${summary.value.todayWarningCount || 0} 条，其中高风险 ${summary.value.highRiskWarningCount || 0} 条。`,
    `待处理预警 ${summary.value.pendingWarningCount || 0} 条，需优先推进状态流转。`,
    `当前闭环率 ${formatPercent(summary.value.closureRate)}，平均闭环时长 ${summary.value.avgClosureMinutes || 0} 分钟。`
  ]
})

const aiSuggestions = computed(() => {
  const list = Array.isArray(homeData.value.aiSuggestionList) ? homeData.value.aiSuggestionList : []
  if (list.length) {
    return list.slice(0, 3)
  }
  return [
    '优先处理高风险预警，保持处置链路可追溯。',
    '建议对重复预警设备进行一次联动巡检。',
    '处理完成后及时闭环，提升今日闭环率。'
  ]
})

const inspectionSummary = computed(() => {
  return homeData.value.inspectionSummary || defaultHomeData().inspectionSummary
})

const latestInspectionText = computed(() => {
  return formatDateTime(inspectionSummary.value.latestInspectionTime) || '暂无'
})

const inspectionStatusText = computed(() => {
  return inspectionSummary.value.taskStatus === 'RUNNING' ? '运行中' : '已停止'
})

const warningStatusList = computed(() => {
  const list = Array.isArray(homeData.value.warningStatusList) ? homeData.value.warningStatusList : []
  return list.map((item) => ({
    name: item.name || '未知',
    value: Number(item.value || 0)
  }))
})

const warningStatusPercentList = computed(() => {
  const total = warningStatusList.value.reduce((sum, item) => sum + item.value, 0)
  return warningStatusList.value.map((item) => ({
    ...item,
    percent: total ? Math.round((item.value * 100) / total) : 0
  }))
})

const highRiskQueue = computed(() => {
  return Array.isArray(homeData.value.highRiskWarningQueue) ? homeData.value.highRiskWarningQueue : []
})

const recentWarningRows = computed(() => {
  const rows = highRiskQueue.value.slice(0, 4).map((item) => ({
    id: item.id,
    warningTypeName: item.warningTypeName,
    levelName: item.levelName,
    statusName: item.statusName,
    farmlandName: item.farmlandName,
    triggerTime: item.triggerTime,
    placeholder: false
  }))
  while (rows.length < 4) {
    rows.push({ placeholder: true })
  }
  return rows
})

const deviceOverview = computed(() => {
  const total = Number(summary.value.deviceCount || 0)
  const offlineCount = highRiskQueue.value.filter((item) => String(item.warningTypeName || '').includes('离线')).length
  const onlineCount = Math.max(total - offlineCount, 0)
  const onlineRate = total ? Math.round((onlineCount * 100) / total) : 0
  const offlineRate = total ? Math.round((offlineCount * 100) / total) : 0
  const latestLog = Array.isArray(inspectionSummary.value.recentLogs) ? inspectionSummary.value.recentLogs[0] : null
  const coverageRate =
    total && latestLog?.inspectDeviceCount ? Math.min(100, Math.round((latestLog.inspectDeviceCount * 100) / total)) : 0
  const stabilityRate = Math.max(0, 100 - Math.min(100, Number(summary.value.pendingWarningCount || 0) * 10))
  return {
    total,
    offlineCount,
    onlineCount,
    bars: [
      { name: '在线率', value: onlineRate },
      { name: '离线占比', value: offlineRate },
      { name: '巡检覆盖率', value: coverageRate },
      { name: '运行稳定率', value: stabilityRate }
    ]
  }
})

watch(selectedMetricKey, () => {
  renderTrendChart()
})

watch(
  homeData,
  async () => {
    await nextTick()
    renderAllCharts()
  },
  { deep: true }
)

onMounted(async () => {
  await fetchDashboardData()
  await nextTick()
  renderAllCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstances.forEach((instance) => instance.dispose())
  chartInstances.clear()
})

async function fetchDashboardData() {
  loading.value = true
  try {
    const data = await getDashboardHomeApi()
    homeData.value = {
      ...defaultHomeData(),
      ...data
    }
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  await fetchDashboardData()
}

function handleResize() {
  chartInstances.forEach((instance) => instance.resize())
}

function getOrCreateChart(key, dom) {
  if (!dom) {
    return null
  }
  const exist = chartInstances.get(key)
  if (exist) {
    return exist
  }
  const instance = echarts.init(dom)
  chartInstances.set(key, instance)
  return instance
}

function renderAllCharts() {
  renderTrendChart()
  renderWarningStatusChart()
  renderClosureTrendChart()
  renderDeviceStatusChart()
}

function renderTrendChart() {
  const instance = getOrCreateChart('trend', trendChartRef.value)
  if (!instance) {
    return
  }

  const xAxis =
    (Array.isArray(homeData.value.environmentTrend?.xAxis) && homeData.value.environmentTrend.xAxis.length
      ? homeData.value.environmentTrend.xAxis
      : ['D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'D7']) || []

  const data = selectedSeriesData.value.length ? selectedSeriesData.value : [0, 0, 0, 0, 0, 0, 0]
  const color = selectedMetric.value.color

  instance.setOption(
    {
      animationDuration: 400,
      color: [color],
      tooltip: { trigger: 'axis' },
      grid: { left: 44, right: 18, top: 22, bottom: 34 },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: xAxis,
        axisLine: { lineStyle: { color: 'rgba(23,49,42,0.18)' } },
        axisLabel: { color: '#6f817c' }
      },
      yAxis: {
        type: 'value',
        scale: true,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#6f817c' },
        splitLine: { lineStyle: { color: 'rgba(23,49,42,0.08)' } }
      },
      series: [
        {
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: false,
          lineStyle: { width: 3, color },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: `${color}33` },
              { offset: 1, color: `${color}08` }
            ])
          },
          data
        }
      ]
    },
    true
  )
}

function renderWarningStatusChart() {
  const instance = getOrCreateChart('warningStatus', warningStatusChartRef.value)
  if (!instance) {
    return
  }

  const source = warningStatusList.value.length
    ? warningStatusList.value
    : [
        { name: '待确认', value: 0 },
        { name: '处理中', value: 0 },
        { name: '已处理', value: 0 },
        { name: '已闭环', value: 0 }
      ]

  instance.setOption(
    {
      animationDuration: 400,
      tooltip: { trigger: 'item' },
      legend: {
        bottom: 0,
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: '#5f726d', fontSize: 12 }
      },
      series: [
        {
          type: 'pie',
          radius: ['52%', '72%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          label: {
            color: '#415650',
            fontSize: 12,
            formatter: '{b}\n{d}%'
          },
          data: source
        }
      ]
    },
    true
  )
}

function renderClosureTrendChart() {
  const instance = getOrCreateChart('closureTrend', closureTrendChartRef.value)
  if (!instance) {
    return
  }

  const xAxis =
    (Array.isArray(homeData.value.closureTrend?.xAxis) && homeData.value.closureTrend.xAxis.length
      ? homeData.value.closureTrend.xAxis
      : ['D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'D7']) || []

  const series = Array.isArray(homeData.value.closureTrend?.series) ? homeData.value.closureTrend.series : []
  const triggerData = Array.isArray(series[0]?.data) ? series[0].data.map((item) => Number(item || 0)) : []
  const closedData = Array.isArray(series[1]?.data) ? series[1].data.map((item) => Number(item || 0)) : []
  const rateData = Array.isArray(series[2]?.data) ? series[2].data.map((item) => Number(item || 0)) : []

  instance.setOption(
    {
      animationDuration: 400,
      color: ['#4c8bf5', '#1f8f5f', '#f2b544'],
      tooltip: { trigger: 'axis' },
      legend: {
        top: 0,
        right: 4,
        textStyle: { color: '#5f726d', fontSize: 12 }
      },
      grid: { left: 40, right: 40, top: 32, bottom: 30 },
      xAxis: {
        type: 'category',
        data: xAxis,
        axisLine: { lineStyle: { color: 'rgba(23,49,42,0.18)' } },
        axisLabel: { color: '#6f817c' }
      },
      yAxis: [
        {
          type: 'value',
          name: '数量',
          axisLabel: { color: '#6f817c' },
          splitLine: { lineStyle: { color: 'rgba(23,49,42,0.08)' } }
        },
        {
          type: 'value',
          name: '闭环率%',
          min: 0,
          max: 100,
          axisLabel: { color: '#6f817c' },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: series[0]?.name || '新增预警',
          type: 'line',
          smooth: true,
          yAxisIndex: 0,
          data: triggerData
        },
        {
          name: series[1]?.name || '闭环数量',
          type: 'line',
          smooth: true,
          yAxisIndex: 0,
          data: closedData
        },
        {
          name: series[2]?.name || '闭环率',
          type: 'line',
          smooth: true,
          yAxisIndex: 1,
          lineStyle: { type: 'dashed' },
          data: rateData
        }
      ]
    },
    true
  )
}

function renderDeviceStatusChart() {
  const instance = getOrCreateChart('deviceStatus', deviceStatusChartRef.value)
  if (!instance) {
    return
  }

  const categories = deviceOverview.value.bars.map((item) => item.name)
  const values = deviceOverview.value.bars.map((item) => item.value)

  instance.setOption(
    {
      animationDuration: 400,
      color: ['#1f8f5f'],
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter(params) {
          const row = params?.[0]
          if (!row) {
            return ''
          }
          return `${row.name}：${row.value}%`
        }
      },
      grid: { left: 78, right: 24, top: 18, bottom: 18 },
      xAxis: {
        type: 'value',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}%', color: '#6f817c' },
        splitLine: { lineStyle: { color: 'rgba(23,49,42,0.08)' } }
      },
      yAxis: {
        type: 'category',
        data: categories,
        axisTick: { show: false },
        axisLine: { show: false },
        axisLabel: { color: '#556b65' }
      },
      series: [
        {
          type: 'bar',
          data: values,
          barWidth: 14,
          itemStyle: {
            borderRadius: [0, 9, 9, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#27986a' },
              { offset: 1, color: '#61be90' }
            ])
          },
          label: {
            show: true,
            position: 'right',
            formatter: '{c}%'
          }
        }
      ]
    },
    true
  )
}

function formatMetric(value, unit = '') {
  const num = Number(value || 0)
  return `${num.toFixed(1)}${unit}`
}

function formatPercent(value) {
  const num = Number(value || 0)
  return `${num.toFixed(1)}%`
}

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  const hh = `${date.getHours()}`.padStart(2, '0')
  const mm = `${date.getMinutes()}`.padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

function getLevelTag(levelName) {
  if (String(levelName).includes('高')) {
    return 'danger'
  }
  if (String(levelName).includes('中')) {
    return 'warning'
  }
  return 'success'
}
</script>

<style scoped lang="scss">
.dashboard-page {
  gap: 16px;
  width: 100%;
}

.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: var(--app-banner-min-height);
  padding: var(--app-banner-padding-y) var(--app-banner-padding-x);
}

.hero-main h1 {
  margin: 0;
  font-size: 20px;
  line-height: 1.2;
  color: var(--app-text);
}

.hero-main p {
  margin: 0;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-pill,
.hero-user {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  background: #f6fbf8;
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-secondary);
}

.hero-pill.success {
  border-color: rgba(23, 138, 98, 0.16);
  background: rgba(23, 138, 98, 0.08);
  color: var(--app-primary-dark);
}

.hero-user {
  background: #fff;
  color: var(--app-text);
}

.core-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  align-items: stretch;
}

.core-card {
  display: flex;
  flex-direction: column;
  flex: 1 0 auto;
  padding: 14px 16px 16px;
}

.card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  min-height: 56px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(17, 82, 62, 0.08);
}

.card-head h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.2;
  color: var(--app-text);
}

.card-head p {
  margin: 3px 0 0;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.metric-tabs {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-radius: 12px;
  background: #f3f7f4;
  border: 1px solid rgba(17, 82, 62, 0.08);
}

.metric-tabs button {
  border: none;
  height: 30px;
  padding: 0 11px;
  border-radius: 9px;
  background: transparent;
  color: var(--app-text-secondary);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}

.metric-tabs button.active {
  background: #fff;
  color: var(--app-primary-dark);
  box-shadow: 0 2px 8px rgba(17, 82, 62, 0.08);
}

.trend-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.trend-metric-box {
  display: grid;
  gap: 6px;
  flex: 1 0 auto;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  background: #fbfdfc;
}

.trend-metric-box.warning {
  border-color: rgba(242, 181, 68, 0.52);
  background: rgba(255, 248, 233, 0.8);
}

.trend-metric-box span {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.trend-metric-box strong {
  font-size: 26px;
  line-height: 1.15;
  color: var(--app-text);
}

.trend-chart {
  flex: 1;
  min-height: 300px;
  margin-top: 10px;
}

.trend-summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.summary-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  background: #fbfdfc;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.insight-main .card-head h3 {
  font-size: 18px;
}

.insight-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.insight-section {
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 14px;
  background: #fbfdfc;
  padding: 12px 14px;
}

.score-section {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 12px;
}

.score-left span {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.score-left strong {
  display: block;
  margin-top: 6px;
  font-size: 40px;
  line-height: 1;
  color: var(--app-text);
}

.score-right {
  display: grid;
  align-content: center;
  gap: 8px;
}

.score-progress-track {
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: #e7efec;
}

.score-progress-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1f8f5f 0%, #f2b544 62%, #e75a4d 100%);
}

.score-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.insight-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--app-text);
}

.bullet-list,
.suggest-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.bullet-list {
  display: grid;
  gap: 8px;
}

.bullet-list li {
  position: relative;
  padding-left: 14px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-text-secondary);
}

.bullet-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--app-primary);
}

.suggest-list {
  display: grid;
  gap: 8px;
}

.suggest-list li {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
}

.suggest-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 8px;
  background: var(--app-primary-soft);
  color: var(--app-primary-dark);
  font-size: 12px;
  font-weight: 700;
}

.suggest-text {
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-text-secondary);
}

.inspection-section {
  margin-top: auto;
}

.inspection-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.inspection-item {
  flex: 1 0 auto;
  padding: 10px 12px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 12px;
  background: #fff;
}

.inspection-item span {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.inspection-item strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
  line-height: 1.2;
  color: var(--app-text);
}

.bottom-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  align-items: stretch;
}

.bottom-card {
  display: flex;
  flex-direction: column;
  flex: 1 0 auto;
  padding: 14px 16px 16px;
}

.sub-chart {
  flex: 1;
  min-height: 200px;
  margin-top: 10px;
}

.status-chip-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.status-chip-item {
  flex: 1 0 auto;
  padding: 10px 12px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 12px;
  background: #fbfdfc;
}

.status-chip-item span {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.status-chip-item strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  line-height: 1.15;
  color: var(--app-text);
}

.warning-list-card {
  display: flex;
  flex-direction: column;
}

.warning-list {
  display: grid;
  flex: 1;
  gap: 10px;
  margin-top: 10px;
}

.warning-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex: 1 0 auto;
  padding: 10px 12px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 12px;
  background: #fbfdfc;
}

.warning-row.placeholder {
  background: linear-gradient(180deg, rgba(248, 251, 250, 0.95), rgba(248, 251, 250, 0.8));
}

.warning-row-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.warning-row-main strong {
  font-size: 14px;
  color: var(--app-text);
}

.warning-row-main span {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.warning-row-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  font-size: 12px;
  color: var(--app-text-secondary);
}

@container app-content (max-width: 900px) {
  .core-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .core-card,
  .bottom-card {
    min-height: auto;
  }

  .status-chip-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@container app-content (max-width: 760px) {
  .dashboard-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .trend-metrics,
  .trend-summary-row,
  .score-meta,
  .inspection-grid,
  .status-chip-grid {
    grid-template-columns: 1fr;
  }

  .score-section {
    grid-template-columns: 1fr;
  }

  .warning-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .warning-row-meta {
    justify-content: flex-start;
  }
}
</style>
