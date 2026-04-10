<template>
  <div class="kg-page">
    <header class="kg-toolbar page-card">
      <div class="kg-toolbar__title">
        <div class="kg-toolbar__heading">
          <h1>农业知识网络</h1>
          <span class="kg-toolbar__badge">Neo4j · 多布局 · 路径分析</span>
        </div>
        <p>业务可读：实体说明、一度关系、类型分布 · 炫酷：力导向 / 环形 / 辐射分层</p>
      </div>
      <div class="kg-toolbar__stats" v-if="stats">
        <span>实体 {{ stats.entity_count }}</span>
        <span>关系 {{ stats.relation_count }}</span>
        <el-tag :type="stats.neo4j_connected ? 'success' : 'danger'" size="small">
          {{ stats.neo4j_connected ? 'Neo4j 已连接' : 'Neo4j 未连接' }}
        </el-tag>
        <div v-if="stats.kind_distribution?.length && stats.entity_count" class="kg-toolbar__dist">
          <span
            v-for="row in stats.kind_distribution.slice(0, 7)"
            :key="row.kind"
            class="kg-toolbar__dist-item"
            :title="`${kindLabel(row.kind)}: ${row.count}`"
          >
            <i class="kg-toolbar__dist-dot" :style="{ background: categoryColor(row.kind) }" />
            {{ kindLabel(row.kind).slice(0, 1) }}{{ Math.round((row.count / stats.entity_count) * 100) }}%
          </span>
        </div>
      </div>
      <div class="kg-toolbar__actions">
        <el-button type="primary" plain :loading="seeding" @click="handleSeedDemo">快速导入（内置种子）</el-button>
        <el-button plain @click="sideTab = 'ingest'">上传 JSON 灌库</el-button>
        <el-button :loading="loading" @click="loadSubgraph">刷新子图</el-button>
      </div>
    </header>

    <section class="kg-workspace">
      <aside class="kg-side page-card">
        <el-tabs v-model="sideTab" class="kg-tabs">
          <el-tab-pane label="入库" name="ingest">
            <p class="kg-tab-tip">
              将三元组写入 Neo4j 后，本页探索与 AI 助手「知识图谱关联」才会命中实体。可选用内置种子、上传脚本生成的 JSON，或在电脑上用命令行一键灌库。
            </p>
            <el-button type="primary" class="kg-full-btn" :loading="seeding" @click="handleSeedDemo">
              从服务端内置资源导入
            </el-button>
            <p class="kg-ingest-hint">与顶部「快速导入」相同，读取后端 jar 内 agri-graph-seed.json + 实体说明。</p>

            <h3 class="kg-side__h">上传文件导入</h3>
            <p class="kg-tab-tip kg-tab-tip--tight">
              运行 <code class="kg-code">scripts/generate_agri_graph_seed.py</code> 后，在
              <code class="kg-code">src/main/resources/knowledge-graph/</code> 可得到
              <code class="kg-code">agri-graph-seed.json</code>（数组）与
              <code class="kg-code">agri-graph-entities.json</code>（对象）。也可选择一个同时含
              <code class="kg-code">triples</code> 与 <code class="kg-code">entities</code> 的 bundle。
            </p>
            <input
              ref="triplesFileRef"
              type="file"
              accept=".json,application/json"
              class="kg-file-input"
              @change="onTriplesJsonPicked"
            />
            <input
              ref="entitiesFileRef"
              type="file"
              accept=".json,application/json"
              class="kg-file-input"
              @change="onEntitiesJsonPicked"
            />
            <el-button class="kg-full-btn kg-mt" @click="openTriplesPicker">1. 选择三元组 JSON</el-button>
            <p v-if="triplesPickLabel" class="kg-file-label">{{ triplesPickLabel }}</p>
            <el-button class="kg-full-btn kg-mt" @click="openEntitiesPicker">2. 选择实体说明 JSON（可选）</el-button>
            <p v-if="entitiesPickLabel" class="kg-file-label">{{ entitiesPickLabel }}</p>
            <el-button
              type="primary"
              class="kg-full-btn kg-mt"
              :loading="importing"
              :disabled="!pendingImportTriples?.length"
              @click="submitBundleImport"
            >
              导入到 Neo4j
            </el-button>
            <el-button
              v-if="triplesPickLabel || entitiesPickLabel"
              class="kg-full-btn kg-mt"
              link
              type="danger"
              @click="clearPendingImport"
            >
              清除已选文件
            </el-button>

            <h3 class="kg-side__h">命令行流水线（本机）</h3>
            <p class="kg-tab-tip kg-tab-tip--tight">
              在后端项目根目录执行（需已配置 Neo4j 密码）。任选一种鉴权：流水线密钥免登录，或管理员 JWT。
            </p>
            <pre class="kg-cli-block">{{ kgCliSnippet }}</pre>
            <el-button class="kg-full-btn kg-mt" @click="copyKgCli">复制命令说明</el-button>
          </el-tab-pane>

          <el-tab-pane label="探索" name="explore">
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="中心实体 id">
                <el-input v-model="centerId" placeholder="如 crop_strawberry" clearable />
              </el-form-item>
              <el-form-item label="展开深度">
                <el-slider v-model="depth" :min="1" :max="3" :marks="{ 1: '1', 2: '2', 3: '3' }" show-stops />
              </el-form-item>
              <el-button type="primary" class="kg-full-btn" :loading="loading" @click="loadSubgraph">展开子图</el-button>
            </el-form>
            <h3 class="kg-side__h">搜索实体</h3>
            <el-input
              v-model="searchQ"
              placeholder="名称或 id 关键字"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-button class="kg-full-btn kg-mt" :loading="searching" @click="handleSearch">搜索</el-button>
            <ul v-if="searchResults.length" class="kg-search-list">
              <li v-for="item in searchResults" :key="item.id">
                <button type="button" class="kg-search-item" @click="pickEntity(item)">
                  <strong>{{ item.name }}</strong>
                  <small>{{ item.kind }} · {{ item.id }}</small>
                </button>
              </li>
            </ul>
          </el-tab-pane>

          <el-tab-pane label="路径" name="path">
            <p class="kg-tab-tip">查询两实体间<strong>最短无向路径</strong>，用于因果链 / 溯源解读。</p>
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="起点 id">
                <el-input v-model="pathFromId" placeholder="如 crop_strawberry" clearable />
              </el-form-item>
              <el-form-item label="终点 id">
                <el-input v-model="pathToId" placeholder="如 dis_gray_mold" clearable />
              </el-form-item>
              <el-form-item label="最大跳数">
                <el-input-number v-model="pathMaxHops" :min="1" :max="12" size="small" class="kg-num" />
              </el-form-item>
              <el-button type="primary" class="kg-full-btn" :loading="pathLoading" @click="loadPathGraph">查找并展示路径</el-button>
              <el-button class="kg-full-btn kg-mt" :disabled="!lastViz || lastViz._view !== 'path'" @click="backToSubgraph">
                返回子图探索
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="视图" name="view">
            <p class="kg-tab-tip">布局影响<strong>读图方式</strong>；类型筛选可聚焦作物/病害等业务视角。</p>
            <el-form label-position="top" @submit.prevent>
              <el-form-item label="图布局">
                <el-radio-group v-model="layoutMode" size="small" class="kg-radio-group" @change="refreshChartRender">
                  <el-radio-button label="force">力导向</el-radio-button>
                  <el-radio-button label="circular">环形</el-radio-button>
                  <el-radio-button label="radial">辐射</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-form>
            <div class="kg-kind-filter">
              <div class="kg-kind-filter__head">
                <span>实体类型</span>
                <el-button link type="primary" size="small" @click="setAllKinds(true)">全选</el-button>
                <el-button link type="primary" size="small" @click="setAllKinds(false)">清空</el-button>
              </div>
              <el-checkbox-group v-model="kindCheckedList" class="kg-kind-checks" @change="refreshChartRender">
                <el-checkbox v-for="k in KIND_ORDER" :key="k" :label="k">
                  {{ kindLabel(k) }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
            <el-button class="kg-full-btn kg-mt" :disabled="!getDisplayViz()" @click="exportVizJson">导出当前图 JSON</el-button>
          </el-tab-pane>
        </el-tabs>
      </aside>

      <div ref="chartWrapRef" class="kg-chart-wrap page-card" :class="{ 'kg-chart-wrap--fs': chartFullscreen }">
        <div class="kg-chart-grid" aria-hidden="true" />
        <div class="kg-chart-vignette" aria-hidden="true" />
        <div class="kg-chart-hud">
          <div class="kg-chart-hud__main">
            <span class="kg-chart-hud__label">当前焦点</span>
            <strong class="kg-chart-hud__center">{{ hudCenterLabel }}</strong>
            <div class="kg-chart-hud__meta">
              <span class="kg-chip">{{ hudStats.nodes }} 实体</span>
              <span class="kg-chip">{{ hudStats.edges }} 关系</span>
              <span class="kg-chip">深度 {{ lastViz?.depth ?? depth }}</span>
              <span v-if="lastViz?._view === 'path'" class="kg-chip kg-chip--accent">路径视图 → {{ pathTargetLabel }}</span>
            </div>
          </div>
          <div class="kg-chart-hud__actions">
            <el-button size="small" :disabled="!lastViz" @click="relayoutGraph">重新布局</el-button>
            <el-button size="small" @click="resizeChart">适应窗口</el-button>
            <el-button size="small" type="primary" plain @click="toggleChartFullscreen">
              {{ chartFullscreen ? '退出全屏' : '全屏画布' }}
            </el-button>
          </div>
        </div>
        <div ref="chartRef" class="kg-chart" />
        <p class="kg-chart-hint">
          单击<strong>节点</strong>：打开业务详情抽屉 · <kbd>Shift</kbd>+单击：快捷以此为中心展开子图 · 单击<strong>连线</strong>：关系溯源 · 右上工具栏导出 PNG
        </p>
      </div>
    </section>

    <el-drawer
      v-model="edgeDrawerVisible"
      title="关系详情"
      direction="rtl"
      size="320px"
      class="kg-edge-drawer"
    >
      <template v-if="selectedEdge">
        <dl class="kg-edge-dl">
          <dt>起点</dt>
          <dd>{{ nodeNameMap[selectedEdge.source] || selectedEdge.source }}</dd>
          <dt>终点</dt>
          <dd>{{ nodeNameMap[selectedEdge.target] || selectedEdge.target }}</dd>
          <dt>谓词</dt>
          <dd class="kg-edge-predicate">{{ selectedEdge.predicate || '—' }}</dd>
          <dt>来源参考</dt>
          <dd>{{ selectedEdge.source_ref ?? '—' }}</dd>
          <dt>置信度</dt>
          <dd>
            {{
              selectedEdge.confidence == null || selectedEdge.confidence === ''
                ? '—'
                : Number(selectedEdge.confidence).toFixed(2)
            }}
          </dd>
        </dl>
      </template>
    </el-drawer>

    <el-drawer v-model="nodeDrawerVisible" title="实体详情（业务）" direction="rtl" size="400px" class="kg-node-drawer">
      <template v-if="nodeDetailLoading">
        <el-skeleton :rows="6" animated />
      </template>
      <template v-else-if="nodeDetail?.found">
        <div class="kg-node-head">
          <h2 class="kg-node-title">{{ nodeDetail.name }}</h2>
          <p class="kg-node-id">{{ nodeDetail.id }}</p>
          <el-tag size="small" type="success">{{ kindLabel(nodeDetail.kind) }}</el-tag>
          <span class="kg-node-deg">出度 {{ nodeDetail.degree_out }} · 入度 {{ nodeDetail.degree_in }}</span>
        </div>
        <p v-if="nodeDetail.description" class="kg-node-desc">{{ nodeDetail.description }}</p>
        <p v-else class="kg-node-desc kg-node-desc--muted">暂无业务说明（导入种子数据后会写入 description）</p>
        <div v-if="nodeDetail.aliases?.length" class="kg-node-alias">
          <span v-for="a in nodeDetail.aliases" :key="a" class="kg-chip">{{ a }}</span>
        </div>
        <h4 class="kg-node-sec">出边（{{ nodeDetail.relations_out?.length || 0 }}）</h4>
        <ul class="kg-rel-list">
          <li v-for="r in nodeDetail.relations_out || []" :key="'o' + r.id + r.predicate">
            <button type="button" class="kg-rel-btn" @click="pickNeighbor(r)">
              <span class="kg-rel-pred">{{ r.predicate }}</span>
              <span>{{ r.name }} · {{ r.id }}</span>
            </button>
          </li>
        </ul>
        <h4 class="kg-node-sec">入边（{{ nodeDetail.relations_in?.length || 0 }}）</h4>
        <ul class="kg-rel-list">
          <li v-for="r in nodeDetail.relations_in || []" :key="'i' + r.id + r.predicate">
            <button type="button" class="kg-rel-btn" @click="pickNeighbor(r)">
              <span class="kg-rel-pred">{{ r.predicate }}</span>
              <span>{{ r.name }} · {{ r.id }}</span>
            </button>
          </li>
        </ul>
        <div class="kg-node-actions">
          <el-button type="primary" @click="setCenterFromDetail">设为中心并展开子图</el-button>
          <el-button @click="fillPathFromDetail">填入路径起点</el-button>
        </div>
      </template>
      <template v-else-if="nodeDetail && !nodeDetail.found">
        <el-empty description="未找到该实体" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  getKnowledgeGraphEntityApi,
  getKnowledgeGraphPathApi,
  getKnowledgeGraphStatsApi,
  getKnowledgeGraphSubgraphApi,
  importKnowledgeGraphBundleApi,
  searchKnowledgeGraphApi,
  seedKnowledgeGraphDemoApi
} from '@/api/knowledgeGraph'

const KIND_ORDER = [
  'crop',
  'disease',
  'pest',
  'stress',
  'practice',
  'environment',
  'soil',
  'nutrient',
  'deficiency',
  'fertilizer',
  'pesticide',
  'growth_stage',
  'symptom',
  'input',
  'concept',
  'other'
]

/** 与图例顺序一致，用于节点/分类着色 */
const CATEGORY_PALETTE = [
  '#5FD68A',
  '#FF8B7A',
  '#FFC66D',
  '#7EB6FF',
  '#5CE1E6',
  '#B794F6',
  '#C4A574',
  '#9AE6B4',
  '#F6AD55',
  '#68D391',
  '#FC8181',
  '#76E4F7',
  '#F687B3',
  '#A0AEC0',
  '#E2E8F0',
  '#718096'
]

const chartRef = ref(null)
const chartWrapRef = ref(null)
const chart = shallowRef(null)
/** 最近一次子图数据，用于「重新布局」不重拉接口 */
const lastViz = ref(null)
const chartFullscreen = ref(false)

const stats = ref(null)
const centerId = ref('crop_strawberry')
const depth = ref(2)
const searchQ = ref('')
const searchResults = ref([])
const loading = ref(false)
const searching = ref(false)
const seeding = ref(false)
const importing = ref(false)
const triplesFileRef = ref(null)
const entitiesFileRef = ref(null)
const pendingImportTriples = ref(null)
const pendingImportEntities = ref(null)
const triplesPickLabel = ref('')
const entitiesPickLabel = ref('')

const kgCliSnippet = [
  '# ① 生成 JSON（在 smart-agriculture-server/scripts 下）',
  'python generate_agri_graph_seed.py',
  '',
  '# ② 灌库：流水线密钥（需在 application.yml 配置 pipeline-ingest-key）',
  'set SMART_AGRI_KG_PIPELINE_KEY=你的密钥',
  'python kg_ingest_pipeline.py --generate',
  '',
  '# 或：使用管理员 JWT 调用 /knowledge-graph/import',
  'set SMART_AGRI_ADMIN_JWT=登录后复制的Bearer令牌',
  'python kg_ingest_pipeline.py --generate --use-jwt-import'
].join('\n')

const edgeDrawerVisible = ref(false)
const selectedEdge = ref(null)
/** id -> 中文名，便于抽屉与提示展示 */
const nodeNameMap = ref({})

const sideTab = ref('explore')
const layoutMode = ref('force')
const kindCheckedList = ref([...KIND_ORDER])
const pathFromId = ref('crop_strawberry')
const pathToId = ref('dis_gray_mold')
const pathMaxHops = ref(10)
const pathLoading = ref(false)

const nodeDrawerVisible = ref(false)
const nodeDetailLoading = ref(false)
const nodeDetail = ref(null)

const hudStats = computed(() => ({
  nodes: lastViz.value?.nodes?.length ?? 0,
  edges: lastViz.value?.edges?.length ?? 0
}))

const hudCenterLabel = computed(() => {
  const v = lastViz.value
  if (!v?.center_id) return '—'
  const map = nodeNameMap.value || {}
  return map[v.center_id] || v.center_id
})

const pathTargetLabel = computed(() => {
  const v = lastViz.value
  if (!v?._pathTo) return ''
  const m = nodeNameMap.value || {}
  return m[v._pathTo] || v._pathTo
})

function categoryColor(kind) {
  return CATEGORY_PALETTE[kindIndex(kind) % CATEGORY_PALETTE.length]
}

function kindLabel(kind) {
  const map = {
    crop: '作物',
    disease: '病害',
    pest: '虫害',
    stress: '胁迫',
    practice: '农艺措施',
    environment: '环境',
    soil: '土壤',
    nutrient: '养分',
    deficiency: '缺素',
    fertilizer: '肥料',
    pesticide: '农药',
    growth_stage: '生育期',
    symptom: '症状',
    input: '投入品',
    concept: '概念',
    other: '其他'
  }
  return map[kind] || kind
}

function buildCategories() {
  return KIND_ORDER.map((k, i) => ({
    name: kindLabel(k),
    itemStyle: {
      color: CATEGORY_PALETTE[i % CATEGORY_PALETTE.length],
      borderColor: 'rgba(255,255,255,0.35)',
      borderWidth: 1
    }
  }))
}

function kindIndex(kind) {
  const i = KIND_ORDER.indexOf(kind)
  return i >= 0 ? i : KIND_ORDER.indexOf('other')
}

function liftColor(hex, amount) {
  if (echarts.color?.lift) {
    return echarts.color.lift(hex, amount)
  }
  return hex
}

function filterVizByKinds(viz, checkedList) {
  const center = viz.center_id
  if (!checkedList.length) {
    const nodes = (viz.nodes || []).filter((n) => n.id === center)
    const ids = new Set(nodes.map((n) => n.id))
    const edges = (viz.edges || []).filter((e) => ids.has(e.source) && ids.has(e.target))
    return { ...viz, nodes, edges }
  }
  const allow = new Set(checkedList)
  const nodes = (viz.nodes || []).filter((n) => n.id === center || allow.has(n.kind))
  const ids = new Set(nodes.map((n) => n.id))
  const edges = (viz.edges || []).filter((e) => ids.has(e.source) && ids.has(e.target))
  return { ...viz, nodes, edges }
}

function getDisplayViz() {
  const v = lastViz.value
  if (!v?.nodes) return null
  return filterVizByKinds(v, kindCheckedList.value)
}

function computeRadialPositions(viz, w, h) {
  const cx = w / 2
  const cy = h / 2
  const center = viz.center_id
  const adj = new Map()
  for (const e of viz.edges || []) {
    if (!adj.has(e.source)) adj.set(e.source, [])
    if (!adj.has(e.target)) adj.set(e.target, [])
    adj.get(e.source).push(e.target)
    adj.get(e.target).push(e.source)
  }
  const depth = new Map()
  const q = [center]
  depth.set(center, 0)
  while (q.length) {
    const u = q.shift()
    const du = depth.get(u)
    for (const v of adj.get(u) || []) {
      if (!depth.has(v)) {
        depth.set(v, du + 1)
        q.push(v)
      }
    }
  }
  for (const n of viz.nodes || []) {
    if (!depth.has(n.id)) depth.set(n.id, 0)
  }
  const layers = new Map()
  for (const n of viz.nodes || []) {
    const d = depth.get(n.id) ?? 0
    if (!layers.has(d)) layers.set(d, [])
    layers.get(d).push(n.id)
  }
  const keys = [...layers.keys()]
  const maxD = keys.length ? Math.max(...keys) : 0
  const maxR = Math.min(w, h) / 2 - 70
  const pos = {}
  for (const [d, ids] of layers) {
    const r = maxD === 0 ? 0 : (d / maxD) * maxR
    ids.forEach((id, i) => {
      const angle = (2 * Math.PI * i) / Math.max(ids.length, 1) - Math.PI / 2
      pos[id] = { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) }
    })
  }
  return pos
}

function setAllKinds(yes) {
  kindCheckedList.value = yes ? [...KIND_ORDER] : []
  refreshChartRender()
}

function refreshChartRender() {
  const v = getDisplayViz()
  if (!v) return
  renderGraph(JSON.parse(JSON.stringify(v)), layoutMode.value)
}

async function loadPathGraph() {
  const a = pathFromId.value?.trim()
  const b = pathToId.value?.trim()
  if (!a || !b) {
    ElMessage.warning('请填写起点与终点实体 id')
    return
  }
  pathLoading.value = true
  try {
    const r = await getKnowledgeGraphPathApi({ fromId: a, toId: b, maxHops: pathMaxHops.value })
    if (!r?.found) {
      ElMessage.warning('未找到连接路径，可增大最大跳数或更换实体')
      return
    }
    lastViz.value = {
      center_id: r.from_id,
      depth: r.hop_count,
      nodes: r.nodes,
      edges: r.edges,
      _view: 'path',
      _pathTo: r.to_id
    }
    refreshChartRender()
    ElMessage.success(`已展示最短路径，共 ${r.hop_count} 跳`)
  } catch {
    /* request 拦截器提示 */
  } finally {
    pathLoading.value = false
  }
}

async function backToSubgraph() {
  await loadSubgraph()
}

async function openNodeDetail(id) {
  if (!id) return
  nodeDrawerVisible.value = true
  edgeDrawerVisible.value = false
  nodeDetailLoading.value = true
  nodeDetail.value = null
  try {
    nodeDetail.value = await getKnowledgeGraphEntityApi(id)
  } catch {
    nodeDetail.value = { found: false }
  } finally {
    nodeDetailLoading.value = false
  }
}

function setCenterFromDetail() {
  if (!nodeDetail.value?.id) return
  centerId.value = nodeDetail.value.id
  nodeDrawerVisible.value = false
  loadSubgraph()
}

function fillPathFromDetail() {
  if (!nodeDetail.value?.id) return
  pathFromId.value = nodeDetail.value.id
  sideTab.value = 'path'
  ElMessage.info('已填入路径起点，请填写终点后点击「查找并展示路径」')
}

function pickNeighbor(r) {
  openNodeDetail(r.id)
}

function exportVizJson() {
  const v = getDisplayViz()
  if (!v) return
  const blob = new Blob([JSON.stringify(v, null, 2)], { type: 'application/json' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `kg_${v.center_id || 'export'}.json`
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('已下载当前图的 JSON')
}

function bindChartInteractions() {
  if (!chart.value) return
  chart.value.off('click')
  chart.value.on('click', (params) => {
    if (params.dataType === 'node') {
      const id = params.data?.id
      if (!id) return
      const ev = params.event?.event
      if (ev && ev.shiftKey) {
        centerId.value = id
        edgeDrawerVisible.value = false
        nodeDrawerVisible.value = false
        loadSubgraph()
        ElMessage.success({ message: '已以该实体为中心展开子图', duration: 1800 })
        return
      }
      openNodeDetail(id)
    } else if (params.dataType === 'edge') {
      const d = params.data || {}
      selectedEdge.value = {
        source: d.source,
        target: d.target,
        predicate: d.predicate ?? d.value,
        source_ref: d.source_ref,
        confidence: d.confidence
      }
      edgeDrawerVisible.value = true
    }
  })
}

function renderGraph(viz, layout = 'force') {
  if (!chartRef.value) return
  if (!chart.value) {
    chart.value = echarts.init(chartRef.value)
  }

  const nameMap = {}
  for (const n of viz.nodes || []) {
    nameMap[n.id] = n.name || n.id
  }
  nodeNameMap.value = nameMap

  const center = viz.center_id
  const catIdx = (k) => kindIndex(k)
  const nodes = (viz.nodes || []).map((n) => {
    const isCenter = n.id === center
    const baseColor = CATEGORY_PALETTE[catIdx(n.kind) % CATEGORY_PALETTE.length]
    const fillColor = isCenter
      ? new echarts.graphic.RadialGradient(0.35, 0.35, 0.7, [
          { offset: 0, color: '#f2fff7' },
          { offset: 0.35, color: '#8fefb0' },
          { offset: 1, color: '#1e8f52' }
        ])
      : new echarts.graphic.RadialGradient(0.4, 0.4, 0.85, [
          { offset: 0, color: liftColor(baseColor, 0.35) },
          { offset: 1, color: liftColor(baseColor, -0.22) }
        ])
    return {
      id: n.id,
      name: n.name,
      category: catIdx(n.kind),
      symbolSize: isCenter ? 62 : 34 + Math.min(12, (viz.nodes?.length || 0) > 40 ? 0 : 6),
      itemStyle: {
        color: fillColor,
        shadowBlur: isCenter ? 32 : 14,
        shadowColor: isCenter ? 'rgba(110, 255, 170, 0.5)' : 'rgba(0, 0, 0, 0.4)',
        borderWidth: isCenter ? 3 : 1,
        borderColor: isCenter ? 'rgba(255, 255, 255, 0.92)' : 'rgba(255, 255, 255, 0.32)'
      },
      label: {
        show: true,
        fontSize: isCenter ? 14 : 11,
        fontWeight: isCenter ? 700 : 500
      }
    }
  })

  let graphNodes = nodes
  if (layout === 'radial') {
    const w = chartRef.value?.clientWidth || 920
    const h = chartRef.value?.clientHeight || 520
    const pos = computeRadialPositions(viz, w, h)
    graphNodes = nodes.map((node) => ({
      ...node,
      x: pos[node.id]?.x ?? w / 2,
      y: pos[node.id]?.y ?? h / 2
    }))
  }

  const links = (viz.edges || []).map((e) => ({
    source: e.source,
    target: e.target,
    value: e.predicate,
    predicate: e.predicate,
    source_ref: e.source_ref,
    confidence: e.confidence,
    label: {
      show: true,
      formatter: e.predicate,
      fontSize: 10,
      color: 'rgba(230, 255, 240, 0.88)',
      backgroundColor: 'rgba(8, 32, 26, 0.55)',
      padding: [2, 6],
      borderRadius: 4
    },
    lineStyle: {
      width: 2,
      curveness: 0.2,
      color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: 'rgba(100, 200, 255, 0.35)' },
        { offset: 0.5, color: 'rgba(120, 255, 190, 0.55)' },
        { offset: 1, color: 'rgba(80, 220, 150, 0.4)' }
      ]),
      opacity: 0.88
    }
  }))

  const categories = buildCategories()

  chart.value.setOption(
    {
      backgroundColor: 'transparent',
      title: {
        text: 'Knowledge Graph',
        subtext: `${nameMap[center] || center}  ·  ${graphNodes.length} nodes · ${layout}`,
        left: 'center',
        top: 6,
        textStyle: {
          color: 'rgba(255,255,255,0.22)',
          fontSize: 11,
          fontWeight: 500,
          letterSpacing: 2,
          fontFamily: 'ui-sans-serif, system-ui, sans-serif'
        },
        subtextStyle: {
          color: 'rgba(200, 255, 220, 0.35)',
          fontSize: 11
        }
      },
      toolbox: {
        show: true,
        orient: 'vertical',
        right: 10,
        top: 'middle',
        iconStyle: {
          borderColor: 'rgba(160, 240, 200, 0.65)'
        },
        emphasis: {
          iconStyle: { borderColor: '#b8ffd4' }
        },
        feature: {
          saveAsImage: {
            title: '导出高清 PNG',
            name: `知识图谱_${center}`,
            type: 'png',
            pixelRatio: 2,
            backgroundColor: 'rgba(6, 22, 20, 0.96)'
          },
          restore: {
            title: '重置缩放与平移'
          }
        }
      },
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(12, 38, 30, 0.92)',
        borderColor: 'rgba(94, 214, 138, 0.35)',
        textStyle: { color: '#e8fff2' },
        formatter: (p) => {
          if (p.dataType === 'edge') {
            const d = p.data || {}
            const ps = d.predicate || d.value || ''
            return `${nameMap[d.source] || d.source} → ${nameMap[d.target] || d.target}<br/><b>${ps}</b><br/><span style="opacity:.75;font-size:12px">单击连线查看详情</span>`
          }
          const d = p.data || {}
          const hint =
            d.id === center ? '中心 / 路径端点' : '单击查看业务详情 · Shift+单击快捷展开子图'
          const nk = viz.nodes?.find((x) => x.id === d.id)?.kind
          const typeLine = nk ? `${kindLabel(nk)} · ${d.id}` : d.id
          return `<div style="font-weight:600">${d.name}</div><div style="opacity:.85;font-size:12px">${typeLine}</div><div style="opacity:.65;font-size:11px;margin-top:4px">${hint}</div>`
        }
      },
      legend: {
        show: true,
        data: categories.map((c) => c.name),
        textStyle: { color: 'rgba(255,255,255,0.72)' },
        bottom: 2,
        left: 'center',
        type: 'scroll',
        itemGap: 12,
        itemWidth: 10,
        itemHeight: 10
      },
      series: [
        (() => {
          const s = {
            type: 'graph',
            roam: true,
            draggable: true,
            scaleLimit: { min: 0.12, max: 4 },
            categories,
            data: graphNodes,
            links,
            edgeSymbol: ['circle', 'arrow'],
            edgeSymbolSize: [4, 10],
            emphasis: {
              focus: 'adjacency',
              scale: 1.12,
              itemStyle: { shadowBlur: 32 },
              lineStyle: { width: 4, opacity: 1, color: 'rgba(140, 255, 200, 0.95)' },
              label: { fontSize: 14 }
            },
            blur: {
              itemStyle: { opacity: 0.25 },
              lineStyle: { opacity: 0.12 }
            },
            lineStyle: { curveness: layout === 'circular' ? 0.08 : 0.16, opacity: 0.78 },
            label: {
              color: '#f0fff4',
              fontSize: 11,
              textBorderColor: 'rgba(0,0,0,0.65)',
              textBorderWidth: 2
            },
            animation: true,
            animationDuration: 1000,
            animationDurationUpdate: 750,
            animationEasingUpdate: 'cubicOut'
          }
          if (layout === 'force') {
            s.layout = 'force'
            s.force = {
              repulsion: Math.min(720, 320 + (graphNodes.length || 0) * 14),
              edgeLength: [72, 156],
              gravity: 0.1,
              friction: 0.68
            }
          } else if (layout === 'circular') {
            s.layout = 'circular'
            s.circular = { rotateLabel: true }
          } else {
            s.layout = 'none'
          }
          return s
        })()
      ]
    },
    true
  )

  bindChartInteractions()
}

async function loadStats() {
  try {
    stats.value = await getKnowledgeGraphStatsApi()
  } catch {
    stats.value = null
    // 业务/网络错误由 @/utils/request 拦截器统一 ElMessage
  }
}

async function loadSubgraph() {
  const id = centerId.value?.trim()
  if (!id) {
    ElMessage.warning('请填写中心实体 id')
    return
  }
  loading.value = true
  try {
    const viz = await getKnowledgeGraphSubgraphApi({ centerId: id, depth: depth.value })
    lastViz.value = { ...viz, _view: 'subgraph' }
    pathFromId.value = id
    refreshChartRender()
    if (!viz.nodes?.length) {
      ElMessage.info('未找到该实体或子图为空，可先「导入农业知识图谱数据」')
    }
  } catch {
    /* 同上 */
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  searching.value = true
  searchResults.value = []
  try {
    const list = await searchKnowledgeGraphApi({ q: searchQ.value?.trim() || '', limit: 30 })
    searchResults.value = Array.isArray(list) ? list : []
    if (!searchResults.value.length) {
      ElMessage.info('无匹配实体')
    }
  } catch {
    /* 同上 */
  } finally {
    searching.value = false
  }
}

function pickEntity(item) {
  centerId.value = item.id
  loadSubgraph()
}

async function handleSeedDemo() {
  seeding.value = true
  try {
    await seedKnowledgeGraphDemoApi()
    ElMessage.success('农业领域知识三元组已 MERGE 写入 Neo4j（可重复导入更新属性）')
    await loadStats()
    await loadSubgraph()
  } catch {
    /* 同上 */
  } finally {
    seeding.value = false
  }
}

function onTriplesJsonPicked(ev) {
  const input = ev.target
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    try {
      const parsed = JSON.parse(String(reader.result || ''))
      if (Array.isArray(parsed)) {
        pendingImportTriples.value = parsed
        pendingImportEntities.value = null
      } else if (parsed && typeof parsed === 'object' && Array.isArray(parsed.triples)) {
        pendingImportTriples.value = parsed.triples
        pendingImportEntities.value =
          parsed.entities && typeof parsed.entities === 'object' && !Array.isArray(parsed.entities)
            ? parsed.entities
            : null
      } else {
        ElMessage.error('三元组文件须为 JSON 数组，或包含 triples 数组的对象')
        return
      }
      triplesPickLabel.value = file.name
    } catch {
      ElMessage.error('三元组 JSON 解析失败')
    }
  }
  reader.readAsText(file, 'UTF-8')
}

function onEntitiesJsonPicked(ev) {
  const input = ev.target
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    try {
      const parsed = JSON.parse(String(reader.result || ''))
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        ElMessage.error('实体文件须为 JSON 对象（实体 id → description / aliases）')
        return
      }
      pendingImportEntities.value = parsed
      entitiesPickLabel.value = file.name
    } catch {
      ElMessage.error('实体 JSON 解析失败')
    }
  }
  reader.readAsText(file, 'UTF-8')
}

function clearPendingImport() {
  pendingImportTriples.value = null
  pendingImportEntities.value = null
  triplesPickLabel.value = ''
  entitiesPickLabel.value = ''
}

async function submitBundleImport() {
  if (!pendingImportTriples.value?.length) {
    ElMessage.warning('请先选择三元组 JSON')
    return
  }
  const body = { triples: pendingImportTriples.value }
  if (pendingImportEntities.value) {
    body.entities = pendingImportEntities.value
  }
  importing.value = true
  try {
    const res = await importKnowledgeGraphBundleApi(body)
    const tu = res?.triples_upserted ?? res?.triplesUpserted
    const eu = res?.entities_updated ?? res?.entitiesUpdated
    ElMessage.success(
      `已写入三元组 ${tu != null ? tu : '?'} 条，实体说明更新 ${eu != null ? eu : 0} 条`
    )
    clearPendingImport()
    await loadStats()
    await loadSubgraph()
  } catch {
    /* request 已提示 */
  } finally {
    importing.value = false
  }
}

function openTriplesPicker() {
  triplesFileRef.value?.click()
}

function openEntitiesPicker() {
  entitiesFileRef.value?.click()
}

async function copyKgCli() {
  try {
    await navigator.clipboard.writeText(kgCliSnippet)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选中下方文本')
  }
}

function resize() {
  chart.value?.resize()
}

function resizeChart() {
  nextTick(() => {
    chart.value?.resize()
  })
}

function relayoutGraph() {
  refreshChartRender()
}

async function toggleChartFullscreen() {
  const el = chartWrapRef.value
  if (!el) return
  try {
    if (!document.fullscreenElement) {
      await el.requestFullscreen()
    } else {
      await document.exitFullscreen()
    }
  } catch {
    ElMessage.warning('当前环境无法使用全屏')
  }
}

function syncFullscreenState() {
  const el = chartWrapRef.value
  chartFullscreen.value = !!(el && document.fullscreenElement === el)
  nextTick(() => resizeChart())
}

const route = useRoute()

onMounted(async () => {
  const q = route.query.centerId ?? route.query.c
  if (typeof q === 'string' && q.trim()) {
    centerId.value = q.trim()
  }
  await loadStats()
  await loadSubgraph()
  window.addEventListener('resize', resize)
  document.addEventListener('fullscreenchange', syncFullscreenState)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  document.removeEventListener('fullscreenchange', syncFullscreenState)
  chart.value?.dispose()
  chart.value = null
})
</script>

<style scoped lang="scss">
.kg-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: calc(100vh - 120px);
}

.kg-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
}

.kg-toolbar__heading {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.kg-toolbar__title h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.02em;
  background: linear-gradient(120deg, var(--app-text-strong) 0%, rgba(72, 187, 120, 0.95) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.kg-toolbar__badge {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid rgba(94, 214, 138, 0.35);
  color: rgba(72, 187, 120, 0.95);
  background: rgba(72, 187, 120, 0.08);
}

.kg-toolbar__title p {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-muted);
}

.kg-toolbar__stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--app-text-muted);
}

.kg-toolbar__actions {
  display: flex;
  gap: 10px;
}

.kg-toolbar__dist {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  align-items: center;
  max-width: 420px;
}

.kg-toolbar__dist-item {
  font-size: 11px;
  color: var(--app-text-muted);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.kg-toolbar__dist-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.kg-workspace {
  display: flex;
  gap: 14px;
  flex: 1;
  min-height: 560px;
}

.kg-side {
  width: 308px;
  flex-shrink: 0;
  padding: 10px 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kg-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
}

.kg-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  font-weight: 600;
}

.kg-tab-tip {
  font-size: 12px;
  line-height: 1.55;
  color: var(--app-text-muted);
  margin: 0 0 10px;
}

.kg-tab-tip--tight {
  margin-bottom: 8px;
}

.kg-ingest-hint {
  font-size: 11px;
  color: var(--app-text-muted);
  margin: 6px 0 0;
  line-height: 1.45;
}

.kg-code {
  font-size: 11px;
  padding: 1px 4px;
  border-radius: 4px;
  background: rgba(15, 52, 40, 0.35);
  word-break: break-all;
}

.kg-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.kg-file-label {
  font-size: 11px;
  color: var(--app-text-muted);
  margin: 4px 0 0;
  word-break: break-all;
}

.kg-cli-block {
  margin: 0;
  padding: 10px;
  font-size: 11px;
  line-height: 1.5;
  border-radius: 8px;
  border: 1px solid rgba(63, 179, 123, 0.25);
  background: rgba(15, 52, 40, 0.2);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow: auto;
}

.kg-side__h {
  margin: 12px 0 4px;
  font-size: 14px;
  color: var(--app-text-strong);
}

.kg-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.kg-num {
  width: 100% !important;
}

.kg-kind-filter {
  margin-top: 8px;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(63, 179, 123, 0.2);
  background: rgba(15, 52, 40, 0.15);
}

.kg-kind-filter__head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-strong);
  margin-bottom: 8px;
}

.kg-kind-checks {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 200px;
  overflow: auto;
}

.kg-kind-checks :deep(.el-checkbox) {
  margin-right: 0;
  height: auto;
}

.kg-side h3 {
  margin: 12px 0 4px;
  font-size: 14px;
  color: var(--app-text-strong);
}

.kg-side h3:first-child {
  margin-top: 0;
}

.kg-full-btn {
  width: 100%;
}

.kg-mt {
  margin-top: 8px;
}

.kg-search-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  max-height: 220px;
  overflow: auto;
}

.kg-search-item {
  width: 100%;
  text-align: left;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid rgba(63, 179, 123, 0.25);
  border-radius: 8px;
  background: rgba(15, 52, 40, 0.2);
  color: inherit;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;

  strong {
    display: block;
    font-size: 13px;
  }

  small {
    display: block;
    margin-top: 2px;
    font-size: 11px;
    opacity: 0.75;
  }
}

.kg-search-item:hover {
  border-color: rgba(137, 242, 187, 0.55);
  background: rgba(22, 108, 80, 0.22);
}

.kg-chart-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  position: relative;
  background: radial-gradient(ellipse 90% 70% at 18% 12%, rgba(72, 187, 120, 0.22), transparent 52%),
    radial-gradient(ellipse 80% 60% at 88% 78%, rgba(56, 130, 200, 0.14), transparent 48%),
    radial-gradient(ellipse 50% 40% at 50% 100%, rgba(20, 90, 70, 0.35), transparent 55%),
    linear-gradient(168deg, rgba(7, 26, 22, 0.97) 0%, rgba(10, 42, 34, 0.99) 48%, rgba(6, 22, 28, 0.98) 100%);
  box-shadow: inset 0 0 80px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(94, 214, 138, 0.12);
}

.kg-chart-wrap--fs {
  min-height: 100vh;
  border-radius: 0;
}

.kg-chart-grid {
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.14;
  background-image: linear-gradient(rgba(120, 220, 170, 0.35) 1px, transparent 1px),
    linear-gradient(90deg, rgba(120, 220, 170, 0.35) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 75% 70% at 50% 45%, black 20%, transparent 100%);
  pointer-events: none;
  animation: kg-grid-drift 48s linear infinite;
}

@keyframes kg-grid-drift {
  from {
    background-position: 0 0, 0 0;
  }
  to {
    background-position: 48px 48px, 48px 48px;
  }
}

.kg-chart-vignette {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  box-shadow: inset 0 0 120px rgba(0, 0, 0, 0.55);
  border-radius: inherit;
}

.kg-chart-hud {
  position: absolute;
  left: 12px;
  top: 12px;
  right: 52px;
  z-index: 3;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  pointer-events: none;
}

.kg-chart-hud__main {
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(6, 28, 24, 0.55);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(120, 220, 170, 0.22);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
  max-width: min(420px, 100%);
}

.kg-chart-hud__label {
  display: block;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(200, 255, 220, 0.45);
  margin-bottom: 2px;
}

.kg-chart-hud__center {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: rgba(240, 255, 248, 0.96);
  line-height: 1.35;
  text-shadow: 0 0 24px rgba(110, 255, 170, 0.25);
}

.kg-chart-hud__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.kg-chip {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(94, 214, 138, 0.12);
  border: 1px solid rgba(94, 214, 138, 0.22);
  color: rgba(200, 255, 220, 0.88);
}

.kg-chip--accent {
  background: rgba(126, 182, 255, 0.2);
  border-color: rgba(126, 182, 255, 0.45);
  color: rgba(210, 230, 255, 0.95);
}

.kg-chart-hud__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  pointer-events: auto;
}

.kg-chart-hud__actions :deep(.el-button) {
  --el-button-bg-color: rgba(8, 40, 32, 0.75);
  --el-button-border-color: rgba(120, 220, 170, 0.35);
  --el-button-text-color: rgba(230, 255, 240, 0.92);
  backdrop-filter: blur(8px);
}

.kg-chart {
  position: relative;
  z-index: 2;
  flex: 1;
  min-height: 520px;
  width: 100%;
}

.kg-chart-hint {
  margin: 0;
  padding: 10px 16px 12px;
  font-size: 12px;
  line-height: 1.55;
  color: rgba(230, 255, 240, 0.55);
  text-align: center;
  border-top: 1px solid rgba(94, 214, 138, 0.12);
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.2));

  strong {
    color: rgba(160, 255, 200, 0.88);
    font-weight: 600;
  }

  kbd {
    display: inline-block;
    padding: 0 5px;
    font-size: 10px;
    border-radius: 4px;
    border: 1px solid rgba(160, 255, 200, 0.35);
    background: rgba(0, 0, 0, 0.25);
    color: rgba(200, 255, 220, 0.9);
    margin: 0 2px;
  }
}

.kg-edge-dl {
  margin: 0;
  dt {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin: 14px 0 4px;
    &:first-child {
      margin-top: 0;
    }
  }
  dd {
    margin: 0;
    font-size: 14px;
    color: var(--el-text-color-primary);
    word-break: break-word;
  }
}

.kg-edge-predicate {
  font-weight: 600;
  color: var(--el-color-primary);
}

.kg-node-head {
  margin-bottom: 12px;
}

.kg-node-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: var(--app-text-strong);
}

.kg-node-id {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--app-text-muted);
  word-break: break-all;
}

.kg-node-deg {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.kg-node-desc {
  font-size: 13px;
  line-height: 1.65;
  color: var(--app-text-primary);
  margin: 0 0 12px;
}

.kg-node-desc--muted {
  color: var(--app-text-muted);
  font-style: italic;
}

.kg-node-alias {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.kg-node-sec {
  margin: 14px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-strong);
}

.kg-rel-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 200px;
  overflow: auto;
}

.kg-rel-btn {
  width: 100%;
  text-align: left;
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 8px;
  border: 1px solid rgba(63, 179, 123, 0.22);
  background: rgba(15, 52, 40, 0.12);
  color: inherit;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
  font-size: 12px;
  line-height: 1.45;
}

.kg-rel-btn:hover {
  border-color: rgba(137, 242, 187, 0.45);
  background: rgba(22, 108, 80, 0.15);
}

.kg-rel-pred {
  display: block;
  font-weight: 600;
  color: var(--el-color-primary);
  margin-bottom: 2px;
}

.kg-node-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
