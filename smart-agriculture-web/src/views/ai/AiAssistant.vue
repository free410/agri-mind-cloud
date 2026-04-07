<template>
  <div class="page-shell ai-warning-page">
    <section class="page-banner">
      <div class="page-banner__main">
        <span class="page-banner__badge">AI 智能助手</span>
        <h1 class="page-banner__title">预警联动分析工作台</h1>
        <p class="page-banner__desc">面向智能预警闭环，支持分析、建议、简报与闭环总结。</p>
      </div>
      <div class="page-banner__actions">
        <div class="page-banner__meta">
          <span class="status-dot online"></span>
          <span>预警 AI 接口在线</span>
        </div>
        <div class="page-banner__meta">
          <span class="status-dot"></span>
          <span>会话 {{ sessionIdLabel }}</span>
        </div>
      </div>
    </section>

    <section class="ai-warning-layout">
      <el-card class="page-content-card quick-panel" shadow="never">
        <template #header>
          <div class="page-panel-title">
            <strong>业务快捷入口</strong>
            <span class="page-panel-tip">点击后直接联动预警与 AI 分析能力，输出结构化结果。</span>
          </div>
        </template>

        <div class="quick-actions">
          <button
            v-for="item in quickActions"
            :key="item.key"
            type="button"
            class="quick-action-btn"
            :disabled="shortcutLoadingKey === item.key"
            @click="handleShortcut(item.key)"
          >
            <div class="quick-action-btn__head">
              <strong>{{ item.title }}</strong>
              <el-tag v-if="shortcutLoadingKey === item.key" size="small" type="warning" effect="plain">处理中</el-tag>
            </div>
            <span>{{ item.desc }}</span>
          </button>
        </div>

        <div class="warning-list-mini">
          <div class="warning-list-mini__head">
            <strong>最近预警</strong>
            <el-button text type="primary" @click="fetchLatestWarnings">刷新</el-button>
          </div>
          <div v-if="latestWarnings.length" class="warning-list-mini__items">
            <button
              v-for="warning in latestWarnings"
              :key="warning.id"
              type="button"
              class="warning-list-mini__item"
              :class="{ active: selectedWarning.id === warning.id }"
              @click="handleSelectWarning(warning)"
            >
              <div>
                <strong>{{ warning.warningTypeName || warning.warningNo || `预警#${warning.id}` }}</strong>
                <span>{{ warning.farmlandName || '未知农田' }}</span>
              </div>
              <el-tag size="small" :type="priorityTagByLevel(warning.level)" effect="plain">
                {{ warning.levelName || levelLabel(warning.level) }}
              </el-tag>
            </button>
          </div>
          <el-empty v-else description="暂无预警数据" :image-size="56" />
        </div>
      </el-card>

      <el-card class="page-content-card workbench-panel" shadow="never">
        <template #header>
          <div class="page-panel-head">
            <div class="page-panel-title">
              <strong>结构化分析结果</strong>
              <span class="page-panel-tip">统一输出：结论、可能原因、建议、优先级。</span>
            </div>
            <div class="page-toolbar">
              <el-tag type="success" effect="light">联动模式</el-tag>
              <el-tag type="info" effect="light">更新 {{ lastUpdateTime }}</el-tag>
            </div>
          </div>
        </template>

        <div v-if="!selectedWarning.id && !panelLoading" class="workbench-empty">
          请先点击左侧快捷入口，或从“最近预警”中选择一条记录。
        </div>

        <div v-else v-loading="panelLoading" class="workbench-content">
          <section class="selected-warning-card">
            <div class="selected-warning-card__head">
              <strong>当前联动预警</strong>
              <el-tag type="warning" effect="plain">{{ selectedWarning.statusName || '待确认' }}</el-tag>
            </div>
            <div class="selected-warning-grid">
              <div class="selected-warning-item">
                <span>预警编号</span>
                <strong>{{ selectedWarning.warningNo || `#${selectedWarning.id || '--'}` }}</strong>
              </div>
              <div class="selected-warning-item">
                <span>预警类型</span>
                <strong>{{ selectedWarning.warningTypeName || '--' }}</strong>
              </div>
              <div class="selected-warning-item">
                <span>风险等级</span>
                <strong>{{ selectedWarning.levelName || '--' }}</strong>
              </div>
              <div class="selected-warning-item">
                <span>所属农田</span>
                <strong>{{ selectedWarning.farmlandName || '--' }}</strong>
              </div>
              <div class="selected-warning-item">
                <span>关联设备</span>
                <strong>{{ selectedWarning.relatedDeviceName || '--' }}</strong>
              </div>
              <div class="selected-warning-item">
                <span>触发时间</span>
                <strong>{{ selectedWarning.triggerTime || '--' }}</strong>
              </div>
            </div>
          </section>

          <section class="quick-result-card">
            <div class="quick-result-card__head">
              <strong>{{ quickInsight.title }}</strong>
              <el-tag :type="priorityTagByLevel(quickInsight.priorityLevel)" effect="plain">
                {{ priorityLabel(quickInsight.priorityLevel) }}
              </el-tag>
            </div>
            <div class="quick-result-grid">
              <article class="quick-result-block">
                <span>结论</span>
                <p>{{ quickInsight.conclusion || '--' }}</p>
              </article>
              <article class="quick-result-block">
                <span>可能原因</span>
                <ul>
                  <li v-for="item in quickInsight.possibleCauses" :key="item">{{ item }}</li>
                  <li v-if="!quickInsight.possibleCauses.length">暂无</li>
                </ul>
              </article>
              <article class="quick-result-block">
                <span>建议</span>
                <ul>
                  <li v-for="item in quickInsight.suggestions" :key="item">{{ item }}</li>
                  <li v-if="!quickInsight.suggestions.length">暂无</li>
                </ul>
              </article>
              <article class="quick-result-block quick-result-block--stats">
                <span>统计快照</span>
                <div class="stats-grid">
                  <div>
                    <em>总预警</em>
                    <strong>{{ quickInsight.stats.total }}</strong>
                  </div>
                  <div>
                    <em>高风险</em>
                    <strong>{{ quickInsight.stats.high }}</strong>
                  </div>
                  <div>
                    <em>待处理</em>
                    <strong>{{ quickInsight.stats.pending }}</strong>
                  </div>
                  <div>
                    <em>已闭环</em>
                    <strong>{{ quickInsight.stats.closed }}</strong>
                  </div>
                </div>
              </article>
            </div>
          </section>

          <section class="result-grid">
            <article class="result-card">
              <div class="result-card__head">
                <strong>AI 原因分析</strong>
                <el-tag :type="priorityTagByLevel(aiAnalysis.priorityLevel)" effect="plain">
                  {{ priorityLabel(aiAnalysis.priorityLevel) }}
                </el-tag>
              </div>
              <div class="result-block">
                <span>预警概述</span>
                <p>{{ aiAnalysis.warningOverview || '暂无分析结果' }}</p>
              </div>
              <div class="result-block">
                <span>可能原因</span>
                <ul>
                  <li v-for="item in aiAnalysis.possibleCauses" :key="item">{{ item }}</li>
                  <li v-if="!aiAnalysis.possibleCauses.length">暂无原因项</li>
                </ul>
              </div>
              <div class="result-block">
                <span>风险影响</span>
                <p>{{ aiAnalysis.riskImpact || '--' }}</p>
              </div>
            </article>

            <article class="result-card">
              <div class="result-card__head">
                <strong>AI 处置建议</strong>
                <el-tag :type="aiDisposal.needDeviceInspection ? 'warning' : 'info'" effect="plain">
                  {{ aiDisposal.needDeviceInspection ? '建议设备巡检' : '无需额外巡检' }}
                </el-tag>
              </div>
              <div class="result-block">
                <span>推荐步骤</span>
                <ol>
                  <li v-for="step in aiDisposal.steps" :key="step">{{ step }}</li>
                  <li v-if="!aiDisposal.steps.length">暂无处置步骤</li>
                </ol>
              </div>
              <div class="result-block">
                <span>推荐检查对象</span>
                <ul>
                  <li v-for="target in aiDisposal.checkTargets" :key="target">{{ target }}</li>
                  <li v-if="!aiDisposal.checkTargets.length">暂无检查对象</li>
                </ul>
              </div>
              <div class="result-block">
                <span>总结</span>
                <p>{{ aiDisposal.summary || '--' }}</p>
              </div>
            </article>
          </section>
        </div>

        <section class="chat-panel">
          <div class="chat-panel__head">
            <strong>跟进问答</strong>
            <span>围绕当前预警继续追问，支持答辩场景互动展示。</span>
          </div>
          <div ref="messageContainerRef" class="chat-records">
            <div v-for="message in messages" :key="message.id" class="chat-item" :class="message.role">
              <div class="chat-item__meta">
                <strong>{{ message.role === 'user' ? '管理员' : 'AI 助手' }}</strong>
                <span>{{ message.time }}</span>
              </div>
              <p>{{ message.content }}</p>
            </div>
          </div>
          <div class="chat-editor">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              resize="none"
              maxlength="1000"
              show-word-limit
              placeholder="请输入追问内容，例如：该预警的现场排查优先顺序是什么？"
              @keydown.enter.exact.prevent="handleSend"
            />
            <div class="chat-editor__toolbar">
              <el-button plain @click="handleClear">清空会话</el-button>
              <el-button type="primary" :loading="sending" @click="handleSend">发送问题</el-button>
            </div>
          </div>
        </section>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getWarningAiAnalysisApi,
  getWarningDisposalSuggestionApi,
  getWarningQuickSummaryApi,
  sendAiChatApi
} from '@/api/ai'
import { getWarningPageApi } from '@/api/warning'

const quickActions = [
  { key: 'latest-analysis', title: '分析最新预警', desc: '定位最新预警并输出结构化原因分析。' },
  { key: 'high-risk-explain', title: '解释当前高风险预警', desc: '优先选择高风险事件并解释影响与复核建议。' },
  { key: 'generate-disposal', title: '生成处置建议', desc: '为当前预警生成步骤化处置方案。' },
  { key: 'daily-brief', title: '生成今日异常简报', desc: '聚合今日异常并输出结论、原因、建议。' },
  { key: 'closure-summary', title: '总结今日闭环情况', desc: '聚合闭环效率，给出优化方向。' }
]

const latestWarnings = ref([])
const panelLoading = ref(false)
const shortcutLoadingKey = ref('')
const lastUpdateTime = ref('--:--')

const selectedWarning = reactive({
  id: null,
  warningNo: '',
  warningTypeName: '',
  level: 2,
  levelName: '',
  status: 0,
  statusName: '',
  farmlandName: '',
  relatedDeviceName: '',
  triggerTime: ''
})

const aiAnalysis = reactive({
  warningOverview: '',
  possibleCauses: [],
  riskImpact: '',
  priorityLevel: 2
})

const aiDisposal = reactive({
  steps: [],
  checkTargets: [],
  needDeviceInspection: false,
  summary: ''
})

const quickInsight = reactive({
  title: 'AI 业务结论',
  conclusion: '',
  possibleCauses: [],
  suggestions: [],
  priorityLevel: 2,
  stats: {
    total: 0,
    high: 0,
    pending: 0,
    closed: 0
  }
})

const messageContainerRef = ref(null)
const inputText = ref('')
const sending = ref(false)
const sessionId = ref(createSessionId())
const messageSeed = ref(1)
const messages = ref([])

const sessionIdLabel = computed(() => sessionId.value.slice(-8).toUpperCase())

function createSessionId() {
  return `session-${Date.now()}`
}

function nowHHMM() {
  const now = new Date()
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

function markUpdated() {
  lastUpdateTime.value = nowHHMM()
}

function appendMessage(role, content) {
  messages.value.push({ id: messageSeed.value++, role, content, time: nowHHMM() })
  nextTick(() => {
    if (messageContainerRef.value) {
      messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight
    }
  })
}

function priorityLabel(level) {
  if (Number(level) === 3) return '高优先级'
  if (Number(level) === 1) return '低优先级'
  return '中优先级'
}

function levelLabel(level) {
  if (Number(level) === 3) return '高'
  if (Number(level) === 2) return '中'
  if (Number(level) === 1) return '低'
  return '--'
}

function priorityTagByLevel(level) {
  if (Number(level) === 3) return 'danger'
  if (Number(level) === 1) return 'success'
  return 'warning'
}

function setSelectedWarning(warning) {
  Object.assign(selectedWarning, {
    id: warning?.id || null,
    warningNo: warning?.warningNo || '',
    warningTypeName: warning?.warningTypeName || '',
    level: warning?.level ?? 2,
    levelName: warning?.levelName || levelLabel(warning?.level),
    status: warning?.status ?? 0,
    statusName: warning?.statusName || '',
    farmlandName: warning?.farmlandName || '',
    relatedDeviceName: warning?.relatedDeviceName || '',
    triggerTime: warning?.triggerTime || ''
  })
}

function applyQuickSummary(summary, fallbackTitle = 'AI 业务结论') {
  Object.assign(quickInsight, {
    title: summary?.title || fallbackTitle,
    conclusion: summary?.conclusion || '',
    possibleCauses: summary?.possibleCauses || [],
    suggestions: summary?.suggestions || [],
    priorityLevel: summary?.priorityLevel ?? 2,
    stats: {
      total: summary?.totalWarnings ?? 0,
      high: summary?.highRiskWarnings ?? 0,
      pending: summary?.pendingWarnings ?? 0,
      closed: summary?.closedWarnings ?? 0
    }
  })
}

async function fetchLatestWarnings() {
  try {
    const res = await getWarningPageApi({ current: 1, size: 8 })
    latestWarnings.value = res.records || []
    markUpdated()
  } catch (error) {
    latestWarnings.value = []
    ElMessage.warning('预警列表加载失败')
  }
}

async function pickLatestWarning() {
  const res = await getWarningPageApi({ current: 1, size: 1 })
  return res.records?.[0] || null
}

async function pickHighRiskWarning() {
  const res = await getWarningPageApi({ current: 1, size: 10, level: 3 })
  const list = res.records || []
  if (!list.length) return null
  return list.find((item) => Number(item.status) !== 3) || list[0]
}

async function loadAnalysisForWarning(warningId) {
  const res = await getWarningAiAnalysisApi(warningId)
  Object.assign(aiAnalysis, {
    warningOverview: res.warningOverview || '',
    possibleCauses: res.possibleCauses || [],
    riskImpact: res.riskImpact || '',
    priorityLevel: res.priorityLevel ?? 2
  })
  applyQuickSummary(
    {
      title: '预警分析结论',
      conclusion: res.warningOverview || '',
      possibleCauses: res.possibleCauses || [],
      suggestions: res.recommendations || [],
      priorityLevel: res.priorityLevel ?? 2
    },
    '预警分析结论'
  )
}

async function loadDisposalForWarning(warningId) {
  const res = await getWarningDisposalSuggestionApi(warningId)
  Object.assign(aiDisposal, {
    steps: res.steps || [],
    checkTargets: res.checkTargets || [],
    needDeviceInspection: !!res.needDeviceInspection,
    summary: res.summary || ''
  })
}

async function handleSelectWarning(warning) {
  if (!warning?.id) return
  panelLoading.value = true
  try {
    setSelectedWarning(warning)
    await Promise.all([loadAnalysisForWarning(warning.id), loadDisposalForWarning(warning.id)])
    appendMessage('assistant', `已切换到预警 ${warning.warningNo || `#${warning.id}`}，结构化结果已更新。`)
    markUpdated()
  } catch (error) {
    appendMessage('assistant', '当前预警详情加载失败，请稍后重试。')
  } finally {
    panelLoading.value = false
  }
}

async function handleShortcut(key) {
  shortcutLoadingKey.value = key
  panelLoading.value = true
  try {
    if (key === 'latest-analysis') await actionLatestAnalysis()
    if (key === 'high-risk-explain') await actionHighRiskExplain()
    if (key === 'generate-disposal') await actionGenerateDisposal()
    if (key === 'daily-brief') await actionDailyBrief()
    if (key === 'closure-summary') await actionClosureSummary()
    markUpdated()
  } catch (error) {
    appendMessage('assistant', '本次联动请求失败，请稍后重试。')
  } finally {
    shortcutLoadingKey.value = ''
    panelLoading.value = false
  }
}

async function actionLatestAnalysis() {
  const warning = await pickLatestWarning()
  if (!warning) {
    ElMessage.warning('暂无可分析的预警数据')
    return
  }
  setSelectedWarning(warning)
  await Promise.all([loadAnalysisForWarning(warning.id), loadDisposalForWarning(warning.id)])
  appendMessage('assistant', `已完成“最新预警”分析：${warning.warningTypeName || warning.warningNo || warning.id}`)
}

async function actionHighRiskExplain() {
  const warning = (await pickHighRiskWarning()) || (await pickLatestWarning())
  if (!warning) {
    ElMessage.warning('暂无高风险预警可解释')
    return
  }
  setSelectedWarning(warning)
  await Promise.all([loadAnalysisForWarning(warning.id), loadDisposalForWarning(warning.id)])
  appendMessage('assistant', '已输出当前高风险预警解释，请查看结构化卡片。')
}

async function actionGenerateDisposal() {
  let warning = selectedWarning.id ? { ...selectedWarning } : null
  if (!warning?.id) warning = (await pickHighRiskWarning()) || (await pickLatestWarning())
  if (!warning) {
    ElMessage.warning('暂无可生成处置建议的预警')
    return
  }
  setSelectedWarning(warning)
  await Promise.all([loadAnalysisForWarning(warning.id), loadDisposalForWarning(warning.id)])
  applyQuickSummary(
    {
      title: '处置建议结论',
      conclusion: aiDisposal.summary || '已生成处置建议，请按步骤执行。',
      possibleCauses: aiDisposal.checkTargets || [],
      suggestions: aiDisposal.steps || [],
      priorityLevel: aiAnalysis.priorityLevel ?? warning.level ?? 2
    },
    '处置建议结论'
  )
  appendMessage('assistant', '已生成处置建议，建议按“步骤 1 -> 步骤 2 -> 步骤 3”执行。')
}

async function actionDailyBrief() {
  const res = await getWarningQuickSummaryApi('abnormal')
  applyQuickSummary(res, '今日异常简报')
  if (res.relatedWarningId) {
    const target = latestWarnings.value.find((item) => item.id === res.relatedWarningId)
    if (target) {
      setSelectedWarning(target)
      await Promise.all([loadAnalysisForWarning(target.id), loadDisposalForWarning(target.id)])
    }
  }
  appendMessage('assistant', '今日异常简报已生成，可直接用于比赛展示。')
}

async function actionClosureSummary() {
  const res = await getWarningQuickSummaryApi('closure')
  applyQuickSummary(res, '今日闭环总结')
  appendMessage('assistant', '今日闭环总结已生成，已给出闭环效率与优化建议。')
}

async function handleSend() {
  const question = inputText.value.trim()
  if (!question) {
    ElMessage.warning('请输入要追问的内容')
    return
  }

  appendMessage('user', question)
  inputText.value = ''
  sending.value = true

  try {
    const response = await sendAiChatApi({
      sessionId: sessionId.value,
      question,
      relatedModule: selectedWarning.id ? 'WARNING' : 'GENERAL',
      relatedRecordId: selectedWarning.id || undefined
    })
    sessionId.value = response.sessionId || sessionId.value
    appendMessage('assistant', response.answer || '已收到问题，但暂未返回内容。')
  } catch (error) {
    appendMessage('assistant', '调用 AI 对话接口失败，请稍后重试。')
  } finally {
    sending.value = false
  }
}

function handleClear() {
  sessionId.value = createSessionId()
  messages.value = []
  messageSeed.value = 1
  appendMessage('assistant', '会话已重置，可继续发起预警联动分析。')
}

onMounted(async () => {
  appendMessage('assistant', '欢迎使用 AI 助手联动模式，建议先点击“分析最新预警”。')
  await fetchLatestWarnings()
})
</script>

<style scoped lang="scss">
.ai-warning-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.quick-panel :deep(.el-card__body) {
  display: grid;
  gap: 14px;
}

.quick-actions {
  display: grid;
  gap: 10px;
}

.quick-action-btn {
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 14px;
  background: #fff;
  text-align: left;
  padding: 12px 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-action-btn:hover {
  border-color: rgba(23, 138, 98, 0.24);
  box-shadow: var(--app-shadow-xs);
  transform: translateY(-1px);
}

.quick-action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.78;
}

.quick-action-btn__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.quick-action-btn__head strong {
  font-size: 14px;
  color: var(--app-text);
}

.quick-action-btn span {
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-secondary);
}

.warning-list-mini {
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 14px;
  background: #fbfdfc;
  padding: 12px;
}

.warning-list-mini__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.warning-list-mini__head strong {
  font-size: 14px;
  color: var(--app-text);
}

.warning-list-mini__items {
  display: grid;
  gap: 8px;
}

.warning-list-mini__item {
  width: 100%;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 12px;
  background: #fff;
  padding: 9px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.warning-list-mini__item:hover {
  border-color: rgba(23, 138, 98, 0.22);
}

.warning-list-mini__item.active {
  border-color: rgba(23, 138, 98, 0.28);
  background: #f3faf6;
}

.warning-list-mini__item strong {
  display: block;
  font-size: 13px;
  color: var(--app-text);
}

.warning-list-mini__item span {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.workbench-panel :deep(.el-card__body) {
  display: grid;
  gap: 14px;
}

.workbench-empty {
  min-height: 240px;
  border: 1px dashed rgba(17, 82, 62, 0.24);
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: var(--app-text-secondary);
  font-size: 14px;
}

.workbench-content {
  display: grid;
  gap: 12px;
}

.selected-warning-card,
.quick-result-card,
.chat-panel {
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 16px;
  background: #fff;
  padding: 14px;
}

.selected-warning-card__head,
.quick-result-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.selected-warning-card__head strong,
.quick-result-card__head strong {
  font-size: 14px;
  color: var(--app-text);
}

.selected-warning-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.selected-warning-item {
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 12px;
  padding: 10px;
}

.selected-warning-item span {
  display: block;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.selected-warning-item strong {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--app-text);
}

.quick-result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-result-block {
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 12px;
  background: #fbfdfc;
  padding: 10px;
}

.quick-result-block span {
  display: block;
  font-size: 12px;
  color: var(--app-text-secondary);
  margin-bottom: 6px;
}

.quick-result-block p {
  margin: 0;
  color: var(--app-text);
  line-height: 1.6;
}

.quick-result-block ul {
  margin: 0;
  padding-left: 18px;
}

.quick-result-block li {
  line-height: 1.55;
  color: var(--app-text);
}

.quick-result-block--stats .stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.quick-result-block--stats em {
  display: block;
  font-style: normal;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.quick-result-block--stats strong {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  color: var(--app-text);
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.result-card {
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 16px;
  background: #fff;
  padding: 14px;
  display: grid;
  gap: 10px;
}

.result-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-card__head strong {
  font-size: 14px;
  color: var(--app-text);
}

.result-block span {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.result-block p {
  margin: 0;
  color: var(--app-text);
  line-height: 1.65;
}

.result-block ul,
.result-block ol {
  margin: 0;
  padding-left: 18px;
}

.result-block li {
  line-height: 1.55;
  color: var(--app-text);
}

.chat-panel {
  padding: 12px;
}

.chat-panel__head {
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
}

.chat-panel__head strong {
  font-size: 14px;
  color: var(--app-text);
}

.chat-panel__head span {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.chat-records {
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 12px;
  padding: 10px;
  background: #fbfdfc;
  display: grid;
  gap: 8px;
}

.chat-item {
  border-radius: 12px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  background: #fff;
  padding: 8px 10px;
}

.chat-item.user {
  border-color: rgba(76, 139, 245, 0.2);
  background: #f4f8ff;
}

.chat-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.chat-item__meta strong {
  font-size: 12px;
  color: var(--app-text);
}

.chat-item__meta span {
  font-size: 11px;
  color: var(--app-text-tertiary);
}

.chat-item p {
  margin: 0;
  line-height: 1.6;
  font-size: 13px;
  color: var(--app-text);
}

.chat-editor {
  margin-top: 10px;
}

.chat-editor__toolbar {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.65);
}

.status-dot.online {
  background: #7ef1bb;
  box-shadow: 0 0 0 4px rgba(126, 241, 187, 0.14);
}

@media (max-width: 1360px) {
  .ai-warning-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1080px) {
  .selected-warning-grid,
  .result-grid,
  .quick-result-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .selected-warning-grid,
  .result-grid,
  .quick-result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
