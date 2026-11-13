<template>
  <div class="page-shell warning-center-page">
    <section class="page-banner warning-center-banner">
      <div class="page-banner__main">
        <span class="page-banner__badge">智能预警闭环中心</span>
        <p class="page-banner__desc">覆盖预警发现、AI 研判、处置建议、状态流转与闭环结果展示。</p>
      </div>
      <div class="page-banner__actions">
        <el-tag type="success" effect="plain">AI 联动已开启</el-tag>
      </div>
    </section>

    <section class="page-overview-grid warning-overview-grid">
      <article class="page-overview-card">
        <span class="page-overview-card__label">预警总数</span>
        <strong class="page-overview-card__value">{{ overviewStats.total }}</strong>
        <p class="page-overview-card__desc">系统累计识别的预警事件。</p>
      </article>
      <article class="page-overview-card">
        <span class="page-overview-card__label">高风险预警</span>
        <strong class="page-overview-card__value">{{ overviewStats.highRisk }}</strong>
        <p class="page-overview-card__desc">高等级事件建议优先处置。</p>
      </article>
      <article class="page-overview-card">
        <span class="page-overview-card__label">待处理预警</span>
        <strong class="page-overview-card__value">{{ overviewStats.pending }}</strong>
        <p class="page-overview-card__desc">待确认与处理中事件合计。</p>
      </article>
      <article class="page-overview-card">
        <span class="page-overview-card__label">已闭环预警</span>
        <strong class="page-overview-card__value">{{ overviewStats.closed }}</strong>
        <p class="page-overview-card__desc">含正常闭环与误报闭环。</p>
      </article>
      <article class="page-overview-card">
        <span class="page-overview-card__label">闭环率</span>
        <strong class="page-overview-card__value">{{ closureRate }}</strong>
        <p class="page-overview-card__desc">闭环能力可视化指标。</p>
      </article>
    </section>

    <section class="warning-layout">
      <div class="warning-layout__left">
        <el-card class="page-filter-card" shadow="never">
          <template #header>
            <div class="page-panel-title">
              <strong>预警筛选</strong>
              <span class="page-panel-tip">按等级、状态、农田定位重点事件。</span>
            </div>
          </template>
          <el-form :model="queryForm" inline class="page-filter-form warning-filter-form">
            <el-form-item label="等级">
              <el-select v-model="queryForm.level" placeholder="全部等级" clearable style="width: 150px">
                <el-option
                  v-for="item in warningLevelOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 150px">
                <el-option
                  v-for="item in warningStatusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="农田">
              <el-select v-model="queryForm.farmlandId" placeholder="全部农田" clearable style="width: 220px">
                <el-option
                  v-for="item in farmlandOptions"
                  :key="item.id"
                  :label="item.fieldName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <div class="page-toolbar">
                <el-button type="primary" @click="handleSearch">查询</el-button>
                <el-button @click="handleReset">重置</el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="page-content-card warning-list-card" shadow="never">
          <template #header>
            <div class="page-panel-title">
              <strong>预警列表</strong>
              <span class="page-panel-tip">单击任意预警，右侧工作台自动加载闭环详情。</span>
            </div>
          </template>

          <div class="warning-list-summary">
            <div class="warning-list-summary__item">
              <span>总预警数</span>
              <strong>{{ overviewStats.total }}</strong>
            </div>
            <div class="warning-list-summary__item">
              <span>待处理</span>
              <strong>{{ overviewStats.pending }}</strong>
            </div>
            <div class="warning-list-summary__item">
              <span>高风险</span>
              <strong>{{ overviewStats.highRisk }}</strong>
            </div>
          </div>

          <div class="page-table-shell">
            <el-table
              v-loading="listLoading"
              :data="tableData"
              row-key="id"
              highlight-current-row
              :current-row-key="selectedWarningId"
              class="warning-center-table"
              @row-click="handleSelectWarning"
            >
              <el-table-column prop="warningNo" label="编号" width="160" />
              <el-table-column prop="warningTypeName" label="类型" width="110" />
              <el-table-column label="等级" width="82" align="center">
                <template #default="{ row }">
                  <el-tag :type="getOptionTagType(warningLevelOptions, row.level)" effect="plain">
                    {{ row.levelName || getOptionLabel(warningLevelOptions, row.level) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="96" align="center">
                <template #default="{ row }">
                  <el-tag :type="getOptionTagType(warningStatusOptions, row.status)" effect="plain">
                    {{ row.statusName || getOptionLabel(warningStatusOptions, row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="farmlandName" label="农田" min-width="140" show-overflow-tooltip />
              <el-table-column prop="relatedDeviceName" label="设备" min-width="130" show-overflow-tooltip />
              <el-table-column prop="triggerTime" label="触发时间" width="168" />
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" @click.stop="handleSelectWarning(row)">工作台</el-button>
                </template>
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
              layout="total, sizes, prev, pager, next"
              @size-change="fetchWarningTable"
              @current-change="fetchWarningTable"
            />
          </div>
        </el-card>

        <el-card class="page-content-card warning-distribution-card" shadow="never">
          <template #header>
            <div class="page-panel-title">
              <strong>预警分布概览</strong>
              <span class="page-panel-tip">用于补齐左侧工作区高度，并展示当前范围预警结构。</span>
            </div>
          </template>
          <div class="warning-distribution-grid">
            <article class="distribution-panel">
              <div class="distribution-panel__head">
                <strong>等级分布</strong>
                <span>{{ levelDistributionTotal }} 条</span>
              </div>
              <div class="distribution-list">
                <div
                  v-for="item in levelDistributionList"
                  :key="item.key"
                  class="distribution-item"
                >
                  <div class="distribution-item__meta">
                    <em>{{ item.label }}</em>
                    <strong>{{ item.value }} 条</strong>
                  </div>
                  <div class="distribution-track">
                    <span class="distribution-track__bar" :class="item.tone" :style="{ width: `${item.percent}%` }"></span>
                  </div>
                  <span class="distribution-item__percent">{{ item.percentText }}</span>
                </div>
              </div>
            </article>

            <article class="distribution-panel">
              <div class="distribution-panel__head">
                <strong>状态分布</strong>
                <span>{{ statusDistributionTotal }} 条</span>
              </div>
              <div class="distribution-list">
                <div
                  v-for="item in statusDistributionList"
                  :key="item.key"
                  class="distribution-item"
                >
                  <div class="distribution-item__meta">
                    <em>{{ item.label }}</em>
                    <strong>{{ item.value }} 条</strong>
                  </div>
                  <div class="distribution-track">
                    <span class="distribution-track__bar" :class="item.tone" :style="{ width: `${item.percent}%` }"></span>
                  </div>
                  <span class="distribution-item__percent">{{ item.percentText }}</span>
                </div>
              </div>
            </article>
          </div>
        </el-card>
      </div>

      <div class="warning-layout__right">
        <el-card class="page-content-card warning-workbench-card" shadow="never">
          <template #header>
            <div class="page-panel-title">
              <strong>预警闭环工作台</strong>
              <span class="page-panel-tip">当前预警：{{ detailData.warningNo || '--' }}</span>
            </div>
          </template>

          <div v-if="!selectedWarningId" class="empty-hint">
            请在左侧列表中选择一条预警，查看 AI 分析、处置建议和状态流转。
          </div>

          <div v-else v-loading="detailLoading" class="warning-workbench">
            <section class="workbench-section">
              <div class="workbench-section__head workbench-section__head--with-action">
                <div class="workbench-section-title">
                  <strong>预警概览</strong>
                  <span>结构化展示当前事件与触发快照。</span>
                </div>
                <el-button
                  type="primary"
                  plain
                  size="small"
                  :loading="reportLoading"
                  @click="handleGenerateReport"
                >
                  生成处置报告
                </el-button>
              </div>
              <div class="overview-info-grid">
                <div class="overview-info-item">
                  <span>预警类型</span>
                  <strong>{{ detailData.warningTypeName || '--' }}</strong>
                </div>
                <div class="overview-info-item">
                  <span>风险等级</span>
                  <strong>{{ detailData.levelName || '--' }}</strong>
                </div>
                <div class="overview-info-item">
                  <span>当前状态</span>
                  <strong>{{ detailData.statusName || '--' }}</strong>
                </div>
                <div class="overview-info-item">
                  <span>所属农田</span>
                  <strong>{{ detailData.farmlandName || '--' }}</strong>
                </div>
                <div class="overview-info-item">
                  <span>关联设备</span>
                  <strong>{{ detailData.relatedDeviceName || '--' }}</strong>
                </div>
                <div class="overview-info-item">
                  <span>触发时间</span>
                  <strong>{{ detailData.triggerTime || '--' }}</strong>
                </div>
              </div>
              <div class="overview-summary-block">
                <span>预警摘要</span>
                <p>{{ detailData.content || '--' }}</p>
              </div>
              <div class="overview-snapshot-grid">
                <div class="overview-snapshot-item">
                  <span>触发来源</span>
                  <strong>{{ detailData.triggerSourceName || '--' }}</strong>
                </div>
                <div class="overview-snapshot-item">
                  <span>处理人</span>
                  <strong>{{ detailData.handlerName || '未分配' }}</strong>
                </div>
                <div class="overview-snapshot-item">
                  <span>处理时间</span>
                  <strong>{{ detailData.handleTime || '--' }}</strong>
                </div>
                <div class="overview-snapshot-item">
                  <span>闭环时间</span>
                  <strong>{{ detailData.closeTime || '--' }}</strong>
                </div>
              </div>
            </section>

            <section class="workbench-section">
              <div class="workbench-section__head">
                <div class="workbench-section-title">
                  <strong>AI 分析与处置建议</strong>
                  <span>并排呈现原因研判与执行建议，便于快速决策。</span>
                </div>
              </div>
              <div class="ai-workbench-grid">
                <article class="ai-pane">
                  <div class="ai-pane__head">
                    <strong>AI 原因分析</strong>
                    <el-button text type="primary" @click="refreshAiAnalysis">刷新分析</el-button>
                  </div>
                  <div v-loading="analysisLoading">
                    <p class="ai-overview">{{ aiAnalysis.warningOverview || '暂无分析结果' }}</p>
                    <div class="ai-block">
                      <span>可能原因</span>
                      <ul>
                        <li v-for="item in aiAnalysis.possibleCauses" :key="item">{{ item }}</li>
                      </ul>
                    </div>
                    <div class="ai-block">
                      <span>风险影响</span>
                      <p>{{ aiAnalysis.riskImpact || '--' }}</p>
                    </div>
                    <div class="ai-block" v-if="aiAnalysis.recommendations?.length">
                      <span>补充建议</span>
                      <ul>
                        <li v-for="item in aiAnalysis.recommendations" :key="item">{{ item }}</li>
                      </ul>
                    </div>
                    <div class="ai-meta-row">
                      <el-tag type="warning" effect="plain">优先级：{{ aiPriorityLabel(aiAnalysis.priorityLevel) }}</el-tag>
                      <el-tag :type="aiAnalysis.manualReviewRequired ? 'danger' : 'success'" effect="plain">
                        {{ aiAnalysis.manualReviewRequired ? '建议人工复核' : '可按流程自动处理' }}
                      </el-tag>
                    </div>
                  </div>
                </article>

                <article class="ai-pane">
                  <div class="ai-pane__head">
                    <strong>AI 处置建议</strong>
                    <el-button text type="primary" @click="refreshAiDisposal">刷新建议</el-button>
                  </div>
                  <div v-loading="disposalLoading">
                    <div class="ai-block">
                      <span>推荐步骤</span>
                      <ol class="ordered-list">
                        <li v-for="step in aiDisposal.steps" :key="step">{{ step }}</li>
                      </ol>
                    </div>
                    <div class="ai-block">
                      <span>检查对象</span>
                      <ul>
                        <li v-for="target in aiDisposal.checkTargets" :key="target">{{ target }}</li>
                      </ul>
                    </div>
                    <div class="ai-meta-row">
                      <el-tag :type="aiDisposal.needDeviceInspection ? 'warning' : 'info'" effect="plain">
                        {{ aiDisposal.needDeviceInspection ? '建议联动设备巡检' : '当前无需额外巡检' }}
                      </el-tag>
                    </div>
                    <p class="ai-summary">{{ aiDisposal.summary || '--' }}</p>
                  </div>
                </article>
              </div>
            </section>

            <section class="workbench-section">
              <div class="workbench-section__head">
                <div class="workbench-section-title">
                  <strong>状态流转操作区</strong>
                  <span>按流程完成确认、处理中、结果提交与闭环动作。</span>
                </div>
              </div>
              <el-form :model="actionForm" label-width="88px" class="action-form">
                <el-form-item label="操作人">
                  <el-input v-model="actionForm.operatorName" disabled />
                  <div class="form-hint">操作人由当前登录账号自动记录，不可更改</div>
                </el-form-item>
                <el-form-item label="处理说明">
                  <el-input
                    v-model="actionForm.note"
                    type="textarea"
                    :rows="3"
                    maxlength="1000"
                    show-word-limit
                    placeholder="请输入本次流转说明，例如：已完成现场核验..."
                  />
                </el-form-item>
              </el-form>

              <div class="flow-actions">
                <el-button
                  v-if="detailData.status === 0"
                  :loading="actionLoading"
                  @click="handleConfirmWarning"
                >
                  确认预警
                </el-button>
                <el-button
                  v-if="detailData.status === 0"
                  type="primary"
                  :loading="actionLoading"
                  @click="handleMarkProcessing"
                >
                  标记处理中
                </el-button>
                <el-button
                  v-if="detailData.status === 1"
                  type="success"
                  :loading="actionLoading"
                  @click="handleSubmitResult"
                >
                  提交处理结果
                </el-button>
                <el-button
                  v-if="detailData.status === 2"
                  type="success"
                  plain
                  :loading="actionLoading"
                  @click="handleCloseLoop"
                >
                  标记已闭环
                </el-button>
                <el-button
                  v-if="allowFalseAlarm"
                  type="warning"
                  plain
                  :loading="actionLoading"
                  @click="handleMarkFalseAlarm"
                >
                  标记误报
                </el-button>
              </div>
            </section>

            <section class="workbench-section">
              <div class="workbench-section__head">
                <div class="workbench-section-title">
                  <strong>处理记录时间线</strong>
                  <span>按时间查看关键操作、状态变化与说明。</span>
                </div>
              </div>
              <el-timeline v-if="flowLogs.length" class="warning-timeline">
                <el-timeline-item
                  v-for="log in flowLogs"
                  :key="log.id"
                  :timestamp="log.processTime"
                  placement="top"
                  :type="timelineTypeByStatus(log.statusTo)"
                >
                  <div class="timeline-card">
                    <div class="timeline-card__head">
                      <strong>{{ log.actionTypeName }}</strong>
                      <el-tag size="small" effect="plain">
                        {{ log.statusFromName }} -> {{ log.statusToName }}
                      </el-tag>
                    </div>
                    <p>{{ log.processNote || '无说明' }}</p>
                    <span>操作人：{{ log.operatorName || '--' }}</span>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无处理记录" :image-size="56" />
            </section>
          </div>
        </el-card>
      </div>
    </section>

    <el-drawer
      v-model="reportDrawerVisible"
      title="预警处置报告"
      size="56%"
      destroy-on-close
      class="warning-report-drawer"
    >
      <div v-loading="reportLoading" class="report-view">
        <template v-if="reportData.warningId">
          <section class="report-section">
            <div class="report-head">
              <div>
                <h3>{{ reportData.warningNo || '--' }}</h3>
                <p>{{ reportData.warningTypeName || '--' }} · {{ reportData.warningLevelName || '--' }}</p>
              </div>
              <div class="report-head-actions">
                <el-button size="small" @click="handleCopyReportSummary">复制摘要</el-button>
                <el-button size="small" type="primary" plain @click="handlePrintReport">打印友好页</el-button>
              </div>
            </div>
            <div class="report-grid">
              <div class="report-item"><span>触发来源</span><strong>{{ reportData.triggerSourceName || '--' }}</strong></div>
              <div class="report-item"><span>所属农田</span><strong>{{ reportData.farmlandName || '--' }}</strong></div>
              <div class="report-item"><span>关联设备</span><strong>{{ reportData.relatedDeviceName || '--' }}</strong></div>
              <div class="report-item"><span>关联作物</span><strong>{{ reportData.relatedCropName || '--' }}</strong></div>
              <div class="report-item"><span>触发时间</span><strong>{{ reportData.triggerTime || '--' }}</strong></div>
              <div class="report-item"><span>当前状态</span><strong>{{ reportData.currentStatusName || '--' }}</strong></div>
              <div class="report-item"><span>闭环结果</span><strong>{{ reportData.closedLoop ? '已闭环' : '未闭环' }}</strong></div>
              <div class="report-item"><span>报告生成</span><strong>{{ reportData.reportGenerateTime || '--' }}</strong></div>
            </div>
          </section>

          <section class="report-section">
            <h4>环境数据快照</h4>
            <div class="report-grid">
              <div class="report-item"><span>温度</span><strong>{{ formatSnapshotValue(reportData.environmentSnapshot?.temperature, '℃') }}</strong></div>
              <div class="report-item"><span>湿度</span><strong>{{ formatSnapshotValue(reportData.environmentSnapshot?.humidity, '%') }}</strong></div>
              <div class="report-item"><span>土壤湿度</span><strong>{{ formatSnapshotValue(reportData.environmentSnapshot?.soilMoisture, '%') }}</strong></div>
              <div class="report-item"><span>光照</span><strong>{{ formatSnapshotValue(reportData.environmentSnapshot?.lightIntensity, 'lux') }}</strong></div>
              <div class="report-item"><span>pH值</span><strong>{{ formatSnapshotValue(reportData.environmentSnapshot?.phValue, '') }}</strong></div>
              <div class="report-item"><span>采集时间</span><strong>{{ reportData.environmentSnapshot?.collectTime || '--' }}</strong></div>
            </div>
          </section>

          <section class="report-section">
            <h4>AI 分析与处置建议</h4>
            <div class="report-plain-block">{{ reportData.aiAnalysisSummary || '--' }}</div>
            <div class="report-plain-block">{{ reportData.aiSuggestionSummary || '--' }}</div>
          </section>

          <section class="report-section">
            <h4>处理记录摘要</h4>
            <div class="report-plain-block">{{ reportData.processRecordSummary || '--' }}</div>
            <el-timeline class="report-timeline">
              <el-timeline-item
                v-for="item in reportData.processLogs"
                :key="item.id"
                :timestamp="item.processTime"
                placement="top"
              >
                <div class="timeline-card">
                  <div class="timeline-card__head">
                    <strong>{{ item.actionTypeName }}</strong>
                    <el-tag size="small" effect="plain">{{ item.statusFromName }} -> {{ item.statusToName }}</el-tag>
                  </div>
                  <p>{{ item.processNote || '无说明' }}</p>
                  <span>操作人：{{ item.operatorName || '--' }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </section>

          <section class="report-section">
            <h4>报告摘要</h4>
            <div class="report-summary-block">{{ reportData.reportSummary || '--' }}</div>
          </section>
        </template>
        <el-empty v-else description="请选择预警并点击生成处置报告" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getFieldPageApi } from '@/api/field'
import {
  closeWarningApi,
  confirmWarningApi,
  getWarningDisposalReportApi,
  getWarningDetailApi,
  getWarningFlowApi,
  getWarningPageApi,
  markWarningFalseAlarmApi,
  markWarningProcessingApi,
  submitWarningHandleResultApi
} from '@/api/warning'
import { getWarningAiAnalysisApi, getWarningDisposalSuggestionApi } from '@/api/ai'
import {
  getOptionLabel,
  getOptionTagType,
  warningLevelOptions,
  warningStatusOptions
} from '@/utils/agriDisplay'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const listLoading = ref(false)
const detailLoading = ref(false)
const analysisLoading = ref(false)
const disposalLoading = ref(false)
const actionLoading = ref(false)
const reportLoading = ref(false)
const reportDrawerVisible = ref(false)

const tableData = ref([])
const farmlandOptions = ref([])
const selectedWarningId = ref(null)
const flowLogs = ref([])

const queryForm = reactive({
  level: undefined,
  status: undefined,
  farmlandId: undefined
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const overviewStats = reactive({
  total: 0,
  highRisk: 0,
  pending: 0,
  closed: 0
})

const distributionStats = reactive({
  levelLow: 0,
  levelMedium: 0,
  levelHigh: 0,
  statusPending: 0,
  statusProcessing: 0,
  statusHandled: 0,
  statusClosed: 0
})

const detailData = reactive({
  id: undefined,
  warningNo: '',
  warningTypeName: '',
  level: undefined,
  levelName: '',
  status: undefined,
  statusName: '',
  content: '',
  farmlandName: '',
  relatedDeviceName: '',
  triggerSourceName: '',
  triggerTime: '',
  handlerName: '',
  handleTime: '',
  closeTime: '',
  isFalseAlarm: 0
})

const aiAnalysis = reactive({
  warningOverview: '',
  possibleCauses: [],
  riskImpact: '',
  recommendations: [],
  priorityLevel: 2,
  manualReviewRequired: false
})

const aiDisposal = reactive({
  steps: [],
  checkTargets: [],
  needDeviceInspection: false,
  summary: ''
})

const reportData = reactive({
  warningId: undefined,
  warningNo: '',
  warningTypeName: '',
  warningLevelName: '',
  triggerSourceName: '',
  farmlandName: '',
  relatedDeviceName: '',
  relatedCropName: '',
  triggerTime: '',
  environmentSnapshot: null,
  aiAnalysisSummary: '',
  aiSuggestionSummary: '',
  currentStatusName: '',
  processRecordSummary: '',
  processLogs: [],
  closedLoop: false,
  closeTime: '',
  reportSummary: '',
  reportGenerateTime: ''
})

const actionForm = reactive({
  operatorName: userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员',
  note: ''
})

const closureRate = computed(() => {
  if (!overviewStats.total) return '0%'
  return `${((overviewStats.closed / overviewStats.total) * 100).toFixed(1)}%`
})

const levelDistributionTotal = computed(
  () => distributionStats.levelLow + distributionStats.levelMedium + distributionStats.levelHigh
)

const statusDistributionTotal = computed(
  () =>
    distributionStats.statusPending +
    distributionStats.statusProcessing +
    distributionStats.statusHandled +
    distributionStats.statusClosed
)

const levelDistributionList = computed(() => {
  const total = levelDistributionTotal.value
  return [
    {
      key: 'level-low',
      label: '低风险',
      value: distributionStats.levelLow,
      tone: 'tone-success',
      percent: calcPercent(distributionStats.levelLow, total),
      percentText: formatPercent(calcPercent(distributionStats.levelLow, total))
    },
    {
      key: 'level-medium',
      label: '中风险',
      value: distributionStats.levelMedium,
      tone: 'tone-warning',
      percent: calcPercent(distributionStats.levelMedium, total),
      percentText: formatPercent(calcPercent(distributionStats.levelMedium, total))
    },
    {
      key: 'level-high',
      label: '高风险',
      value: distributionStats.levelHigh,
      tone: 'tone-danger',
      percent: calcPercent(distributionStats.levelHigh, total),
      percentText: formatPercent(calcPercent(distributionStats.levelHigh, total))
    }
  ]
})

const statusDistributionList = computed(() => {
  const total = statusDistributionTotal.value
  return [
    {
      key: 'status-pending',
      label: '待确认',
      value: distributionStats.statusPending,
      tone: 'tone-primary',
      percent: calcPercent(distributionStats.statusPending, total),
      percentText: formatPercent(calcPercent(distributionStats.statusPending, total))
    },
    {
      key: 'status-processing',
      label: '处理中',
      value: distributionStats.statusProcessing,
      tone: 'tone-warning',
      percent: calcPercent(distributionStats.statusProcessing, total),
      percentText: formatPercent(calcPercent(distributionStats.statusProcessing, total))
    },
    {
      key: 'status-handled',
      label: '已处理',
      tone: 'tone-success',
      value: distributionStats.statusHandled,
      percent: calcPercent(distributionStats.statusHandled, total),
      percentText: formatPercent(calcPercent(distributionStats.statusHandled, total))
    },
    {
      key: 'status-closed',
      label: '已闭环',
      value: distributionStats.statusClosed,
      tone: 'tone-closed',
      percent: calcPercent(distributionStats.statusClosed, total),
      percentText: formatPercent(calcPercent(distributionStats.statusClosed, total))
    }
  ]
})

const allowFalseAlarm = computed(
  () => [0, 1, 2].includes(detailData.status) && detailData.id && !isTrue(detailData.isFalseAlarm)
)

onMounted(async () => {
  await fetchFarmlandOptions()
  await Promise.all([fetchWarningTable(), fetchOverviewStats()])
})

async function fetchFarmlandOptions() {
  try {
    const res = await getFieldPageApi({ current: 1, size: 100 })
    farmlandOptions.value = res.records || []
  } catch (error) {
    farmlandOptions.value = []
    ElMessage.warning('农田筛选项加载失败')
  }
}

async function fetchOverviewStats() {
  try {
    const scopeParams = queryForm.farmlandId ? { farmlandId: queryForm.farmlandId } : {}
    const [totalRes, highRes, confirmRes, processingRes, closedRes, levelLowRes, levelMediumRes, levelHighRes, statusPendingRes, statusProcessingRes, statusHandledRes, statusClosedRes] = await Promise.all([
      getWarningPageApi({ current: 1, size: 1, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, level: 3, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, status: 0, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, status: 1, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, status: 3, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, level: 1, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, level: 2, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, level: 3, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, status: 0, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, status: 1, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, status: 2, ...scopeParams }),
      getWarningPageApi({ current: 1, size: 1, status: 3, ...scopeParams })
    ])

    overviewStats.total = totalRes.total || 0
    overviewStats.highRisk = highRes.total || 0
    overviewStats.pending = (confirmRes.total || 0) + (processingRes.total || 0)
    overviewStats.closed = closedRes.total || 0

    distributionStats.levelLow = levelLowRes.total || 0
    distributionStats.levelMedium = levelMediumRes.total || 0
    distributionStats.levelHigh = levelHighRes.total || 0
    distributionStats.statusPending = statusPendingRes.total || 0
    distributionStats.statusProcessing = statusProcessingRes.total || 0
    distributionStats.statusHandled = statusHandledRes.total || 0
    distributionStats.statusClosed = statusClosedRes.total || 0
  } catch (error) {
    overviewStats.total = 0
    overviewStats.highRisk = 0
    overviewStats.pending = 0
    overviewStats.closed = 0
    distributionStats.levelLow = 0
    distributionStats.levelMedium = 0
    distributionStats.levelHigh = 0
    distributionStats.statusPending = 0
    distributionStats.statusProcessing = 0
    distributionStats.statusHandled = 0
    distributionStats.statusClosed = 0
    ElMessage.warning('预警概览统计加载失败')
  }
}

async function fetchWarningTable() {
  listLoading.value = true
  try {
    const res = await getWarningPageApi({
      current: pagination.current,
      size: pagination.size,
      level: queryForm.level,
      status: queryForm.status,
      farmlandId: queryForm.farmlandId
    })

    tableData.value = res.records || []
    pagination.total = res.total || 0

    if (!tableData.value.length) {
      selectedWarningId.value = null
      resetDetailState()
      return
    }

    const currentExists = tableData.value.some((item) => item.id === selectedWarningId.value)
    const targetId = currentExists ? selectedWarningId.value : tableData.value[0].id
    await handleSelectWarning(tableData.value.find((item) => item.id === targetId))
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    selectedWarningId.value = null
    resetDetailState()
    ElMessage.error('预警列表加载失败')
  } finally {
    listLoading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  Promise.all([fetchWarningTable(), fetchOverviewStats()])
}

function handleReset() {
  queryForm.level = undefined
  queryForm.status = undefined
  queryForm.farmlandId = undefined
  pagination.current = 1
  Promise.all([fetchWarningTable(), fetchOverviewStats()])
}

async function handleSelectWarning(row) {
  if (!row?.id) return
  selectedWarningId.value = row.id
  await fetchWarningWorkbench(row.id)
}

async function fetchWarningWorkbench(warningId) {
  detailLoading.value = true
  try {
    const [detail, flow] = await Promise.all([getWarningDetailApi(warningId), getWarningFlowApi(warningId)])
    Object.assign(detailData, detail)
    flowLogs.value = flow.processLogs || []
    await Promise.all([refreshAiAnalysis(), refreshAiDisposal()])
  } catch (error) {
    resetDetailState()
    ElMessage.error('预警详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function refreshAiAnalysis() {
  if (!selectedWarningId.value) return
  analysisLoading.value = true
  try {
    const res = await getWarningAiAnalysisApi(selectedWarningId.value)
    Object.assign(aiAnalysis, {
      warningOverview: res.warningOverview || '',
      possibleCauses: res.possibleCauses || [],
      riskImpact: res.riskImpact || '',
      recommendations: res.recommendations || [],
      priorityLevel: res.priorityLevel ?? 2,
      manualReviewRequired: !!res.manualReviewRequired
    })
  } catch (error) {
    Object.assign(aiAnalysis, {
      warningOverview: '',
      possibleCauses: [],
      riskImpact: '',
      recommendations: [],
      priorityLevel: 2,
      manualReviewRequired: false
    })
    ElMessage.warning('AI 原因分析加载失败')
  } finally {
    analysisLoading.value = false
  }
}

async function refreshAiDisposal() {
  if (!selectedWarningId.value) return
  disposalLoading.value = true
  try {
    const res = await getWarningDisposalSuggestionApi(selectedWarningId.value)
    Object.assign(aiDisposal, {
      steps: res.steps || [],
      checkTargets: res.checkTargets || [],
      needDeviceInspection: !!res.needDeviceInspection,
      summary: res.summary || ''
    })
  } catch (error) {
    Object.assign(aiDisposal, {
      steps: [],
      checkTargets: [],
      needDeviceInspection: false,
      summary: ''
    })
    ElMessage.warning('AI 处置建议加载失败')
  } finally {
    disposalLoading.value = false
  }
}

async function handleGenerateReport() {
  if (!selectedWarningId.value) {
    ElMessage.warning('请先选择一条预警')
    return
  }
  reportLoading.value = true
  reportDrawerVisible.value = true
  try {
    const res = await getWarningDisposalReportApi(selectedWarningId.value)
    Object.assign(reportData, {
      warningId: res.warningId,
      warningNo: res.warningNo || '',
      warningTypeName: res.warningTypeName || '',
      warningLevelName: res.warningLevelName || '',
      triggerSourceName: res.triggerSourceName || '',
      farmlandName: res.farmlandName || '',
      relatedDeviceName: res.relatedDeviceName || '',
      relatedCropName: res.relatedCropName || '',
      triggerTime: res.triggerTime || '',
      environmentSnapshot: res.environmentSnapshot || null,
      aiAnalysisSummary: res.aiAnalysisSummary || '',
      aiSuggestionSummary: res.aiSuggestionSummary || '',
      currentStatusName: res.currentStatusName || '',
      processRecordSummary: res.processRecordSummary || '',
      processLogs: res.processLogs || [],
      closedLoop: !!res.closedLoop,
      closeTime: res.closeTime || '',
      reportSummary: res.reportSummary || '',
      reportGenerateTime: res.reportGenerateTime || ''
    })
  } catch (error) {
    resetReportState()
    ElMessage.error('处置报告生成失败，请稍后重试')
  } finally {
    reportLoading.value = false
  }
}

async function handleCopyReportSummary() {
  if (!reportData.reportSummary) {
    ElMessage.warning('暂无可复制的报告摘要')
    return
  }
  try {
    await navigator.clipboard.writeText(reportData.reportSummary)
    ElMessage.success('报告摘要已复制')
  } catch (error) {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function handlePrintReport() {
  if (!reportData.warningId) {
    ElMessage.warning('请先生成报告')
    return
  }
  const html = `
    <html>
      <head>
        <title>预警处置报告-${reportData.warningNo || reportData.warningId}</title>
        <style>
          body { font-family: "Microsoft YaHei", sans-serif; padding: 24px; color: #1f2d2a; line-height: 1.7; }
          h1 { font-size: 22px; margin: 0 0 12px; }
          h2 { font-size: 16px; margin: 20px 0 8px; }
          .item { margin-bottom: 6px; }
          .summary { background: #f6fbf8; border: 1px solid #d8ebe1; border-radius: 10px; padding: 10px 12px; }
        </style>
      </head>
      <body>
        <h1>智能预警处置报告</h1>
        <div class="item">预警编号：${reportData.warningNo || '--'}</div>
        <div class="item">预警类型：${reportData.warningTypeName || '--'}</div>
        <div class="item">风险等级：${reportData.warningLevelName || '--'}</div>
        <div class="item">触发来源：${reportData.triggerSourceName || '--'}</div>
        <div class="item">所属农田：${reportData.farmlandName || '--'}</div>
        <div class="item">关联设备：${reportData.relatedDeviceName || '--'}</div>
        <div class="item">触发时间：${reportData.triggerTime || '--'}</div>
        <div class="item">当前状态：${reportData.currentStatusName || '--'}</div>
        <h2>AI原因分析</h2>
        <div class="summary">${reportData.aiAnalysisSummary || '--'}</div>
        <h2>AI处置建议</h2>
        <div class="summary">${reportData.aiSuggestionSummary || '--'}</div>
        <h2>处理记录摘要</h2>
        <div class="summary">${reportData.processRecordSummary || '--'}</div>
        <h2>报告摘要</h2>
        <div class="summary">${reportData.reportSummary || '--'}</div>
        <div class="item" style="margin-top: 14px;">报告生成时间：${reportData.reportGenerateTime || '--'}</div>
      </body>
    </html>
  `
  const printWindow = window.open('', '_blank', 'width=960,height=800')
  if (!printWindow) {
    ElMessage.warning('浏览器阻止了弹窗，请允许后重试')
    return
  }
  printWindow.document.write(html)
  printWindow.document.close()
  printWindow.focus()
  printWindow.print()
}

function formatSnapshotValue(value, unit = '') {
  if (value === null || value === undefined || value === '') return '--'
  return `${value}${unit}`
}

async function handleConfirmWarning() {
  await runFlowAction(() =>
    confirmWarningApi(selectedWarningId.value, {
      operatorName: actionForm.operatorName,
      processNote: actionForm.note || '确认预警有效'
    }),
    '预警确认成功'
  )
}

async function handleMarkProcessing() {
  await runFlowAction(() =>
    markWarningProcessingApi(selectedWarningId.value, {
      operatorName: actionForm.operatorName,
      processNote: actionForm.note || '进入处理流程'
    }),
    '已标记为处理中'
  )
}

async function handleSubmitResult() {
  if (!actionForm.note) {
    ElMessage.warning('请填写处理结果说明后再提交')
    return
  }
  await runFlowAction(() =>
    submitWarningHandleResultApi(selectedWarningId.value, {
      operatorName: actionForm.operatorName,
      handleRemark: actionForm.note
    }),
    '处理结果提交成功'
  )
}

async function handleCloseLoop() {
  await runFlowAction(() =>
    closeWarningApi(selectedWarningId.value, {
      operatorName: actionForm.operatorName,
      processNote: actionForm.note || '预警处置完成并闭环'
    }),
    '已标记为闭环'
  )
}

async function handleMarkFalseAlarm() {
  if (!actionForm.note) {
    ElMessage.warning('请填写误报说明后再提交')
    return
  }
  await runFlowAction(() =>
    markWarningFalseAlarmApi(selectedWarningId.value, {
      operatorName: actionForm.operatorName,
      falseAlarmReason: actionForm.note
    }),
    '误报标记成功'
  )
}

async function runFlowAction(apiCall, successMessage) {
  if (!selectedWarningId.value) return
  actionLoading.value = true
  try {
    await apiCall()
    ElMessage.success(successMessage)
    actionForm.note = ''
    await Promise.all([fetchWarningTable(), fetchOverviewStats()])
  } catch (error) {
    ElMessage.error('状态流转失败，请检查后端服务后重试')
  } finally {
    actionLoading.value = false
  }
}

function calcPercent(value, total) {
  if (!total) return 0
  return Number(((value / total) * 100).toFixed(1))
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function aiPriorityLabel(level) {
  return level === 3 ? '高' : level === 1 ? '低' : '中'
}

function timelineTypeByStatus(status) {
  if (status === 3) return 'success'
  if (status === 2) return 'primary'
  if (status === 1) return 'warning'
  return 'info'
}

function isTrue(value) {
  return value === 1 || value === true
}

function resetDetailState() {
  Object.assign(detailData, {
    id: undefined,
    warningNo: '',
    warningTypeName: '',
    level: undefined,
    levelName: '',
    status: undefined,
    statusName: '',
    content: '',
    farmlandName: '',
    relatedDeviceName: '',
    triggerSourceName: '',
    triggerTime: '',
    handlerName: '',
    handleTime: '',
    closeTime: '',
    isFalseAlarm: 0
  })
  flowLogs.value = []
  Object.assign(aiAnalysis, {
    warningOverview: '',
    possibleCauses: [],
    riskImpact: '',
    recommendations: [],
    priorityLevel: 2,
    manualReviewRequired: false
  })
  Object.assign(aiDisposal, {
    steps: [],
    checkTargets: [],
    needDeviceInspection: false,
    summary: ''
  })
  resetReportState()
}

function resetReportState() {
  Object.assign(reportData, {
    warningId: undefined,
    warningNo: '',
    warningTypeName: '',
    warningLevelName: '',
    triggerSourceName: '',
    farmlandName: '',
    relatedDeviceName: '',
    relatedCropName: '',
    triggerTime: '',
    environmentSnapshot: null,
    aiAnalysisSummary: '',
    aiSuggestionSummary: '',
    currentStatusName: '',
    processRecordSummary: '',
    processLogs: [],
    closedLoop: false,
    closeTime: '',
    reportSummary: '',
    reportGenerateTime: ''
  })
}
</script>

<style scoped lang="scss">
.warning-overview-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.warning-layout {
  display: grid;
  grid-template-columns: minmax(0, 44fr) minmax(0, 56fr);
  gap: 16px;
  align-items: stretch;
}

.warning-layout__left,
.warning-layout__right {
  display: grid;
  gap: 16px;
}

.warning-layout__left {
  grid-template-rows: auto 1fr auto;
}

.warning-layout__right {
  grid-template-rows: 1fr;
}

.warning-list-card {
  display: flex;
  flex-direction: column;
}

.warning-list-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.warning-list-card .page-table-shell {
  flex: 1;
}

.warning-list-card :deep(.el-table) {
  height: 100%;
}

.warning-filter-form {
  row-gap: 8px;

  :deep(.el-form-item) {
    margin-bottom: 8px;
  }
}

.warning-list-summary {
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.warning-list-summary__item {
  padding: 9px 12px;
  border: 1px solid rgba(15, 73, 54, 0.1);
  border-radius: 10px;
  background: #fbfefd;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;

  span {
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    font-size: 18px;
    line-height: 1;
    color: var(--app-text);
  }
}

.warning-distribution-card {
  min-height: 240px;
}

.warning-distribution-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.distribution-panel {
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 12px;
  background: #fcfefd;
  padding: 10px 11px;
}

.distribution-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;

  strong {
    font-size: 13px;
    color: var(--app-text);
  }

  span {
    font-size: 12px;
    color: var(--app-text-secondary);
  }
}

.distribution-list {
  display: grid;
  gap: 8px;
}

.distribution-item {
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 10px;
  background: #fff;
  padding: 8px 9px;
  display: grid;
  gap: 6px;
}

.distribution-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  em {
    font-style: normal;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    font-size: 13px;
    color: var(--app-text);
  }
}

.distribution-track {
  height: 6px;
  border-radius: 999px;
  background: rgba(15, 73, 54, 0.08);
  overflow: hidden;
}

.distribution-track__bar {
  display: block;
  height: 100%;
  border-radius: 999px;
  min-width: 2px;
}

.distribution-item__percent {
  display: block;
  font-size: 12px;
  color: var(--app-text-secondary);
  text-align: right;
}

.distribution-track__bar.tone-success {
  background: #16a34a;
}

.distribution-track__bar.tone-warning {
  background: #f59e0b;
}

.distribution-track__bar.tone-danger {
  background: #ef4444;
}

.distribution-track__bar.tone-primary {
  background: #3b82f6;
}

.distribution-track__bar.tone-closed {
  background: #22c55e;
}

.warning-center-table :deep(.el-table__row) {
  cursor: pointer;
}

.warning-center-table :deep(.el-table__row.current-row > td.el-table__cell) {
  background: rgba(31, 143, 95, 0.08) !important;
}

.warning-workbench-card :deep(.el-card__body) {
  padding-top: 10px;
  min-height: 100%;
}

.warning-workbench {
  display: grid;
  gap: 16px;
}

.workbench-section {
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 252, 251, 0.98));
}

.workbench-section__head {
  padding: 12px 14px 11px;
  border-bottom: 1px solid rgba(15, 73, 54, 0.08);

  strong {
    font-size: 14px;
    color: var(--app-text);
  }
}

.workbench-section__head--with-action {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.workbench-section-title {
  display: grid;
  gap: 2px;

  span {
    font-size: 12px;
    line-height: 1.45;
    color: var(--app-text-secondary);
  }
}

.overview-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 12px 12px 8px;
}

.overview-info-item {
  padding: 10px 12px;
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 12px;
  background: #ffffff;

  span {
    display: block;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    display: block;
    margin-top: 5px;
    font-size: 16px;
    color: var(--app-text);
    line-height: 1.4;
  }
}

.overview-summary-block {
  margin: 0 12px;
  padding: 10px 12px;
  border: 1px dashed rgba(15, 73, 54, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);

  span {
    display: block;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  p {
    margin: 6px 0 0;
    line-height: 1.6;
    color: var(--app-text);
  }
}

.overview-snapshot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 10px 12px 12px;
}

.overview-snapshot-item {
  padding: 9px 11px;
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 12px;
  background: #f9fcfb;

  span {
    display: block;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    display: block;
    margin-top: 6px;
    font-size: 14px;
    color: var(--app-text);
  }
}

.ai-workbench-grid {
  padding: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ai-pane {
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 14px;
  background: #fff;
  padding: 10px 11px;
  display: grid;
  gap: 10px;
}

.ai-pane__head {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;

  strong {
    margin-right: auto;
    font-size: 14px;
    color: var(--app-text);
  }
}

.ai-overview {
  margin: 0;
  line-height: 1.65;
  color: var(--app-text);
}

.ai-block {
  span {
    display: block;
    margin-bottom: 6px;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  p {
    margin: 0 0 10px;
    line-height: 1.6;
    color: var(--app-text);
  }

  ul,
  ol {
    margin: 0 0 10px 18px;
    padding: 0;
  }

  li {
    margin-bottom: 6px;
    line-height: 1.55;
    color: var(--app-text);
  }
}

.ordered-list {
  list-style: decimal;
}

.ai-summary {
  margin: 6px 0 0;
  line-height: 1.65;
  color: var(--app-text-secondary);
}

.ai-meta-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-form {
  padding: 12px 12px 4px;

  :deep(.el-form-item:last-child) {
    margin-bottom: 8px;
  }
}

.flow-actions {
  padding: 0 12px 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.warning-timeline {
  padding: 12px 14px 12px;
}

.timeline-card {
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 12px;
  padding: 10px 12px 9px;
  background: #fff;

  p {
    margin: 8px 0;
    color: var(--app-text);
    line-height: 1.6;
  }

  span {
    font-size: 12px;
    color: var(--app-text-secondary);
  }
}

.timeline-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.empty-hint {
  min-height: 520px;
  display: grid;
  place-items: center;
  color: var(--app-text-secondary);
  text-align: center;
  padding: 0 24px;
}

.report-view {
  display: grid;
  gap: 14px;
}

.report-section {
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 14px;
  background: #fff;
  padding: 14px;

  h4 {
    margin: 0 0 10px;
    font-size: 14px;
    color: var(--app-text);
  }
}

.report-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;

  h3 {
    margin: 0;
    font-size: 18px;
    color: var(--app-text);
  }

  p {
    margin: 6px 0 0;
    color: var(--app-text-secondary);
  }
}

.report-head-actions {
  display: flex;
  gap: 8px;
}

.report-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.report-item {
  border: 1px solid rgba(15, 73, 54, 0.08);
  border-radius: 10px;
  padding: 8px 10px;
  background: #fafcfb;

  span {
    display: block;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    display: block;
    margin-top: 4px;
    color: var(--app-text);
    font-size: 14px;
    line-height: 1.5;
    word-break: break-word;
  }
}

.report-plain-block {
  border: 1px dashed rgba(15, 73, 54, 0.16);
  border-radius: 10px;
  padding: 10px 12px;
  color: var(--app-text);
  line-height: 1.65;
  margin-bottom: 8px;
}

.report-summary-block {
  border-radius: 12px;
  background: rgba(31, 143, 95, 0.08);
  border: 1px solid rgba(31, 143, 95, 0.2);
  padding: 10px 12px;
  line-height: 1.7;
  color: var(--app-text);
}

.report-timeline {
  padding-top: 8px;
}

@container app-content (max-width: 1160px) {
  .warning-overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@container app-content (max-width: 1000px) {
  .warning-layout {
    grid-template-columns: 1fr;
  }

  .warning-layout__left {
    grid-template-rows: auto auto auto;
  }

  .warning-list-card {
    min-height: 0;
  }

  .warning-list-card .page-table-shell {
    min-height: 320px;
  }

  .warning-distribution-grid,
  .overview-snapshot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@container app-content (max-width: 760px) {
  .warning-overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .warning-list-summary,
  .overview-info-grid,
  .ai-workbench-grid,
  .warning-distribution-grid {
    grid-template-columns: 1fr;
  }

  .overview-snapshot-grid {
    grid-template-columns: 1fr;
  }

  .report-grid,
  .report-head {
    grid-template-columns: 1fr;
    display: grid;
  }
}

.form-hint {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}
</style>
