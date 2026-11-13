<template>
  <div class="page-shell ai-rag-page">
    <section class="ai-rag-header">
      <div class="ai-rag-header__main">
        <span class="page-banner__badge">RAG 联合问答</span>
        <p>业务数据 + 知识库 + 知识图谱联合问答，回答下方会标注来源与可信度。</p>
      </div>
      <div class="ai-rag-header__meta">
        <el-button type="primary" plain :icon="Upload" @click="uploadDialogVisible = true">上传资料</el-button>
        <el-tag type="success" effect="plain">AI 在线</el-tag>
        <el-tag effect="plain">{{ modeLabelMap[currentMode] }}</el-tag>
        <el-tag v-if="latestWarning?.warningNo" type="warning" effect="plain">
          联动预警 {{ latestWarning.warningNo }}
        </el-tag>
      </div>
    </section>

    <el-dialog
      v-model="uploadDialogVisible"
      title="知识库管理"
      width="760px"
      @closed="resetUpload"
    >
      <el-form label-width="80px">
        <el-form-item label="资料类型">
          <el-select v-model="uploadCategory" style="width: 100%">
            <el-option label="作物种植 / 水肥管理" value="crop" />
            <el-option label="病虫害 / 技术资料" value="tech" />
            <el-option label="标准作业流程 SOP" value="sop" />
            <el-option label="设备说明" value="device" />
            <el-option label="通用农技基础" value="base" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            v-model:file-list="uploadFileList"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".md,.txt,.json,.pdf,.docx"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .md / .txt / .json / .pdf / .docx，单文件不超过 20MB；上传后自动切片并写入向量库
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <el-alert
        v-if="uploadResult"
        type="success"
        :closable="false"
        show-icon
        :title="`已入库：${uploadResult.file_name || ''}`"
        :description="`本次共索引 ${uploadResult.chunk_count ?? 0} 个切片，向量库现有 ${uploadResult.upserted_count ?? 0} 条，可以直接在下方提问验证`"
      />
      <el-divider content-position="left">已有文档（{{ knowledgeDocuments.length }}）</el-divider>
      <el-table
        v-loading="documentsLoading"
        :data="knowledgeDocuments"
        size="small"
        max-height="260"
        empty-text="知识库暂无文档"
      >
        <el-table-column prop="path" label="文档" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="类型" width="90" />
        <el-table-column label="大小" width="90">
          <template #default="{ row }">{{ formatFileSize(row.size_bytes) }}</template>
        </el-table-column>
        <el-table-column label="向量数" width="90">
          <template #default="{ row }">{{ row.vector_count }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeKnowledgeDocument(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="uploading"
          :disabled="!uploadFileList.length"
          @click="submitUpload"
        >
          开始上传
        </el-button>
      </template>
    </el-dialog>

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
                {{ stripMarkdown(message.content) }}
              </p>

              <section v-if="message.structured" class="rag-answer-min">
                <header class="rag-answer-min__head">
                  <span class="rag-answer-min__head-label">
                    <el-icon class="rag-answer-min__head-ic"><Reading /></el-icon>
                    回答正文
                  </span>
                  <el-button type="primary" link size="small" @click="copyRagDigest(message.structured)">
                    <el-icon class="rag-answer-min__ic"><CopyDocument /></el-icon>
                    复制全文
                  </el-button>
                </header>

                <div class="rag-answer-min__prose">
                  <p
                    v-for="(para, pIdx) in answerParagraphs(message.structured.answerSummary)"
                    :key="`${message.id}-ap-${pIdx}`"
                    class="rag-answer-min__para"
                  >
                    {{ para }}
                  </p>
                </div>

                <aside
                  v-if="showSeparateConclusion(message.structured)"
                  class="rag-answer-min__conclusion-card"
                >
                  <div class="rag-answer-min__conclusion-card__hd">要点小结</div>
                  <p class="rag-answer-min__conclusion-card__body">{{ stripMarkdown(message.structured.conclusion) }}</p>
                </aside>

                <div
                  v-if="message.structured"
                  class="rag-answer-min__meta-panel"
                >
                  <div class="rag-answer-min__meta-panel__title">
                    <el-icon><DataLine /></el-icon>
                    检索与可信度
                  </div>
                  <div class="rag-answer-min__meta-panel__tags">
                    <el-tag v-if="message.structured.shouldManualReview" type="warning" size="small" effect="dark">
                      建议人工复核
                    </el-tag>
                    <el-tag v-if="message.structured.fallbackReason" type="warning" size="small" effect="plain">
                      兜底模式
                    </el-tag>
                    <el-tag v-if="message.structured.priority" type="info" size="small" effect="plain">
                      优先级 {{ message.structured.priority }}
                    </el-tag>
                    <el-tag
                      :type="retrievalScoreTagType(firstKnowledgeScore(message))"
                      size="small"
                      effect="plain"
                      class="rag-answer-min__score-tag"
                    >
                      向量相似度 {{ formatRetrievalScore(firstKnowledgeScore(message)) }}
                    </el-tag>
                  </div>
                </div>

                <footer
                  v-if="resolvedSingleSource(message.structured)"
                  class="rag-answer-min__source-foot"
                >
                  <el-icon class="rag-answer-min__source-foot__ic"><Document /></el-icon>
                  <div class="rag-answer-min__source-foot__main">
                    <span class="rag-answer-min__source-foot__label">参考来源</span>
                    <span class="rag-answer-min__source-foot__text">{{
                      resolvedSingleSource(message.structured).display
                    }}</span>
                  </div>
                  <el-button
                    v-if="
                      resolvedSingleSource(message.structured).graphId &&
                      !message.structured.graphSources?.length
                    "
                    type="primary"
                    size="small"
                    plain
                    round
                    class="rag-answer-min__graph-pill"
                    @click="goKnowledgeGraph(resolvedSingleSource(message.structured).graphId)"
                  >
                    知识图谱
                  </el-button>
                </footer>

                <div v-if="message.structured.graphSources?.length" class="rag-answer-min__kg-strip">
                  <span class="rag-answer-min__source-label">图谱</span>
                  <span class="rag-answer-min__kg-strip__hint">本次注入模型的实体（可点击查看详情）</span>
                  <div class="rag-answer-min__kg-chips">
                    <el-button
                      v-for="g in message.structured.graphSources"
                      :key="`${message.id}-kg-${g.id}`"
                      type="success"
                      link
                      size="small"
                      @click="goKnowledgeGraph(g.id)"
                    >
                      {{ g.title || g.id }}{{ g.kind ? ` · ${g.kind}` : '' }}
                    </el-button>
                  </div>
                  <!-- 迷你知识图谱可视化 - 展示第一个命中的实体子图 -->
                  <MiniGraph
                    v-if="getPrimarySubgraph(message.structured)"
                    :subgraph="getPrimarySubgraph(message.structured)"
                  />
                </div>

                <!-- 检索详情下拉框 - 展示所有命中的知识块、重排分数等 -->
                <div v-if="getRetrievalDetails(message).length" class="rag-answer-min__retrieval-panel">
                  <el-collapse>
                    <el-collapse-item title="检索详情（点击展开）" name="retrieval">
                      <template #title>
                        <div class="retrieval-panel__title">
                          <el-icon><Document /></el-icon>
                          <span>检索详情</span>
                          <span class="retrieval-panel__count">共 {{ getRetrievalDetails(message).length }} 条命中</span>
                        </div>
                      </template>
                      <div class="retrieval-detail-list">
                        <div
                          v-for="(item, idx) in getRetrievalDetails(message)"
                          :key="`${message.id}-ref-${idx}`"
                          class="retrieval-detail-item"
                        >
                          <div class="retrieval-detail-item__header">
                            <span class="retrieval-detail-item__index">#{{ idx + 1 }}</span>
                            <span class="retrieval-detail-item__title">{{ item.title }}</span>
                            <el-tag :type="getScoreTagType(item.score)" size="small" effect="plain">
                              向量相似度 {{ item.score?.toFixed(4) || '--' }}
                            </el-tag>
                            <el-tag v-if="item.rerankScore != null" type="warning" size="small" effect="plain">
                              重排分数 {{ item.rerankScore?.toFixed(4) }}
                            </el-tag>
                          </div>
                          <div class="retrieval-detail-item__source">
                            <span class="retrieval-detail-item__source-label">来源：</span>
                            <span class="retrieval-detail-item__source-value">{{ item.source }}</span>
                          </div>
                          <div class="retrieval-detail-item__content">
                            <span class="retrieval-detail-item__content-label">内容摘要：</span>
                            <span class="retrieval-detail-item__content-text">{{ truncateContent(item.content, 200) }}</span>
                          </div>
                          <div v-if="item.metadata?.topic || item.metadata?.domain" class="retrieval-detail-item__meta">
                            <el-tag v-if="item.metadata?.topic" size="small" type="info">{{ item.metadata.topic }}</el-tag>
                            <el-tag v-if="item.metadata?.domain" size="small" type="info">{{ item.metadata.domain }}</el-tag>
                          </div>
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>

                <p v-if="message.structured.fallbackReason" class="rag-answer-min__fallback">
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
        </div>

        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
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
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { CopyDocument, DataLine, Document, Reading, Upload, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteKnowledgeDocumentApi,
  fetchKnowledgeDocumentsApi,
  sendAiChatApi,
  sendAiRagChatApi,
  uploadKnowledgeApi
} from '@/api/ai'
import { getWarningPageApi } from '@/api/warning'
import { useAiAssistantStore } from '@/stores/aiAssistant'
import MiniGraph from '@/components/ai/MiniGraph.vue'

const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadCategory = ref('crop')
const uploadFileList = ref([])
const uploadResult = ref(null)

async function submitUpload() {
  const raw = uploadFileList.value[0]?.raw
  if (!raw) return
  uploading.value = true
  uploadResult.value = null
  try {
    uploadResult.value = await uploadKnowledgeApi(raw, uploadCategory.value)
    ElMessage.success('文档已入库，可以直接提问验证')
    await loadKnowledgeDocuments()
  } catch (error) {
    ElMessage.error(error?.message || '上传失败，请查看后端日志')
  } finally {
    uploading.value = false
  }
}

function resetUpload() {
  uploadFileList.value = []
  uploadResult.value = null
}

const knowledgeDocuments = ref([])
const documentsLoading = ref(false)

async function loadKnowledgeDocuments() {
  documentsLoading.value = true
  try {
    const result = await fetchKnowledgeDocumentsApi()
    knowledgeDocuments.value = result?.items || []
  } catch (error) {
    ElMessage.error(error?.message || '加载知识文档失败')
  } finally {
    documentsLoading.value = false
  }
}

async function removeKnowledgeDocument(row) {
  try {
    await ElMessageBox.confirm(
      `删除后「${row.path}」及其 ${row.vector_count ?? 0} 条向量会同步移除，且不可恢复。`,
      '确认删除文档',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    const result = await deleteKnowledgeDocumentApi(row.path)
    ElMessage.success(`已删除，同步清理 ${result?.deleted_points ?? 0} 条向量`)
    await loadKnowledgeDocuments()
  } catch (error) {
    ElMessage.error(error?.message || '删除失败')
  }
}

function formatFileSize(bytes) {
  const size = Number(bytes) || 0
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

watch(uploadDialogVisible, (visible) => {
  if (visible) loadKnowledgeDocuments()
})

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
  {
    key: 'knowledge',
    title: '知识库 RAG',
    desc: '检索文档片段；同时后端会按问题匹配 Neo4j 实体并注入上下文（与文档并行，非同一数据源）。'
  },
  { key: 'hybrid', title: '混合问答', desc: '业务数据 + 知识片段 + 图谱实体摘要一并参与作答。' }
]

const sampleQuestions = [
  {
    key: 's1',
    label: '当前最高风险预警是什么？',
    mode: 'business'
  },
  {
    key: 's2',
    label: '这条设备离线预警应该怎么处理？',
    mode: 'business'
  },
  {
    key: 's3',
    label: '土壤湿度长期偏低有什么农业风险？',
    mode: 'knowledge'
  },
  {
    key: 's4',
    label: '帮我总结今天的异常情况',
    mode: 'hybrid'
  },
  {
    key: 's5',
    label: '草莓栽培对土壤湿度有什么要求？',
    mode: 'knowledge'
  },
  {
    key: 's6',
    label: '番茄晚疫病和番茄早疫病有什么区别？',
    mode: 'knowledge'
  }
]

const GENERAL_CHAT_HINT_PATTERNS = [
  /^(你好|您好|嗨|哈喽|在吗|hello|hi)\s*[!！?？。.]?$/i,
  /(你是谁|你能做什么|介绍一下你自己|自我介绍)/i,
  /(今天天气|讲个笑话|闲聊|聊天|陪我聊聊|早上好|晚上好|午安)/i
]

const AGRI_DOMAIN_HINT_PATTERNS = [
  /(预警|农田|作物|设备|环境|土壤|湿度|温度|光照|病害|虫害|灌溉|施肥|巡检|闭环|图谱|知识库|RAG|Neo4j|Qdrant)/i
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

/** 仅保留一条来源：优先知识库首条，否则图谱首条（兼容旧会话无 singleSource） */
function resolvedSingleSource(structured) {
  if (!structured) return null
  if (structured.singleSource?.display) {
    return {
      display: structured.singleSource.display,
      graphId: structured.singleSource.graphId || null
    }
  }
  const k = structured.knowledgeSources?.[0]
  if (k) {
    const title = String(k.title || '').trim() || '知识片段'
    const src = String(k.source || '').trim()
    return { display: src ? `${title} · ${src}` : title, graphId: null }
  }
  const g = structured.graphSources?.[0]
  if (g) {
    const title = String(g.title || '').trim() || '知识图谱实体'
    const id = g.id != null && g.id !== '' ? String(g.id) : ''
    return { display: id ? `${title} · ${id}` : title, graphId: id || null }
  }
  return null
}

function buildSingleSourceFromRefs(knowledgeList, graphList) {
  const k = knowledgeList[0]
  if (k) {
    const title = String(k.title || '').trim() || '知识片段'
    const src = String(k.source || '').trim()
    return { display: src ? `${title} · ${src}` : title, graphId: null }
  }
  const g = graphList[0]
  if (g) {
    const title = String(g.title || '').trim() || '知识图谱实体'
    const id = g.id != null && g.id !== '' ? String(g.id) : ''
    return { display: id ? `${title} · ${id}` : title, graphId: id || null }
  }
  return null
}

async function copyRagDigest(structured) {
  const parts = []
  if (structured.answerSummary) parts.push(structured.answerSummary, '')
  if (showSeparateConclusion(structured) && structured.conclusion) {
    parts.push(structured.conclusion, '')
  }
  const src = resolvedSingleSource(structured)
  if (src?.display) parts.push(`来源：${src.display}`)
  if (structured.graphSources?.length) {
    parts.push(
      '',
      `图谱实体：${structured.graphSources.map((g) => g.title || g.id).join('、')}`
    )
  }
  try {
    await navigator.clipboard.writeText(parts.join('\n').trim())
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本')
  }
}

function appendMessage(role, content, structured = null) {
  const id = aiAssistantStore.appendMessage(role, content, structured)
  scrollToBottom()
  return id
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

/** 获取第一个有子图数据的图谱来源，用于迷你图谱展示 */
function getPrimarySubgraph(structured) {
  if (!structured?.graphSources?.length) return null
  const primary = structured.graphSources.find(g => g.subgraph?.nodes?.length > 0)
  return primary?.subgraph || null
}

function normalizeStructuredResponse(response, ragMode) {
  const answerSummary = response.answerSummary || '已完成结构化分析'
  const conclusion = response.conclusion || ''
  const knowledgeDeduped = dedupeByKey(
    Array.isArray(response.citedKnowledgeSources) ? response.citedKnowledgeSources : [],
    (i) => `${i?.id ?? ''}|${i?.title ?? ''}|${i?.source ?? ''}`
  )
  // 图谱去重时保留 subgraph 数据
  const graphDeduped = dedupeByKeyWithExtra(
    Array.isArray(response.citedKnowledgeGraph) ? response.citedKnowledgeGraph : [],
    (i) => `${i?.id ?? ''}|${i?.title ?? ''}`
  )
  const modeKey = ragMode || currentMode.value
  const singleSource = buildSingleSourceFromRefs(knowledgeDeduped, graphDeduped)

  return {
    title: `${modeLabelMap[modeKey] || '分析'}结果`,
    questionType: response.questionType || response.question_type || '',
    ragMode: modeKey,
    answerSummary,
    conclusion,
    conclusionMergedIntoSummary: conclusionMergedIntoSummary(answerSummary, conclusion),
    singleSource,
    knowledgeSources: knowledgeDeduped,
    graphSources: graphDeduped,
    fallback: Boolean(response.fallback),
    fallbackReason: response.fallbackReason || '',
    shouldManualReview: Boolean(response.shouldManualReview),
    priority: response.priority || '',
    // 保留原始数据用于检索详情展示
    citedKnowledgeSources: response.citedKnowledgeSources || [],
    retrievalMetadata: response.retrievalMetadata || null,
    // 保留完整的 references 列表（包含 content）
    references: response.references || [],
    // 保留业务上下文
    citedBusinessContext: response.citedBusinessContext || null,
  }
}

/** 去重并保留额外字段（如 subgraph） */
function dedupeByKeyWithExtra(list, keyFn) {
  if (!Array.isArray(list)) return []
  const seen = new Map()
  for (const item of list) {
    const k = keyFn(item)
    if (!seen.has(k)) {
      seen.set(k, item)
    }
  }
  return [...seen.values()]
}

/** 向量库返回的相似度分数（与问题 embedding 接近程度），非「事实正确率」 */
function firstKnowledgeScore(message) {
  // 支持传入 structured 对象或整个 message 对象
  const structured = message?.structured ?? message
  if (!structured) return null

  // 来源1：直接从 citedKnowledgeSources 取（API 原名字段，最优先）
  const apiKs = message?.citedKnowledgeSources
  if (Array.isArray(apiKs) && apiKs.length > 0) {
    const s = apiKs[0]?.score
    // 允许 0.0 值（表示检索到但相似度极低）
    if (s != null) {
      const n = Number(s)
      if (Number.isFinite(n)) return n
    }
  }

  // 来源2：从 knowledgeSources 取（转换后的结构化字段）
  const list = structured.knowledgeSources
  if (Array.isArray(list) && list.length > 0) {
    const s = list[0]?.score
    if (s != null) {
      const n = Number(s)
      if (Number.isFinite(n)) return n
    }
  }

  // 来源3：从 retrievalMetadata 取 max_score（后端返回的统计信息）
  const meta = structured.retrievalMetadata
  if (meta && typeof meta === 'object') {
    // 尝试多种可能的字段名
    const s = meta.maxScore ?? meta.max_score ?? meta.retrieved_score ?? meta.maxSimilarityScore
    if (s != null) {
      const n = Number(s)
      if (Number.isFinite(n)) return n
    }
  }

  // 来源4：从 graphSources 取（兜底）
  const graphs = structured.graphSources
  if (Array.isArray(graphs) && graphs.length > 0) {
    const score = graphs[0]?.score
    if (score != null) {
      const n = Number(score)
      if (Number.isFinite(n)) return n
    }
  }

  // 来源5：兜底模式检查 - 如果有 fallback 但没有 score，说明 RAG 服务不可用
  // 返回特殊标记 -1 表示检索不可用（而非 null 表示无数据）
  if (structured.fallback === true) {
    return -1  // 特殊值：-1 表示检索服务不可用
  }

  return null
}

/** el-tag type：便于一眼区分检索强弱（阈值可按业务调） */
function retrievalScoreTagType(score) {
  if (score == null || !Number.isFinite(score)) return 'info'
  if (score === -1) return 'info'  // 检索服务不可用
  if (score >= 0.35) return 'success'
  if (score >= 0.18) return 'warning'
  return 'danger'
}

/** 格式化分数显示文本 */
function formatRetrievalScore(score) {
  if (score == null) return '--'
  if (score === -1) return '不可用'  // 检索服务不可用
  if (!Number.isFinite(score)) return '--'
  // 保留两位小数
  return score.toFixed(2)
}

/** 获取检索详情列表（用于检索详情下拉框展示） */
function getRetrievalDetails(message) {
  // 支持传入 structured 对象或整个 message 对象
  const structured = message?.structured ?? message
  if (!structured) return []

  // 优先从 citedKnowledgeSources 获取（Spring Boot 返回的字段）
  const citedSources = structured.citedKnowledgeSources || []
  if (Array.isArray(citedSources) && citedSources.length > 0) {
    return citedSources.map((item, idx) => ({
      id: item.id || `ref-${idx}`,
      title: item.title || '未知标题',
      source: item.source || '',
      score: Number(item.score) || 0,
      rerankScore: item.rerankScore ?? item.rerank_score ?? null,
      content: item.content || '',
      metadata: item.metadata || {}
    }))
  }

  // 从 knowledgeSources 获取（转换后的字段）
  const knowledgeSources = structured.knowledgeSources || []
  if (Array.isArray(knowledgeSources) && knowledgeSources.length > 0) {
    return knowledgeSources.map((item, idx) => ({
      id: item.id || `ref-${idx}`,
      title: item.title || '未知标题',
      source: item.source || '',
      score: Number(item.score) || 0,
      rerankScore: item.rerankScore ?? item.rerank_score ?? null,
      content: item.content || '',
      metadata: item.metadata || {}
    }))
  }

  // 从 references 获取（后端返回的完整引用列表）
  const references = structured.references || []
  if (Array.isArray(references) && references.length > 0) {
    return references.map((item, idx) => ({
      id: item.id || `ref-${idx}`,
      title: item.title || '未知标题',
      source: item.source || '',
      score: Number(item.score) || 0,
      rerankScore: item.rerankScore ?? item.rerank_score ?? null,
      content: item.content || '',
      metadata: item.metadata || {}
    }))
  }

  // 从 retrievalMetadata.all_scores 获取（按索引匹配）
  const meta = structured.retrievalMetadata || {}
  if (meta.all_scores && Array.isArray(meta.all_scores) && meta.all_scores.length > 0) {
    // 生成默认条目，分数从 all_scores 获取
    return meta.all_scores.map((score, idx) => ({
      id: `meta-ref-${idx}`,
      title: `检索结果 #${idx + 1}`,
      source: `qdrant://${meta.target_collections?.[0] || 'unknown'}`,
      score: Number(score) || 0,
      rerankScore: null,
      content: '',
      metadata: { intent_domain: meta.intent_domain || 'general' }
    }))
  }

  return []
}

/** 根据向量相似度分数返回对应的 el-tag type */
function getScoreTagType(score) {
  if (score == null || !Number.isFinite(score)) return 'info'
  if (score >= 0.35) return 'success'
  if (score >= 0.18) return 'warning'
  return 'danger'
}

/** 截断内容文本以便于展示 */
function truncateContent(text, maxLength = 200) {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

/** 去掉 AI 回答中的 Markdown 标记，转成纯文本 + 符号列表展示 */
function stripMarkdown(text) {
  return String(text || '')
    .replace(/\r\n/g, '\n')
    .replace(/^\s*#{1,6}\s*/gm, '')
    .replace(/^\s*([-*_])\1{2,}\s*$/gm, '')
    .replace(/^\s*[-*+•]\s+/gm, '• ')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/__(.+?)__/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\*+/g, '')
    .replace(/_{2,}/g, '')
    .replace(/[ \t]+$/gm, '')
    .trim()
}

/** 将长文拆成多段排版：按空行分段，段内换行交给 CSS 保留 */
function answerParagraphs(text) {
  const s = stripMarkdown(text)
  if (!s) return []
  const parts = s
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean)
  return parts.length ? parts : [s]
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
  appendMessage('user', question)
  sending.value = true

  try {
    if (shouldUseGeneralAiChat(mode, question)) {
      const chatResp = await sendAiChatApi({
        question,
        relatedModule: 'GENERAL'
      })
      appendMessage('assistant', chatResp?.answer || '你好，我在。')
      return
    }

    const useBusinessContext = mode !== 'knowledge'
    const response = await sendAiRagChatApi({
      query: question,
      mode,
      relatedWarningId: useBusinessContext ? latestWarning.value?.id : undefined,
      relatedDeviceId: useBusinessContext ? latestWarning.value?.relatedDeviceId : undefined,
      relatedFarmlandId: useBusinessContext ? latestWarning.value?.farmlandId : undefined
    })

    appendMessage('assistant', '', normalizeStructuredResponse(response, mode))
  } catch (error) {
    const detail =
      (typeof error?.message === 'string' && error.message) ||
      error?.response?.data?.message ||
      error?.msg ||
      ''
    appendMessage(
      'assistant',
      detail
        ? `${detail}\n\n提示：请确认 rag-service(9001) 与 Spring 的 smart-agri.rag.base-url 已正确连通，并确保已配置真实大模型 API Key。`
        : '本次 RAG 问答调用失败，请稍后重试。'
    )
  } finally {
    sending.value = false
  }
}

function shouldUseGeneralAiChat(mode, question) {
  const text = String(question || '').trim()
  if (!text) {
    return false
  }
  // 闲聊识别与模式解耦：命中通用问候/寒暄即直连 /ai/chat
  const isGeneral = GENERAL_CHAT_HINT_PATTERNS.some((p) => p.test(text))
  const isAgri = AGRI_DOMAIN_HINT_PATTERNS.some((p) => p.test(text))
  // 很短且无农业领域词，也按通用聊天处理（如：在吗、谢谢）
  const isShortGeneral = text.length <= 8 && !isAgri
  return (isGeneral && !isAgri) || isShortGeneral
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
  background:
    radial-gradient(circle at top right, rgba(76, 139, 245, 0.1), transparent 34%),
    radial-gradient(circle at left bottom, rgba(23, 138, 98, 0.12), transparent 28%),
    linear-gradient(135deg, #f6fcf8 0%, #eff8f3 56%, #f7fbf9 100%);
  box-shadow: var(--app-shadow-hero);
  padding: 10px 16px;
}

.ai-rag-header__main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.ai-rag-header__main p {
  margin: 0;
  font-size: 12px;
  color: var(--app-text-secondary);
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
  gap: 8px;
  padding: 10px;
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
  padding: 14px;
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
  gap: 10px;
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
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---------- RAG 结构化回答 ---------- */
.rag-answer-min {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rag-answer-min__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 2px;
  border-bottom: 1px solid rgba(17, 82, 62, 0.08);
}

.rag-answer-min__head-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(22, 101, 72, 0.88);
}

.rag-answer-min__head-ic {
  font-size: 18px;
  color: rgba(28, 146, 102, 0.85);
}

.rag-answer-min__ic {
  margin-right: 4px;
  vertical-align: middle;
}

.rag-answer-min__prose {
  padding: 2px 2px 4px;
}

.rag-answer-min__para {
  margin: 0 0 0.85em;
  font-size: 15px;
  line-height: 1.82;
  letter-spacing: 0.01em;
  color: var(--app-text);
  text-align: justify;
  text-justify: inter-ideograph;
  white-space: pre-wrap;
  word-break: break-word;
}

.rag-answer-min__para:last-child {
  margin-bottom: 0;
}

.rag-answer-min__conclusion-card {
  margin: 0;
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(120deg, rgba(236, 253, 245, 0.95), rgba(248, 252, 250, 0.98));
  border: 1px solid rgba(28, 146, 102, 0.14);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.rag-answer-min__conclusion-card__hd {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: rgba(22, 101, 72, 0.75);
  text-transform: none;
  margin-bottom: 8px;
}

.rag-answer-min__conclusion-card__body {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: rgba(15, 52, 40, 0.88);
}

.rag-answer-min__meta-panel {
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(17, 82, 62, 0.08);
}

.rag-answer-min__meta-panel__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(22, 101, 72, 0.8);
  margin-bottom: 6px;
}

.rag-answer-min__meta-panel__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.rag-answer-min__score-tag {
  font-variant-numeric: tabular-nums;
}

.rag-answer-min__source-foot {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px dashed rgba(17, 82, 62, 0.14);
}

.rag-answer-min__source-foot__ic {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 18px;
  color: rgba(28, 146, 102, 0.75);
}

.rag-answer-min__source-foot__main {
  flex: 1;
  min-width: 0;
}

.rag-answer-min__source-foot__label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(22, 101, 72, 0.65);
  margin-bottom: 4px;
}

.rag-answer-min__source-foot__text {
  font-size: 12px;
  line-height: 1.55;
  color: var(--app-text-secondary);
  word-break: break-word;
}

.rag-answer-min__graph-pill {
  flex-shrink: 0;
  align-self: center;
}

.rag-answer-min__source-label {
  display: inline-block;
  margin-right: 8px;
  font-weight: 600;
  color: rgba(22, 101, 72, 0.85);
}

.rag-answer-min__kg-strip {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(17, 82, 62, 0.12);
  font-size: 13px;
  color: var(--app-text-secondary);
}

.rag-answer-min__kg-strip__hint {
  margin-left: 6px;
  font-size: 12px;
  color: rgba(22, 101, 72, 0.55);
}

.rag-answer-min__kg-chips {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
}

.rag-answer-min__fallback {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #8a5a00;
}

/* ========== 检索详情面板 ========== */
.rag-answer-min__retrieval-panel {
  margin-top: 12px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(17, 82, 62, 0.1);
}

.retrieval-panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(22, 101, 72, 0.8);
}

.retrieval-panel__count {
  font-weight: 400;
  color: rgba(55, 65, 81, 0.6);
  font-size: 12px;
}

.retrieval-detail-list {
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.retrieval-detail-item {
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(17, 82, 62, 0.08);
  transition: all 0.2s ease;
}

.retrieval-detail-item:hover {
  border-color: rgba(28, 146, 102, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.retrieval-detail-item__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.retrieval-detail-item__index {
  font-weight: 700;
  font-size: 13px;
  color: rgba(28, 146, 102, 0.75);
  min-width: 24px;
}

.retrieval-detail-item__title {
  font-weight: 600;
  font-size: 13px;
  color: var(--app-text);
  flex: 1;
  min-width: 120px;
}

.retrieval-detail-item__source {
  font-size: 12px;
  color: rgba(55, 65, 81, 0.7);
  margin-bottom: 4px;
}

.retrieval-detail-item__source-label {
  font-weight: 600;
}

.retrieval-detail-item__source-value {
  font-family: monospace;
  font-size: 11px;
  color: rgba(28, 146, 102, 0.7);
  background: rgba(236, 253, 245, 0.5);
  padding: 2px 6px;
  border-radius: 4px;
}

.retrieval-detail-item__content {
  font-size: 12px;
  color: rgba(55, 65, 81, 0.8);
  line-height: 1.6;
}

.retrieval-detail-item__content-label {
  font-weight: 600;
  margin-right: 6px;
}

.retrieval-detail-item__content-text {
  word-break: break-word;
}

.retrieval-detail-item__meta {
  margin-top: 6px;
  display: flex;
  gap: 6px;
}

.ai-rag-composer {
  flex-shrink: 0;
  display: grid;
  gap: 8px;
  border: 1px solid rgba(17, 82, 62, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  padding: 10px 12px;
}

.ai-rag-composer__top {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ai-rag-composer :deep(.el-textarea__inner) {
  min-height: 96px !important;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
}

.ai-rag-composer__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@container app-content (max-width: 820px) {
  .welcome-mode-cards,
  .welcome-samples {
    grid-template-columns: 1fr;
  }

  .ai-rag-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@container app-content (max-width: 780px) {
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

@container app-content (max-width: 700px) {
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
}
</style>
