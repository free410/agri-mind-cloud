<template>
  <div class="page-shell">
    <section class="page-banner">
      <div class="page-banner__main">
        <span class="page-banner__badge">环境监测分析</span>
        <h1 class="page-banner__title">环境监测</h1>
        <p class="page-banner__desc">
          围绕温度、湿度、土壤湿度、光照与 pH 值构建监测分析页，适合展示农业环境数据的实时感知与趋势分析能力。
        </p>
      </div>

      <div class="page-banner__actions">
        <div class="page-banner__meta">
          <span class="meta-label">数据状态</span>
          <strong>{{ latestCollectTime || '等待数据' }}</strong>
        </div>
        <div class="page-banner__meta">
          <span class="meta-label">当前农田</span>
          <strong>{{ currentFarmlandName }}</strong>
        </div>
      </div>
    </section>

    <section class="page-overview-grid five">
      <article v-for="item in realtimeCards" :key="item.label" class="page-overview-card">
        <span class="page-overview-card__label">{{ item.label }}</span>
        <strong class="page-overview-card__value">{{ item.value }}</strong>
        <p class="page-overview-card__desc">{{ item.desc }}</p>
      </article>
    </section>

    <el-card class="page-filter-card" shadow="never">
      <template #header>
        <div class="page-panel-title">
          <strong>筛选条件</strong>
          <span class="page-panel-tip">支持按农田、设备和时间范围筛选监测数据与趋势结果。</span>
        </div>
      </template>

      <el-form :model="queryForm" inline class="page-filter-form">
        <el-form-item label="农田">
          <el-select v-model="queryForm.farmlandId" placeholder="请选择农田" clearable style="width: 220px">
            <el-option
              v-for="item in farmlandOptions"
              :key="item.id"
              :label="item.fieldName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设备">
          <el-select v-model="queryForm.deviceId" placeholder="请选择设备" clearable style="width: 240px">
            <el-option
              v-for="item in deviceOptions"
              :key="item.id"
              :label="item.deviceName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            range-separator="至"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <div class="page-toolbar">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <section class="monitor-main-grid">
      <el-card class="page-content-card trend-panel" shadow="never">
        <template #header>
          <div class="page-panel-head">
            <div class="page-panel-title">
              <strong>环境趋势图</strong>
              <span class="page-panel-tip">展示当前筛选范围内的温度、湿度、土壤湿度、光照与 pH 趋势。</span>
            </div>
            <div class="trend-legend-tip">趋势点数 {{ trendPointCount }}</div>
          </div>
        </template>

        <div class="trend-summary-strip">
          <div class="summary-mini-card">
            <span>主监测设备</span>
            <strong>{{ currentDeviceName }}</strong>
          </div>
          <div class="summary-mini-card">
            <span>监测时段</span>
            <strong>{{ timeRangeText }}</strong>
          </div>
          <div class="summary-mini-card warning">
            <span>风险项</span>
            <strong>{{ riskSummaryCount }} 个</strong>
          </div>
        </div>

        <div ref="trendChartRef" v-loading="trendLoading" class="trend-chart"></div>
      </el-card>

      <div class="insight-column">
        <el-card class="page-content-card insight-panel" shadow="never">
          <template #header>
            <div class="page-panel-title">
              <strong>当前监测摘要</strong>
              <span class="page-panel-tip">将当前实时值压缩成结构化摘要，避免大段说明文字。</span>
            </div>
          </template>

          <div class="insight-list">
            <div v-for="item in monitorSummaryList" :key="item.label" class="insight-item">
              <span class="insight-item__label">{{ item.label }}</span>
              <strong class="insight-item__value">{{ item.value }}</strong>
              <p class="insight-item__desc">{{ item.desc }}</p>
            </div>
          </div>
        </el-card>

        <el-card class="page-content-card insight-panel" shadow="never">
          <template #header>
            <div class="page-panel-title">
              <strong>AI 监测解读</strong>
              <span class="page-panel-tip">基于当前数据做轻量智能解读，后续可直接联动 AI 助手页。</span>
            </div>
          </template>

          <div class="ai-note-list">
            <div v-for="(item, index) in aiInsightList" :key="item.title" class="ai-note-item">
              <span class="ai-note-item__index">{{ index + 1 }}</span>
              <div class="ai-note-item__body">
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </section>

    <el-card class="page-content-card" shadow="never">
      <template #header>
        <div class="page-panel-title">
          <strong>环境数据列表</strong>
          <span class="page-panel-tip">共 {{ pagination.total }} 条监测记录，支持直接查看最新环境采集数据。</span>
        </div>
      </template>

      <div class="page-table-shell">
        <el-table v-loading="loading" :data="tableData" class="environment-table">
          <el-table-column prop="collectTime" label="采集时间" width="176" />
          <el-table-column prop="farmlandName" label="农田" min-width="150" />
          <el-table-column prop="deviceName" label="设备" min-width="160" />
          <el-table-column label="温度(℃)" width="108" align="center">
            <template #default="{ row }">{{ formatValue(row.temperature) }}</template>
          </el-table-column>
          <el-table-column label="湿度(%)" width="108" align="center">
            <template #default="{ row }">{{ formatValue(row.humidity) }}</template>
          </el-table-column>
          <el-table-column label="土壤湿度(%)" width="120" align="center">
            <template #default="{ row }">{{ formatValue(row.soilMoisture) }}</template>
          </el-table-column>
          <el-table-column label="光照(lux)" width="122" align="center">
            <template #default="{ row }">{{ formatValue(row.lightIntensity, 0) }}</template>
          </el-table-column>
          <el-table-column label="pH 值" width="96" align="center">
            <template #default="{ row }">{{ formatValue(row.phValue) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchTableData"
          @current-change="fetchTableData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getFieldPageApi } from '@/api/field'
import { getDevicePageApi } from '@/api/device'
import { getEnvironmentDataPageApi, getEnvironmentTrendApi } from '@/api/environment'

const loading = ref(false)
const trendLoading = ref(false)
const tableData = ref([])
const overviewData = ref([])
const farmlandOptions = ref([])
const deviceOptions = ref([])
const trendChartRef = ref()

const trendData = reactive({
  xAxis: [],
  series: []
})

let trendChartInstance = null

const queryForm = reactive({
  farmlandId: undefined,
  deviceId: undefined,
  timeRange: []
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

onMounted(async () => {
  await Promise.all([fetchFarmlandOptions(), fetchDeviceOptions()])
  if (!queryForm.farmlandId && farmlandOptions.value.length) {
    queryForm.farmlandId = farmlandOptions.value[0].id
  }
  await Promise.all([fetchTableData(), fetchOverviewData(), fetchTrendData()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChartInstance?.dispose()
})

const latestRow = computed(() => tableData.value[0] || overviewData.value[0] || {})

const latestCollectTime = computed(() => latestRow.value.collectTime || '')

const currentFarmlandName = computed(() => {
  const current = farmlandOptions.value.find((item) => item.id === queryForm.farmlandId)
  return current?.fieldName || '全部农田'
})

const currentDeviceName = computed(() => {
  const current = deviceOptions.value.find((item) => item.id === queryForm.deviceId)
  return current?.deviceName || '默认设备集合'
})

const trendPointCount = computed(() => trendData.xAxis.length || 0)

const timeRangeText = computed(() => {
  const [startTime, endTime] = queryForm.timeRange || []
  if (startTime && endTime) {
    return `${startTime.slice(5, 16)} - ${endTime.slice(5, 16)}`
  }
  return '最近监测区间'
})

const realtimeCards = computed(() => [
  buildRealtimeCard('当前温度', latestRow.value.temperature, '℃', getTemperatureStatus(latestRow.value.temperature)),
  buildRealtimeCard('当前湿度', latestRow.value.humidity, '%', getHumidityStatus(latestRow.value.humidity)),
  buildRealtimeCard('当前土壤湿度', latestRow.value.soilMoisture, '%', getSoilStatus(latestRow.value.soilMoisture)),
  buildRealtimeCard('当前光照', latestRow.value.lightIntensity, 'lux', getLightStatus(latestRow.value.lightIntensity)),
  buildRealtimeCard('当前 pH 值', latestRow.value.phValue, '', getPhStatus(latestRow.value.phValue))
])

const riskSummaryCount = computed(
  () => realtimeCards.value.filter((item) => item.level === 'warning' || item.level === 'danger').length
)

const monitorSummaryList = computed(() => [
  {
    label: '监测农田',
    value: currentFarmlandName.value,
    desc: '当前趋势图和列表数据均以所选农田为主。'
  },
  {
    label: '设备范围',
    value: queryForm.deviceId ? '单设备' : '多设备',
    desc: '支持聚焦单台设备，也支持查看当前农田内的综合监测。'
  },
  {
    label: '最新采集时间',
    value: latestCollectTime.value || '--',
    desc: '当前卡片和摘要优先使用最新一条环境数据。'
  }
])

const aiInsightList = computed(() => {
  const list = []
  const soil = Number(latestRow.value.soilMoisture || 0)
  const temp = Number(latestRow.value.temperature || 0)
  const humidity = Number(latestRow.value.humidity || 0)
  const ph = Number(latestRow.value.phValue || 0)

  if (soil > 0 && soil < 32) {
    list.push({
      title: '关注土壤湿度偏低',
      desc: '当前土壤湿度接近偏低区间，建议结合灌溉设备状态持续观察。'
    })
  }

  if (temp >= 26) {
    list.push({
      title: '温度存在上行趋势',
      desc: '温度偏高时可联动通风或喷淋策略，降低高温胁迫风险。'
    })
  }

  if (humidity > 0 && (humidity < 60 || humidity > 85)) {
    list.push({
      title: '湿度处于波动区间',
      desc: '建议对温室通风与灌溉节奏做联动复核，避免重复预警。'
    })
  }

  if (ph > 0 && (ph < 5.8 || ph > 7.2)) {
    list.push({
      title: 'pH 偏离常见适宜范围',
      desc: '建议结合作物品种与施肥记录进行阶段性土壤调节。'
    })
  }

  const fallback = [
    {
      title: '建议联动 AI 助手分析',
      desc: '可将当前监测结果带入 AI 助手页，形成“监测-分析-建议”的闭环展示。'
    },
    {
      title: '关注异常点变化节奏',
      desc: '建议重点对比最近 24 小时异常点，确认是否存在持续性风险。'
    },
    {
      title: '监测状态总体可控',
      desc: '当前数据可用于比赛演示系统的实时感知与分析能力。'
    }
  ]

  for (const item of fallback) {
    if (list.length >= 3) break
    if (!list.some((v) => v.title === item.title)) {
      list.push(item)
    }
  }

  return list.slice(0, 3)
})

async function fetchFarmlandOptions() {
  const res = await getFieldPageApi({ current: 1, size: 100 })
  farmlandOptions.value = res.records || []
}

async function fetchDeviceOptions() {
  const res = await getDevicePageApi({ current: 1, size: 100 })
  deviceOptions.value = res.records || []
}

async function fetchTableData() {
  loading.value = true
  try {
    const [startTime, endTime] = queryForm.timeRange || []
    const res = await getEnvironmentDataPageApi({
      current: pagination.current,
      size: pagination.size,
      farmlandId: queryForm.farmlandId,
      deviceId: queryForm.deviceId,
      startTime,
      endTime
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } finally {
    loading.value = false
  }
}

async function fetchOverviewData() {
  const [startTime, endTime] = queryForm.timeRange || []
  const res = await getEnvironmentDataPageApi({
    current: 1,
    size: 100,
    farmlandId: queryForm.farmlandId,
    deviceId: queryForm.deviceId,
    startTime,
    endTime
  })
  overviewData.value = res.records || []
}

async function fetchTrendData() {
  if (!queryForm.farmlandId) {
    trendData.xAxis = []
    trendData.series = []
    return
  }

  trendLoading.value = true
  try {
    const [startTime, endTime] = queryForm.timeRange || []
    const res = await getEnvironmentTrendApi({
      farmlandId: queryForm.farmlandId,
      deviceId: queryForm.deviceId,
      startTime,
      endTime,
      limit: 12
    })
    trendData.xAxis = res?.xAxis || []
    trendData.series = res?.series || []
    await nextTick()
    initTrendChart()
  } catch (error) {
    ElMessage.error('环境趋势加载失败')
  } finally {
    trendLoading.value = false
  }
}

function initTrendChart() {
  if (!trendChartRef.value) return
  const isLightSeries = (name = '') => /光照|lux/i.test(name)
  const hasLightSeries = (trendData.series || []).some((item) => isLightSeries(item.name))

  trendChartInstance?.dispose()
  trendChartInstance = echarts.init(trendChartRef.value)
  trendChartInstance.setOption({
    color: ['#1F8F5F', '#4C8BF5', '#F4B740', '#F07C59', '#7B8CFF'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#E5ECE9',
      borderWidth: 1,
      textStyle: { color: '#1F2D2A' },
      extraCssText: 'box-shadow: 0 10px 24px rgba(31,45,42,0.08); border-radius: 12px;'
    },
    legend: {
      top: 0,
      left: 0,
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#6B7A77', fontSize: 12 }
    },
    grid: {
      left: 12,
      right: 28,
      top: 52,
      bottom: 16,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.xAxis,
      axisLine: { lineStyle: { color: '#DCE5E1' } },
      axisTick: { show: false },
      axisLabel: { color: '#6B7A77', fontSize: 12 }
    },
    yAxis: [
      {
        type: 'value',
        position: 'left',
        scale: true,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: {
          lineStyle: { color: '#EEF2F0' }
        },
        axisLabel: { color: '#6B7A77', fontSize: 12 }
      },
      {
        type: 'value',
        position: 'right',
        show: hasLightSeries,
        scale: true,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: {
          color: '#8A9895',
          fontSize: 12,
          formatter: (value) => (value >= 1000 ? `${(value / 1000).toFixed(0)}k` : `${Math.round(value)}`)
        }
      }
    ],
    series: (trendData.series || []).map((item) => ({
      ...item,
      yAxisIndex: isLightSeries(item.name) ? 1 : 0,
      smooth: true,
      showSymbol: false,
      lineStyle: { width: isLightSeries(item.name) ? 2.6 : 3 },
      areaStyle: isLightSeries(item.name) ? undefined : { opacity: 0.08 }
    }))
  })
  trendChartInstance.resize()
}

async function handleSearch() {
  pagination.current = 1
  await Promise.all([fetchTableData(), fetchOverviewData(), fetchTrendData()])
}

async function handleReset() {
  queryForm.farmlandId = farmlandOptions.value[0]?.id
  queryForm.deviceId = undefined
  queryForm.timeRange = []
  pagination.current = 1
  await Promise.all([fetchTableData(), fetchOverviewData(), fetchTrendData()])
}

function handleResize() {
  trendChartInstance?.resize()
}

function formatValue(value, precision = 1) {
  if (value === null || value === undefined || value === '') return '--'
  return Number(value).toFixed(precision)
}

function buildRealtimeCard(label, rawValue, unit, statusInfo) {
  return {
    label,
    value: rawValue === null || rawValue === undefined || rawValue === '' ? '--' : `${formatValue(rawValue, unit === 'lux' ? 0 : 1)} ${unit}`.trim(),
    desc: statusInfo.text,
    level: statusInfo.level
  }
}

function getTemperatureStatus(value) {
  const num = Number(value || 0)
  if (!value && value !== 0) return { text: '等待温度数据', level: 'info' }
  if (num >= 26) return { text: '偏高，建议关注通风', level: 'warning' }
  if (num <= 12) return { text: '偏低，建议关注保温', level: 'warning' }
  return { text: '处于适宜范围', level: 'success' }
}

function getHumidityStatus(value) {
  const num = Number(value || 0)
  if (!value && value !== 0) return { text: '等待湿度数据', level: 'info' }
  if (num < 60) return { text: '偏低，建议持续观察', level: 'warning' }
  if (num > 85) return { text: '偏高，注意病害风险', level: 'warning' }
  return { text: '整体较稳定', level: 'success' }
}

function getSoilStatus(value) {
  const num = Number(value || 0)
  if (!value && value !== 0) return { text: '等待土壤数据', level: 'info' }
  if (num < 32) return { text: '偏低，可考虑补水', level: 'danger' }
  return { text: '土壤水分正常', level: 'success' }
}

function getLightStatus(value) {
  const num = Number(value || 0)
  if (!value && value !== 0) return { text: '等待光照数据', level: 'info' }
  if (num > 45000) return { text: '光照较强，午后需关注', level: 'warning' }
  if (num < 8000) return { text: '光照偏弱，适合继续观察', level: 'warning' }
  return { text: '光照水平正常', level: 'success' }
}

function getPhStatus(value) {
  const num = Number(value || 0)
  if (!value && value !== 0) return { text: '等待 pH 数据', level: 'info' }
  if (num < 5.8 || num > 7.2) return { text: '偏离常见适宜范围', level: 'warning' }
  return { text: '酸碱度平稳', level: 'success' }
}
</script>

<style scoped lang="scss">
.monitor-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(320px, 0.95fr);
  gap: 16px;
  align-items: stretch;
}

.meta-label {
  font-size: 12px;
  color: rgba(238, 251, 245, 0.78);
}

.page-banner__meta {
  strong {
    font-size: 14px;
    color: #fff;
  }
}

.trend-panel,
.insight-panel {
  height: 100%;
}

.trend-panel :deep(.el-card__body),
.insight-panel :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.trend-panel :deep(.el-card__body) {
  justify-content: flex-start;
}

.trend-summary-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.summary-mini-card {
  padding: 14px 16px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 16px;
  background: #fbfcfc;

  span {
    display: block;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    display: block;
    margin-top: 8px;
    font-size: 18px;
    color: var(--app-text);
  }
}

.summary-mini-card.warning {
  background: #fff8ef;
  border-color: #fae0b3;
}

.trend-legend-tip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--app-panel-soft);
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-secondary);
}

.trend-chart {
  width: 100%;
  height: auto;
  min-height: 320px;
  flex: 1;
  margin-top: 4px;
}

.insight-column {
  display: grid;
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 16px;
  height: 100%;
}

.insight-list,
.ai-note-list {
  display: grid;
  gap: 12px;
  height: 100%;
}

.ai-note-list {
  grid-template-rows: repeat(3, minmax(0, 1fr));
}

.insight-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 16px;
  background: #fff;
}

.insight-item__label {
  display: block;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.insight-item__value {
  display: block;
  margin-top: 8px;
  font-size: 18px;
  color: var(--app-text);
}

.insight-item__desc {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--app-text-tertiary);
}

.ai-note-item {
  display: grid;
  grid-template-columns: 36px 1fr;
  align-items: flex-start;
  gap: 12px;
  height: 100%;
  padding: 12px 14px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 16px;
  background: #fff;
}

.ai-note-item__index {
  display: flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: var(--app-tech-soft);
  color: var(--app-tech);
  font-weight: 800;
}

.ai-note-item__body {
  strong {
    display: block;
    margin-bottom: 6px;
    font-size: 14px;
    color: var(--app-text);
  }

  p {
    margin: 0;
    font-size: 12px;
    line-height: 1.7;
    color: var(--app-text-secondary);
  }
}

.environment-table :deep(.el-table__row:hover td) {
  background: rgba(23, 138, 98, 0.04) !important;
}

@media (max-width: 1400px) {
  .monitor-main-grid {
    grid-template-columns: 1fr;
  }

  .insight-column {
    grid-template-rows: unset;
    height: auto;
  }

  .trend-chart {
    min-height: 300px;
    flex: 1;
  }
}

@media (max-width: 900px) {
  .trend-summary-strip {
    grid-template-columns: 1fr;
  }
}
</style>
