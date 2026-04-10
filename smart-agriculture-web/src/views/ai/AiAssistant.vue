<template>
  <div class="page-shell ai-rag-page">
    <section class="ai-rag-header">
      <div class="ai-rag-header__main">
        <h1>AI 助手</h1>
        <p>支持业务数据联动问答、农业知识检索与混合分析。</p>
        <p class="ai-rag-header__graph-hint">
          <strong>知识图谱</strong>：后端会将问题与 Neo4j 中实体做匹配，把命中实体写入模型上下文。需在「知识网络」中完成灌库；提问尽量包含作物/病害等名称（如「番茄晚疫病」）。命中时回答内会出现绿色「图谱实体」并可跳转查看子图。
        </p>
      </div>
      <div class="ai-rag-header__meta">
        <el-tag type="success" effect="plain">AI 在线</el-tag>
        <el-tag effect="plain">{{ modeLabelMap[currentMode] }}</el-tag>
        <el-tag v-if="latestWarning?.warningNo" type="warning" effect="plain">
          联动预警 {{ latestWarning.warningNo }}
        </el-tag>
      </div>
    </section>

    <section class="page-content-card ai-rag-workspace">
      <div ref="messageContainerRef" class="ai-rag-dialogue">
        <div v-if="!messages.length" class="welcome-panel">
          <div class="welcome-panel__icon">AI</div>
          <h3>欢迎使用智慧农业 RAG 助手</h3>
          <p>你可以直接提问，也可以让我结合实时业务数据与农业知识库完成分析。</p>

          <div class="welcome-mode-cards">
            <article v-for="item in modeCards" :key="item.key" class="welcome-mode-card">
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </article>
          </div>

          <div class="welcome-samples">
            <button
              v-for="item in sampleQuestions"
              :key="item.key"
              type="button"
              class="welcome-sample-btn"
              @click="handleSampleQuestion(item)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <div v-else class="chat-list">
          <article
            v-for="message in messages"
            :key="message.id"
            class="chat-item"
            :class="`chat-item--${message.role}`"
          >
            <div class="chat-item__meta">
              <strong>{{ message.role === 'user' ? '管理员' : 'AI 助手' }}</strong>
              <span>{{ message.time }}</span>
            </div>

            <div class="chat-item__bubble">
              <p v-if="message.content && !message.structured" class="chat-item__text">
                <TypewriterText
                  :text="message.content"
                  :active="isPlainStreaming(message)"
                  @complete="onPlainStreamComplete"
                  @progress="scrollToBottom"
                />
              </p>

              <section
                v-if="message.structured"
                :class="['rag-answer-v2', 'rag-answer-dash', priorityShellClass(message.structured.priority)]"
              >
                <header class="rag-dash__masthead rag-dash__masthead--compact">
                  <div class="rag-dash__masthead-left">
                    <div class="rag-dash__logo rag-dash__logo--sm" aria-hidden="true">
                      <el-icon :size="20"><DataAnalysis /></el-icon>
                    </div>
                    <div class="rag-dash__masthead-text">
                      <div class="rag-dash__title-row">
                        <span class="rag-dash__badge">简报</span>
                        <span class="rag-dash__mode">{{ message.structured.title }}</span>
                        <span class="rag-dash__time">{{ message.time }}</span>
                      </div>
                      <div class="rag-dash__masthead-inline">
                        <el-tag :type="priorityTagType(message.structured.priority)" size="small" effect="dark">
                          {{ priorityLabel(message.structured.priority) }}
                        </el-tag>
                        <el-tag
                          :type="message.structured.shouldManualReview ? 'danger' : 'success'"
                          size="small"
                          effect="plain"
                        >
                          {{ message.structured.shouldManualReview ? '需复核' : '可自闭环' }}
                        </el-tag>
                        <el-tag v-if="message.structured.fallback" type="warning" size="small">兜底</el-tag>
                        <span class="rag-dash__urgency-inline">
                          紧急度
                          <strong :style="{ color: priorityBarColor(message.structured.priority) }">
                            {{ priorityUrgencyPercent(message.structured.priority) }}%
                          </strong>
                        </span>
                      </div>
                    </div>
                  </div>
                </header>

                <!-- 核心阅览区：摘要与结论视觉上集中靠前，叙述顺序仍为 ① 摘要 → ② 结论 -->
                <div class="rag-dash__focal">
                  <p
                    v-if="openingHighlightLine(message.structured) && !isStreamingSummary(message)"
                    class="rag-dash__focal-pull"
                  >
                    {{ openingHighlightLine(message.structured) }}
                  </p>
                  <div class="rag-answer-v2__hero rag-dash__hero rag-dash__hero--inset">
                    <div class="rag-dash__hero-bar">
                      <p class="rag-answer-v2__hero-label">① 先读这一段（执行摘要）</p>
                      <el-button type="primary" link size="small" @click="copyRagDigest(message.structured)">
                        <el-icon class="rag-dash__btn-ic"><CopyDocument /></el-icon>
                        复制全文要点
                      </el-button>
                    </div>
                    <p
                      class="rag-answer-v2__lead"
                      :class="{
                        'rag-answer-v2__lead--compact':
                          openingHighlightLine(message.structured) && !isStreamingSummary(message)
                      }"
                    >
                      <TypewriterText
                        :text="message.structured.answerSummary"
                        :active="isStreamingSummary(message)"
                        @complete="onSummaryStreamComplete"
                        @progress="scrollToBottom"
                      />
                    </p>
                    <div
                      v-if="showSeparateConclusion(message.structured)"
                      class="rag-answer-v2__conclusion"
                    >
                      <p class="rag-answer-v2__conclusion-label">② 核心结论</p>
                      <p class="rag-answer-v2__conclusion-text">
                        <TypewriterText
                          :text="message.structured.conclusion"
                          :active="isStreamingConclusion(message)"
                          @complete="onConclusionStreamComplete"
                          @progress="scrollToBottom"
                        />
                      </p>
                    </div>
                  </div>
                </div>

                <!-- KPI 与业务快照合并为一块「信号面板」，避免上下两段样式割裂；网格固定 3 列避免宽屏 4+2 留白 -->
                <div class="rag-dash__signals-panel">
                  <div class="rag-dash__signals-panel-kpis">
                    <div class="rag-dash__evidence rag-dash__evidence--panel">
                      <div class="rag-dash__evidence-item">
                        <span class="rag-dash__evidence-n">{{ ragEvidenceCounts(message.structured).biz }}</span>
                        <span class="rag-dash__evidence-t">业务信号</span>
                      </div>
                      <div class="rag-dash__evidence-item">
                        <span class="rag-dash__evidence-n">{{ ragEvidenceCounts(message.structured).kb }}</span>
                        <span class="rag-dash__evidence-t">知识引用</span>
                      </div>
                      <div class="rag-dash__evidence-item">
                        <span class="rag-dash__evidence-n">{{ ragEvidenceCounts(message.structured).kg }}</span>
                        <span class="rag-dash__evidence-t">图谱实体</span>
                      </div>
                    </div>
                  </div>

                  <div
                    v-if="message.structured.businessCitation?.summaryBlocks?.length"
                    class="rag-dash__biz-snapshot rag-dash__biz-snapshot--panel"
                  >
                    <h4 class="rag-dash__section-title rag-dash__section-title--tight">
                      <span class="rag-dash__section-icon">◇</span>
                      业务快照
                      <span class="rag-dash__biz-snapshot-hint">来自当前业务上下文</span>
                    </h4>
                    <div class="rag-dash__biz-grid">
                      <div
                        v-for="(block, bidx) in message.structured.businessCitation.summaryBlocks.slice(0, 6)"
                        :key="`snap-${bidx}`"
                        class="rag-dash__biz-card"
                      >
                        {{ block }}
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="message.structured.recommendations?.length" class="rag-dash__timeline-wrap">
                  <h4 class="rag-dash__section-title rag-dash__section-title--accent">
                    <span class="rag-dash__section-icon">▶</span>
                    建议执行路径
                  </h4>
                  <ol class="rag-dash__timeline">
                    <li
                      v-for="(item, idx) in message.structured.recommendations"
                      :key="`r-${idx}`"
                      class="rag-dash__timeline-item"
                    >
                      <span class="rag-dash__timeline-idx">{{ idx + 1 }}</span>
                      <p class="rag-dash__timeline-body">{{ item }}</p>
                    </li>
                  </ol>
                </div>

                <div v-if="message.structured.possibleCauses?.length" class="rag-dash__causes">
                  <h4 class="rag-dash__section-title">
                    <el-icon class="rag-dash__section-ic" :size="16"><WarningFilled /></el-icon>
                    可能原因 · 排查树
                  </h4>
                  <ul class="rag-dash__cause-list">
                    <li v-for="(item, idx) in message.structured.possibleCauses" :key="`c-${idx}`">
                      {{ item }}
                    </li>
                  </ul>
                </div>

                <div v-if="message.structured.graphSources?.length" class="rag-answer-v2__graph-strip">
                  <div class="rag-answer-v2__graph-strip-head">
                    <el-tag type="success" effect="dark" size="small">知识图谱已联动</el-tag>
                    <span class="rag-answer-v2__graph-strip-desc">
                      以下实体已检索并参与本次回答；点击可在「农业知识网络」中以该实体为中心展开子图。
                    </span>
                  </div>
                  <div class="rag-answer-v2__graph-chips">
                    <button
                      v-for="(item, idx) in message.structured.graphSources"
                      :key="`gchip-${idx}-${item.id}`"
                      type="button"
                      class="rag-graph-chip"
                      @click="goKnowledgeGraph(item.id)"
                    >
                      <span class="rag-graph-chip__name">{{ item.title }}</span>
                      <span class="rag-graph-chip__meta">{{ item.kind || '实体' }} · {{ item.id }}</span>
                    </button>
                  </div>
                </div>

                <el-collapse class="rag-answer-v2__collapse" accordion>
                  <el-collapse-item
                    v-if="
                      message.structured.businessCitation?.summaryBlocks?.length ||
                      message.structured.businessCitation?.riskSignals?.length
                    "
                    title="业务数据明细（预警 / 设备 / 农田等）"
                    name="biz"
                  >
                    <div class="rag-answer-v2__collapse-body">
                      <div class="rag-answer-v2__chips">
                        <span
                          v-for="item in message.structured.businessCitation?.summaryBlocks || []"
                          :key="item"
                          class="rag-answer-v2__chip"
                        >
                          {{ item }}
                        </span>
                      </div>
                      <ul
                        v-if="message.structured.businessCitation?.riskSignals?.length"
                        class="rag-answer-v2__risk-list"
                      >
                        <li
                          v-for="item in message.structured.businessCitation.riskSignals"
                          :key="item"
                        >
                          {{ item }}
                        </li>
                      </ul>
                    </div>
                  </el-collapse-item>

                  <el-collapse-item
                    v-if="message.structured.knowledgeSources?.length"
                    :title="`知识库参考（已去重，共 ${message.structured.knowledgeSources.length} 条）`"
                    name="kb"
                  >
                    <p class="rag-answer-v2__kb-hint">
                      以下为向量检索命中的文档片段，<strong>不一定与当前问题强相关</strong>；请优先以「先读这一段」与「建议怎么做」为准。
                    </p>
                    <ul class="rag-answer-v2__kb-list rag-dash__kb-list">
                      <li
                        v-for="(item, idx) in message.structured.knowledgeSources"
                        :key="`k-${idx}-${item.id}-${item.title}`"
                        class="rag-dash__kb-row"
                      >
                        <div class="rag-dash__kb-main">
                          <span class="rag-answer-v2__kb-title">{{ item.title }}</span>
                          <span class="rag-answer-v2__kb-meta">{{ formatTopic(item.topic) }} · {{ item.source }}</span>
                        </div>
                        <div v-if="formatKnowledgeScore(item.score) != null" class="rag-dash__kb-score">
                          <el-progress
                            :percentage="formatKnowledgeScore(item.score)"
                            :stroke-width="5"
                            :show-text="false"
                            color="#1c9266"
                          />
                          <span class="rag-dash__kb-score-txt">相关度 {{ formatKnowledgeScore(item.score) }}%</span>
                        </div>
                      </li>
                    </ul>
                  </el-collapse-item>

                </el-collapse>

                <footer class="rag-dash__footer">
                  <span class="rag-dash__footer-brand">智慧农业 · 多源融合分析</span>
                  <span class="rag-dash__footer-disclaimer">输出仅供辅助决策，现场处置请以规程、合同与法规为准。</span>
                </footer>

                <p v-if="message.structured.fallbackReason" class="rag-answer-v2__fallback">
                  {{ message.structured.fallbackReason }}
                </p>
              </section>
            </div>
          </article>
        </div>
      </div>

      <footer class="ai-rag-composer">
        <div class="ai-rag-composer__top">
          <el-radio-group v-model="currentMode" size="default">
            <el-radio-button
              v-for="item in modeOptions"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
          <span class="composer-context">{{ contextDescription }}</span>
        </div>

        <div class="quick-question-row">
          <button
            v-for="item in sampleQuestions"
            :key="`${item.key}-composer`"
            type="button"
            class="quick-question-btn"
            @click="fillSampleQuestion(item)"
          >
            {{ item.shortLabel }}
          </button>
        </div>

        <el-input
          v-model="inputText"
          type="textarea"
          :rows="6"
          resize="none"
          maxlength="1200"
          show-word-limit
          placeholder="请输入你的问题，例如：这条设备离线预警的优先处理顺序是什么？"
          @keydown.enter.exact.prevent="handleSend"
        />

        <div class="ai-rag-composer__actions">
          <el-button plain @click="handleClear">清空会话</el-button>
          <el-button type="primary" :loading="sending" @click="handleSend">发送问题</el-button>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { CopyDocument, DataAnalysis, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { sendAiRagChatApi } from '@/api/ai'
import { getWarningPageApi } from '@/api/warning'
import { useAiAssistantStore } from '@/stores/aiAssistant'
import TypewriterText from '@/components/common/TypewriterText.vue'

const modeOptions = [
  { value: 'auto', label: '自动模式' },
  { value: 'business', label: '业务问答' },
  { value: 'knowledge', label: '知识问答' },
  { value: 'hybrid', label: '混合问答' }
]

const modeLabelMap = {
  auto: '自动模式',
  business: '业务问答',
  knowledge: '知识问答',
  hybrid: '混合问答'
}

const modeCards = [
  { key: 'business', title: '业务数据 RAG', desc: '读取预警、环境、设备与处理记录上下文。' },
  { key: 'knowledge', title: '知识库 RAG', desc: '检索农业知识、SOP 与设备说明文档。' },
  { key: 'hybrid', title: '混合问答', desc: '同时整合业务数据与知识片段生成结构化结论。' }
]

const sampleQuestions = [
  {
    key: 's1',
    label: '当前最高风险预警是什么？',
    shortLabel: '最高风险预警',
    mode: 'business'
  },
  {
    key: 's2',
    label: '这条设备离线预警应该怎么处理？',
    shortLabel: '设备离线处理',
    mode: 'business'
  },
  {
    key: 's3',
    label: '土壤湿度长期偏低有什么农业风险？',
    shortLabel: '土壤湿度风险',
    mode: 'knowledge'
  },
  {
    key: 's4',
    label: '帮我总结今天的异常情况',
    shortLabel: '今日异常总结',
    mode: 'hybrid'
  }
]

const router = useRouter()
const aiAssistantStore = useAiAssistantStore()
const { messages, currentMode } = storeToRefs(aiAssistantStore)

function goKnowledgeGraph(entityId) {
  const id = String(entityId || '').trim()
  if (!id) return
  router.push({ path: '/knowledge-graph', query: { centerId: id } })
}

const messageContainerRef = ref(null)
const sending = ref(false)
const inputText = ref('')
const latestWarning = ref(null)

/** 刚返回的助手消息逐字显示；历史消息 active=false 直接展示全文 */
const streamingMsgId = ref(null)
const streamPhase = ref('idle')

const contextDescription = computed(() => {
  if (currentMode.value === 'knowledge') {
    return '当前仅检索农业知识库'
  }
  if (latestWarning.value?.warningNo) {
    return `默认联动预警 ${latestWarning.value.warningNo}`
  }
  return '当前无默认预警上下文'
})

function priorityLabel(priority) {
  if (priority === '高' || priority === 'HIGH') return '高优先级'
  if (priority === '低' || priority === 'LOW') return '低优先级'
  return '中优先级'
}

function priorityTagType(priority) {
  if (priority === '高' || priority === 'HIGH') return 'danger'
  if (priority === '低' || priority === 'LOW') return 'success'
  return 'warning'
}

function priorityShellClass(priority) {
  if (priority === '高' || priority === 'HIGH') return 'rag-answer-v2--pri-high'
  if (priority === '低' || priority === 'LOW') return 'rag-answer-v2--pri-low'
  return 'rag-answer-v2--pri-mid'
}

function priorityUrgencyPercent(priority) {
  if (priority === '高' || priority === 'HIGH') return 90
  if (priority === '低' || priority === 'LOW') return 26
  return 55
}

function priorityBarColor(priority) {
  if (priority === '高' || priority === 'HIGH') return '#f56c6c'
  if (priority === '低' || priority === 'LOW') return '#67c23a'
  return '#e6a23c'
}

function ragEvidenceCounts(structured) {
  const bizBlocks = structured?.businessCitation?.summaryBlocks?.length ?? 0
  const bizRisk = structured?.businessCitation?.riskSignals?.length ?? 0
  return {
    biz: bizBlocks + bizRisk,
    kb: structured?.knowledgeSources?.length ?? 0,
    kg: structured?.graphSources?.length ?? 0
  }
}

function formatKnowledgeScore(score) {
  if (score == null || score === '') return null
  const n = Number(score)
  if (Number.isNaN(n)) return null
  return n <= 1 ? Math.round(n * 100) : Math.round(Math.min(n, 100))
}

async function copyRagDigest(structured) {
  const parts = []
  parts.push('—— 智慧农业 · AI 分析简报 ——', '')
  parts.push(structured.title || '', '')
  parts.push('【摘要】', structured.answerSummary || '', '')
  if (showSeparateConclusion(structured)) {
    parts.push('【核心结论】', structured.conclusion, '')
  }
  if (structured.recommendations?.length) {
    parts.push('【建议步骤】')
    structured.recommendations.forEach((t, i) => parts.push(`${i + 1}. ${t}`))
    parts.push('')
  }
  if (structured.possibleCauses?.length) {
    parts.push('【可能原因】')
    structured.possibleCauses.forEach((t) => parts.push(`· ${t}`))
  }
  try {
    await navigator.clipboard.writeText(parts.join('\n'))
    ElMessage.success('已复制简报要点')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本')
  }
}

function formatTopic(topic) {
  if (!topic) return '知识片段'
  const topicMap = {
    warning: '预警知识',
    irrigation: '灌溉知识',
    humidity: '湿度知识',
    device: '设备文档',
    crop: '作物知识'
  }
  return topicMap[topic] || topic
}

function appendMessage(role, content, structured = null) {
  const id = aiAssistantStore.appendMessage(role, content, structured)
  scrollToBottom()
  return id
}

function isStreamingSummary(message) {
  return (
    message.role === 'assistant' &&
    message.id === streamingMsgId.value &&
    streamPhase.value === 'summary'
  )
}

function isStreamingConclusion(message) {
  return (
    message.role === 'assistant' &&
    message.id === streamingMsgId.value &&
    streamPhase.value === 'conclusion'
  )
}

function isPlainStreaming(message) {
  return (
    message.role === 'assistant' &&
    message.id === streamingMsgId.value &&
    streamPhase.value === 'plain'
  )
}

function onSummaryStreamComplete() {
  if (streamPhase.value !== 'summary') return
  const mid = streamingMsgId.value
  const m = messages.value.find((x) => x.id === mid)
  if (m?.structured && showSeparateConclusion(m.structured)) {
    streamPhase.value = 'conclusion'
  } else {
    streamingMsgId.value = null
    streamPhase.value = 'idle'
  }
  scrollToBottom()
}

function onConclusionStreamComplete() {
  if (streamPhase.value !== 'conclusion') return
  streamingMsgId.value = null
  streamPhase.value = 'idle'
  scrollToBottom()
}

function onPlainStreamComplete() {
  if (streamPhase.value !== 'plain') return
  streamingMsgId.value = null
  streamPhase.value = 'idle'
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    if (messageContainerRef.value) {
      messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight
    }
  })
}

function dedupeByKey(list, keyFn) {
  if (!Array.isArray(list)) return []
  const seen = new Set()
  const out = []
  for (const item of list) {
    const k = keyFn(item)
    if (seen.has(k)) continue
    seen.add(k)
    out.push(item)
  }
  return out
}

/** 摘要与结论高度重复时只展示摘要，避免用户读两遍 */
function conclusionMergedIntoSummary(summary, conclusion) {
  const s = (summary || '').trim().replace(/\s+/g, ' ')
  const c = (conclusion || '').trim().replace(/\s+/g, ' ')
  if (!c) return true
  if (s === c) return true
  if (s.length >= 12 && c.length >= 12 && (s.includes(c) || c.includes(s))) return true
  return false
}

/** 兼容 localStorage 里旧结构无 conclusionMergedIntoSummary 字段 */
function showSeparateConclusion(structured) {
  if (!structured?.conclusion) return false
  if (structured.conclusionMergedIntoSummary === true) return false
  if (structured.conclusionMergedIntoSummary === false) return true
  return !conclusionMergedIntoSummary(structured.answerSummary, structured.conclusion)
}

/** 摘要较长时提取首句「划重点」，置顶显示但不改变正文顺序与全文 */
function openingHighlightLine(structured) {
  const full = (structured?.answerSummary || '').trim().replace(/\s+/g, ' ')
  if (full.length < 72) return ''
  let end = -1
  for (const ch of '。；！？') {
    const i = full.indexOf(ch)
    if (i >= 20 && (end < 0 || i < end)) end = i
  }
  if (end < 20) return ''
  const first = full.slice(0, end + 1).trim()
  if (first.length >= full.length * 0.92) return ''
  return first
}

function normalizeStructuredResponse(response) {
  const answerSummary = response.answerSummary || '已完成结构化分析'
  const conclusion = response.conclusion || ''
  const knowledgeSources = dedupeByKey(
    Array.isArray(response.citedKnowledgeSources) ? response.citedKnowledgeSources : [],
    (i) => `${i?.id ?? ''}|${i?.title ?? ''}|${i?.source ?? ''}`
  )
  const graphSources = dedupeByKey(
    Array.isArray(response.citedKnowledgeGraph) ? response.citedKnowledgeGraph : [],
    (i) => `${i?.id ?? ''}|${i?.title ?? ''}`
  )

  return {
    title: `${modeLabelMap[currentMode.value]}结果`,
    answerSummary,
    conclusion,
    conclusionMergedIntoSummary: conclusionMergedIntoSummary(answerSummary, conclusion),
    possibleCauses: Array.isArray(response.possibleCauses) ? response.possibleCauses : [],
    recommendations: Array.isArray(response.recommendations) ? response.recommendations : [],
    priority: response.priority || '中',
    shouldManualReview: Boolean(response.shouldManualReview),
    fallback: Boolean(response.fallback),
    fallbackReason: response.fallbackReason || '',
    businessCitation: response.citedBusinessContext || null,
    knowledgeSources,
    graphSources
  }
}

async function fetchLatestWarning() {
  try {
    const highRiskRes = await getWarningPageApi({ current: 1, size: 1, level: 3 })
    if (highRiskRes.records?.length) {
      latestWarning.value = highRiskRes.records[0]
      return
    }

    const latestRes = await getWarningPageApi({ current: 1, size: 1 })
    latestWarning.value = latestRes.records?.[0] || null
  } catch (error) {
    latestWarning.value = null
  }
}

async function sendQuestion(question, mode = currentMode.value) {
  streamingMsgId.value = null
  streamPhase.value = 'idle'
  appendMessage('user', question)
  sending.value = true

  try {
    const useBusinessContext = mode !== 'knowledge'
    const response = await sendAiRagChatApi({
      query: question,
      mode,
      relatedWarningId: useBusinessContext ? latestWarning.value?.id : undefined,
      relatedDeviceId: useBusinessContext ? latestWarning.value?.relatedDeviceId : undefined,
      relatedFarmlandId: useBusinessContext ? latestWarning.value?.farmlandId : undefined
    })

    const assistantId = appendMessage('assistant', '', normalizeStructuredResponse(response))
    streamingMsgId.value = assistantId
    streamPhase.value = 'summary'
  } catch (error) {
    const detail =
      (typeof error?.message === 'string' && error.message) ||
      error?.response?.data?.message ||
      error?.msg ||
      ''
    const assistantId = appendMessage(
      'assistant',
      detail
        ? `${detail}\n\n提示：请确认 rag-service 已在默认端口 9001 运行，且 Spring 的 smart-agri.rag.base-url 指向该地址；未配大模型 Key 时 rag-service 会自动使用本地 mock。`
        : '本次 RAG 问答调用失败，请稍后重试。'
    )
    streamingMsgId.value = assistantId
    streamPhase.value = 'plain'
  } finally {
    sending.value = false
  }
}

function fillSampleQuestion(item) {
  currentMode.value = item.mode
  inputText.value = item.label
}

function handleSampleQuestion(item) {
  currentMode.value = item.mode
  sendQuestion(item.label, item.mode)
}

async function handleSend() {
  const question = inputText.value.trim()
  if (!question) {
    ElMessage.warning('请输入问题')
    return
  }
  inputText.value = ''
  await sendQuestion(question, currentMode.value)
}

function handleClear() {
  aiAssistantStore.clearSession()
}

onMounted(async () => {
  await fetchLatestWarning()
  if (messages.value.length) {
    scrollToBottom()
  }
})
</script>

<style scoped lang="scss">
.ai-rag-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}

.ai-rag-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  padding: 12px 16px;
}

.ai-rag-header__main h1 {
  margin: 0;
  font-size: 24px;
  color: var(--app-text);
}

.ai-rag-header__main p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--app-text-secondary);
}

.ai-rag-header__graph-hint {
  margin: 8px 0 0;
  max-width: 920px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--app-text-secondary);
}

.ai-rag-header__graph-hint strong {
  color: var(--app-text);
  font-weight: 600;
}

.ai-rag-header__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.ai-rag-workspace {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}

.ai-rag-dialogue {
  flex: 1 1 auto;
  min-height: 0;
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(246, 252, 248, 0.96), rgba(255, 255, 255, 0.98)),
    #fff;
  padding: 18px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.welcome-panel {
  min-height: 100%;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.welcome-panel__icon {
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(28, 146, 102, 0.16), rgba(76, 139, 245, 0.14));
  color: var(--app-primary);
  font-size: 22px;
  font-weight: 700;
}

.welcome-panel h3 {
  margin: 0;
  font-size: 26px;
  color: var(--app-text);
}

.welcome-panel p {
  margin: 0;
  max-width: 680px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text-secondary);
}

.welcome-mode-cards {
  width: min(860px, 100%);
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.welcome-mode-card {
  display: grid;
  gap: 6px;
  text-align: left;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  padding: 14px;
}

.welcome-mode-card strong {
  font-size: 15px;
  color: var(--app-text);
}

.welcome-mode-card span {
  font-size: 13px;
  line-height: 1.6;
  color: var(--app-text-secondary);
}

.welcome-samples {
  width: min(860px, 100%);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.welcome-sample-btn {
  border: 1px solid rgba(17, 82, 62, 0.12);
  border-radius: 12px;
  background: #fff;
  color: var(--app-text);
  font-size: 14px;
  text-align: left;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.welcome-sample-btn:hover {
  border-color: rgba(28, 146, 102, 0.24);
  background: #f5fbf7;
  transform: translateY(-1px);
}

.chat-list {
  display: grid;
  gap: 14px;
}

.chat-item {
  display: grid;
  gap: 8px;
}

.chat-item--user {
  justify-items: end;
}

.chat-item__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.chat-item--user .chat-item__meta {
  justify-content: flex-end;
}

.chat-item__bubble {
  width: min(920px, 100%);
  display: grid;
  gap: 10px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 16px;
  background: #fff;
  padding: 14px;
}

.chat-item--assistant .chat-item__bubble,
.chat-item:not(.chat-item--user) .chat-item__bubble {
  width: min(980px, 100%);
  padding: 16px 18px;
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.98), rgba(248, 252, 250, 0.99));
  box-shadow: 0 8px 28px rgba(15, 52, 40, 0.06);
}

.chat-item--user .chat-item__bubble {
  width: min(720px, 100%);
  background: rgba(242, 248, 255, 0.98);
  border-color: rgba(76, 139, 245, 0.18);
}

.chat-item__text {
  margin: 0;
  line-height: 1.75;
  color: var(--app-text);
}

/* ---------- 结构化回答：分析简报 / 仪表盘布局 ---------- */
.rag-answer-v2.rag-answer-dash {
  position: relative;
  padding-left: 4px;
  border-radius: 4px;
}

.rag-answer-v2--pri-high {
  border-left: 4px solid #f56c6c;
}

.rag-answer-v2--pri-mid {
  border-left: 4px solid #e6a23c;
}

.rag-answer-v2--pri-low {
  border-left: 4px solid #67c23a;
}

.rag-dash__masthead {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  margin: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(22, 101, 72, 0.06), rgba(76, 139, 245, 0.05));
  border: 1px solid rgba(28, 146, 102, 0.1);
}

.rag-dash__masthead--compact {
  padding: 8px 12px;
}

.rag-dash__masthead-left {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
  flex: 1;
}

.rag-dash__logo {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #1c9266, #157a55);
  color: #f0fff8;
  box-shadow: 0 6px 16px rgba(28, 146, 102, 0.35);
}

.rag-dash__logo--sm {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgba(28, 146, 102, 0.28);
}

.rag-dash__masthead-text {
  min-width: 0;
}

.rag-dash__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
  margin-bottom: 4px;
}

.rag-dash__time {
  font-size: 12px;
  color: var(--app-text-secondary);
  font-weight: 500;
}

.rag-dash__masthead-inline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.rag-dash__urgency-inline {
  font-size: 11px;
  color: var(--app-text-secondary);
  margin-left: 4px;
}

.rag-dash__urgency-inline strong {
  margin-left: 4px;
  font-size: 12px;
}

.rag-dash__badge {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #fff;
  background: linear-gradient(90deg, #1c9266, #2aab7a);
  padding: 2px 8px;
  border-radius: 999px;
}

.rag-dash__mode {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-text);
}

.rag-dash__focal {
  margin-top: 2px;
  padding: 14px 16px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 252, 249, 0.96));
  border: 1px solid rgba(28, 146, 102, 0.18);
  box-shadow: 0 4px 20px rgba(15, 52, 40, 0.06);
}

.rag-dash__focal-pull {
  margin: 0 0 14px;
  padding: 12px 14px;
  border-radius: 12px;
  border-left: 4px solid #1c9266;
  background: rgba(28, 146, 102, 0.07);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.55;
  color: #0d3d2c;
}

.rag-dash__hero--inset {
  margin: 0;
  padding: 0;
  border: none;
  background: transparent;
  box-shadow: none;
}

.rag-answer-v2__lead--compact {
  font-size: 14px;
  line-height: 1.75;
  color: var(--app-text);
  font-weight: 400;
}

.rag-dash__evidence {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.rag-dash__signals-panel {
  margin-top: 6px;
  border-radius: 14px;
  border: 1px solid rgba(28, 146, 102, 0.14);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(246, 252, 249, 0.88));
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(15, 52, 40, 0.04);
}

.rag-dash__signals-panel-kpis {
  padding: 8px 10px;
}

.rag-dash__evidence--panel .rag-dash__evidence-item {
  padding: 8px 6px;
}

.rag-dash__evidence--panel .rag-dash__evidence-n {
  font-size: 18px;
}

.rag-dash__evidence-item {
  text-align: center;
  padding: 10px 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(17, 82, 62, 0.08);
}

.rag-dash__evidence-n {
  display: block;
  font-size: 22px;
  font-weight: 800;
  color: #1c9266;
  line-height: 1.2;
}

.rag-dash__evidence-t {
  font-size: 11px;
  color: var(--app-text-secondary);
  margin-top: 2px;
  display: block;
}

.rag-dash__hero .rag-dash__hero-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.rag-dash__hero .rag-answer-v2__hero-label {
  margin-bottom: 0;
}

.rag-dash__btn-ic {
  margin-right: 4px;
  vertical-align: middle;
}

.rag-dash__section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text);
}

.rag-dash__section-title--accent {
  color: #157a55;
}

.rag-dash__section-title--tight {
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.rag-dash__biz-snapshot-hint {
  margin-left: auto;
  font-size: 11px;
  font-weight: 500;
  color: var(--app-text-secondary);
  opacity: 0.85;
}

.rag-dash__section-icon {
  font-size: 10px;
  opacity: 0.65;
}

.rag-dash__section-ic {
  color: #e6a23c;
}

.rag-dash__biz-snapshot {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px dashed rgba(28, 146, 102, 0.22);
}

.rag-dash__biz-snapshot--panel {
  margin: 0;
  border: none;
  border-radius: 0;
  border-top: 1px solid rgba(28, 146, 102, 0.12);
  background: rgba(28, 146, 102, 0.04);
  padding: 10px 12px 12px;
}

.rag-dash__biz-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

@media (max-width: 900px) {
  .rag-dash__biz-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .rag-dash__biz-grid {
    grid-template-columns: 1fr;
  }
}

.rag-dash__biz-card {
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-text);
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(28, 146, 102, 0.06);
  border: 1px solid rgba(28, 146, 102, 0.1);
}

.rag-dash__timeline-wrap {
  padding: 4px 0;
}

.rag-dash__timeline {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.rag-dash__timeline-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid rgba(17, 82, 62, 0.06);
}

.rag-dash__timeline-item:last-child {
  border-bottom: none;
}

.rag-dash__timeline-idx {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(145deg, #1c9266, #29a770);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: grid;
  place-items: center;
  box-shadow: 0 2px 8px rgba(28, 146, 102, 0.3);
}

.rag-dash__timeline-body {
  margin: 0;
  flex: 1;
  font-size: 14px;
  line-height: 1.7;
  color: var(--app-text);
  padding-top: 2px;
}

.rag-dash__causes {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(230, 162, 60, 0.06);
  border: 1px solid rgba(230, 162, 60, 0.2);
}

.rag-dash__cause-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.rag-dash__cause-list li {
  position: relative;
  padding: 8px 0 8px 18px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--app-text);
  border-bottom: 1px solid rgba(230, 162, 60, 0.12);
}

.rag-dash__cause-list li:last-child {
  border-bottom: none;
}

.rag-dash__cause-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e6a23c;
  opacity: 0.85;
}

.rag-dash__kb-row {
  flex-direction: row !important;
  align-items: flex-start !important;
  justify-content: space-between;
  gap: 12px !important;
}

.rag-dash__kb-main {
  flex: 1;
  min-width: 0;
}

.rag-dash__kb-score {
  flex-shrink: 0;
  width: 120px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
}

.rag-dash__kb-score-txt {
  font-size: 10px;
  color: var(--app-text-secondary);
}

.rag-dash__footer {
  margin-top: 6px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(17, 82, 62, 0.04);
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  justify-content: space-between;
  font-size: 11px;
  color: var(--app-text-secondary);
}

.rag-dash__footer-brand {
  font-weight: 600;
  color: rgba(22, 101, 72, 0.85);
}

.rag-dash__footer-disclaimer {
  max-width: 520px;
  line-height: 1.45;
}

.rag-answer-v2 {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rag-answer-v2__hero {
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(145deg, rgba(28, 146, 102, 0.09), rgba(76, 139, 245, 0.06));
  border: 1px solid rgba(28, 146, 102, 0.14);
}

.rag-answer-v2__hero-label {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(22, 101, 72, 0.85);
  text-transform: uppercase;
}

.rag-answer-v2__lead {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: var(--app-text);
  font-weight: 500;
}

.rag-answer-v2__conclusion {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(17, 82, 62, 0.1);
}

.rag-answer-v2__conclusion-label {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-secondary);
}

.rag-answer-v2__conclusion-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: var(--app-text);
}

.rag-answer-v2__block {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(251, 254, 252, 0.95);
  border: 1px solid rgba(17, 82, 62, 0.08);
}

.rag-answer-v2__block--primary {
  border-color: rgba(28, 146, 102, 0.22);
  background: rgba(28, 146, 102, 0.04);
}

.rag-answer-v2__block-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--app-text);
}

.rag-answer-v2__steps {
  margin: 0;
  padding-left: 1.2em;
  color: var(--app-text);
  line-height: 1.75;
  font-size: 14px;
}

.rag-answer-v2__steps li {
  margin-bottom: 6px;
}

.rag-answer-v2__steps li:last-child {
  margin-bottom: 0;
}

.rag-answer-v2__bullets {
  margin: 0;
  padding-left: 1.2em;
  color: var(--app-text);
  line-height: 1.7;
  font-size: 13px;
}

.rag-answer-v2__bullets li {
  margin-bottom: 4px;
}

.rag-answer-v2__collapse {
  border: none;
  --el-collapse-border-color: transparent;
}

.rag-answer-v2__collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
  background: rgba(248, 250, 249, 0.9);
  border-radius: 10px;
  padding: 0 12px;
  height: 42px;
  border: 1px solid rgba(17, 82, 62, 0.08);
}

.rag-answer-v2__collapse :deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

.rag-answer-v2__collapse :deep(.el-collapse-item__content) {
  padding: 10px 4px 4px;
}

.rag-answer-v2__collapse-body {
  padding: 4px 0 2px;
}

.rag-answer-v2__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rag-answer-v2__chip {
  border-radius: 999px;
  background: rgba(28, 146, 102, 0.1);
  color: var(--app-text);
  padding: 6px 10px;
  font-size: 12px;
  line-height: 1.4;
}

.rag-answer-v2__risk-list {
  margin: 10px 0 0;
  padding-left: 1.2em;
  font-size: 13px;
  line-height: 1.65;
  color: var(--app-text);
}

.rag-answer-v2__kb-hint {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-secondary);
}

.rag-answer-v2__kb-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.rag-answer-v2__kb-list li {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(17, 82, 62, 0.06);
  font-size: 13px;
}

.rag-answer-v2__kb-list li:last-child {
  border-bottom: none;
}

.rag-answer-v2__kb-title {
  color: var(--app-text);
  line-height: 1.5;
}

.rag-answer-v2__kb-meta {
  font-size: 11px;
  color: var(--app-text-secondary);
}

.rag-answer-v2__fallback {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #8a5a00;
}

.rag-answer-v2__graph-strip {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid rgba(28, 146, 102, 0.28);
  background: rgba(28, 146, 102, 0.06);
}

.rag-answer-v2__graph-strip-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.rag-answer-v2__graph-strip-desc {
  font-size: 12px;
  line-height: 1.5;
  color: var(--app-text-secondary);
  flex: 1;
  min-width: 200px;
}

.rag-answer-v2__graph-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rag-graph-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 12px;
  border-radius: 10px;
  border: 1px solid rgba(28, 146, 102, 0.35);
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.rag-graph-chip:hover {
  border-color: rgba(28, 146, 102, 0.65);
  box-shadow: 0 2px 8px rgba(28, 146, 102, 0.12);
}

.rag-graph-chip__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}

.rag-graph-chip__meta {
  font-size: 11px;
  color: var(--app-text-secondary);
}

.ai-rag-composer {
  flex-shrink: 0;
  display: grid;
  gap: 12px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  padding: 14px;
}

.ai-rag-composer__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.composer-context {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.quick-question-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-question-btn {
  border: 1px solid rgba(17, 82, 62, 0.12);
  border-radius: 999px;
  background: #f7fbf8;
  color: var(--app-text);
  font-size: 12px;
  padding: 7px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-question-btn:hover {
  border-color: rgba(28, 146, 102, 0.24);
  background: #eef8f2;
}

.ai-rag-composer :deep(.el-textarea__inner) {
  min-height: 148px !important;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
}

.ai-rag-composer__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1080px) {
  .welcome-mode-cards,
  .welcome-samples {
    grid-template-columns: 1fr;
  }

  .ai-rag-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 1024px) {
  .ai-rag-page {
    flex: none;
    min-height: 0;
    overflow: visible;
  }

  .ai-rag-workspace {
    flex: none;
    min-height: 0;
    overflow: visible;
  }

  .ai-rag-dialogue {
    flex: none;
    max-height: min(72vh, 640px);
    min-height: 280px;
  }
}

@media (max-width: 768px) {
  .ai-rag-dialogue {
    max-height: min(65vh, 560px);
    min-height: 240px;
    padding: 14px;
  }

  .chat-item__bubble,
  .chat-item--user .chat-item__bubble {
    width: 100%;
  }

  .ai-rag-composer__top {
    align-items: flex-start;
  }

  .rag-dash__evidence {
    grid-template-columns: 1fr;
  }

  .rag-dash__masthead-right {
    width: 100%;
  }

  .rag-dash__kb-score {
    width: 100%;
    align-items: stretch;
  }
}
</style>
