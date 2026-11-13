<template>
  <div class="mini-graph" :class="{ 'mini-graph--expanded': expanded }">
    <div class="mini-graph__header" @click="toggleExpand">
      <div class="mini-graph__header-left">
        <svg class="mini-graph__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="5" r="3"/>
          <circle cx="5" cy="19" r="3"/>
          <circle cx="19" cy="19" r="3"/>
          <line x1="12" y1="8" x2="5" y2="16"/>
          <line x1="12" y1="8" x2="19" y2="16"/>
        </svg>
        <span class="mini-graph__title">知识图谱</span>
        <span class="mini-graph__stats" v-if="subgraph">
          {{ subgraph.nodes?.length || 0 }} 实体 · {{ subgraph.edges?.length || 0 }} 关系
        </span>
      </div>
      <div class="mini-graph__header-right">
        <span class="mini-graph__center" v-if="centerName">{{ centerName }}</span>
        <svg class="mini-graph__chevron" :class="{ 'mini-graph__chevron--up': expanded }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="6,9 12,15 18,9"/>
        </svg>
      </div>
    </div>

    <div class="mini-graph__body" v-show="expanded">
      <div ref="chartRef" class="mini-graph__chart"></div>
      <div class="mini-graph__legend">
        <span
          v-for="kind in activeKinds"
          :key="kind"
          class="mini-graph__legend-item"
        >
          <i class="mini-graph__legend-dot" :style="{ background: categoryColor(kind) }"></i>
          {{ kindLabel(kind) }}
        </span>
      </div>
    </div>

    <div class="mini-graph__collapsed" v-show="!expanded && subgraph">
      <div class="mini-graph__preview-chips">
        <span
          v-for="node in previewNodes"
          :key="node.id"
          class="mini-graph__node-chip"
          :style="{ borderColor: categoryColor(node.kind) }"
        >
          {{ node.name }}
        </span>
        <span v-if="moreCount > 0" class="mini-graph__more">+{{ moreCount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  subgraph: {
    type: Object,
    default: null
  }
})

const chartRef = ref(null)
const chart = shallowRef(null)
const expanded = ref(false)

const KIND_ORDER = [
  'crop', 'disease', 'pest', 'stress', 'practice',
  'environment', 'soil', 'nutrient', 'deficiency', 'fertilizer',
  'pesticide', 'growth_stage', 'symptom', 'input', 'concept', 'other'
]

const CATEGORY_PALETTE = [
  '#5FD68A', '#FF8B7A', '#FFC66D', '#7EB6FF', '#5CE1E6',
  '#B794F6', '#C4A574', '#9AE6B4', '#F6AD55', '#68D391',
  '#FC8181', '#76E4F7', '#F687B3', '#A0AEC0', '#E2E8F0', '#718096'
]

function categoryColor(kind) {
  const i = KIND_ORDER.indexOf(kind)
  return CATEGORY_PALETTE[i >= 0 ? i : KIND_ORDER.indexOf('other') % CATEGORY_PALETTE.length]
}

function kindLabel(kind) {
  const map = {
    crop: '作物', disease: '病害', pest: '虫害', stress: '胁迫',
    practice: '农艺', environment: '环境', soil: '土壤', nutrient: '养分',
    deficiency: '缺素', fertilizer: '肥料', pesticide: '农药',
    growth_stage: '生育期', symptom: '症状', input: '投入品',
    concept: '概念', other: '其他'
  }
  return map[kind] || kind
}

const centerName = computed(() => {
  if (!props.subgraph?.center_id) return ''
  const node = props.subgraph.nodes?.find(n => n.id === props.subgraph.center_id)
  return node?.name || props.subgraph.center_id
})

const previewNodes = computed(() => {
  if (!props.subgraph?.nodes) return []
  return props.subgraph.nodes.slice(0, 4)
})

const moreCount = computed(() => {
  if (!props.subgraph?.nodes) return 0
  return Math.max(0, props.subgraph.nodes.length - 4)
})

const activeKinds = computed(() => {
  if (!props.subgraph?.nodes) return []
  const kinds = new Set(props.subgraph.nodes.map(n => n.kind))
  return KIND_ORDER.filter(k => kinds.has(k))
})

function toggleExpand() {
  expanded.value = !expanded.value
  if (expanded.value) {
    nextTick(() => renderChart())
  }
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

function renderChart() {
  if (!chartRef.value || !props.subgraph) {
    return
  }

  if (!chart.value) {
    chart.value = echarts.init(chartRef.value)
  }

  const viz = props.subgraph
  const center = viz.center_id

  const nodes = (viz.nodes || []).map(n => {
    const isCenter = n.id === center
    const baseColor = CATEGORY_PALETTE[kindIndex(n.kind) % CATEGORY_PALETTE.length]
    return {
      id: n.id,
      name: n.name,
      category: kindIndex(n.kind),
      symbolSize: isCenter ? 45 : 28,
      itemStyle: {
        color: baseColor,
        borderColor: isCenter ? '#fff' : 'rgba(255,255,255,0.4)',
        borderWidth: isCenter ? 3 : 1,
        shadowBlur: isCenter ? 16 : 8,
        shadowColor: baseColor + '80'
      },
      label: {
        show: true,
        fontSize: isCenter ? 11 : 9,
        fontWeight: isCenter ? 700 : 500,
        color: '#fff'
      }
    }
  })

  const edgesData = (viz.edges || [])
  const links = edgesData.map(e => ({
    source: e.source,
    target: e.target,
    value: e.predicate,
    lineStyle: {
      width: 1.5,
      color: '#5FD68A80',
      curveness: 0.15
    },
    label: {
      show: edgesData.length <= 15,
      fontSize: 8,
      color: 'rgba(255,255,255,0.7)',
      formatter: e.predicate
    }
  }))

  chart.value.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(12, 38, 30, 0.95)',
      borderColor: 'rgba(94, 214, 138, 0.4)',
      textStyle: { color: '#e8fff2' },
      formatter: (p) => {
        if (p.dataType === 'edge') {
          return `${p.data.source} → ${p.data.target}<br/><b>${p.data.value}</b>`
        }
        return `<b>${p.data.name}</b><br/>${kindLabel(viz.nodes?.find(n => n.id === p.data.id)?.kind || 'other')}`
      }
    },
    series: [{
      type: 'graph',
      roam: true,
      draggable: true,
      scaleLimit: { min: 0.3, max: 3 },
      categories: buildCategories(),
      data: nodes,
      links: links,
      edgeSymbol: ['circle', 'arrow'],
      edgeSymbolSize: [4, 8],
      layout: 'force',
      force: {
        repulsion: 120,
        edgeLength: [50, 100],
        gravity: 0.1,
        layoutAnimation: true
      },
      lineStyle: { opacity: 0.7, curveness: 0.15 },
      emphasis: {
        focus: 'adjacency',
        scale: 1.15,
        lineStyle: { width: 3 }
      },
      animation: true,
      animationDuration: 800,
      animationEasingUpdate: 'cubicOut'
    }]
  }, true)
}

function resizeChart() {
  chart.value?.resize()
}

watch(() => props.subgraph, () => {
  if (expanded.value) {
    nextTick(() => renderChart())
  }
}, { deep: true })

onMounted(() => {
  if (expanded.value && props.subgraph) {
    nextTick(() => renderChart())
  }
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  chart.value?.dispose()
  chart.value = null
  window.removeEventListener('resize', resizeChart)
})
</script>

<style scoped lang="scss">
.mini-graph {
  margin-top: 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(10, 42, 34, 0.95), rgba(6, 28, 24, 0.98));
  border: 1px solid rgba(94, 214, 138, 0.2);
  overflow: hidden;
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(94, 214, 138, 0.35);
    box-shadow: 0 4px 20px rgba(94, 214, 138, 0.1);
  }

  &--expanded {
    border-color: rgba(94, 214, 138, 0.4);
  }
}

.mini-graph__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;

  &:hover {
    background: rgba(94, 214, 138, 0.05);
  }
}

.mini-graph__header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-graph__icon {
  width: 18px;
  height: 18px;
  color: #5FD68A;
}

.mini-graph__title {
  font-size: 13px;
  font-weight: 600;
  color: #e8fff2;
}

.mini-graph__stats {
  font-size: 11px;
  color: rgba(200, 255, 220, 0.6);
  padding: 2px 8px;
  background: rgba(94, 214, 138, 0.1);
  border-radius: 10px;
}

.mini-graph__header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-graph__center {
  font-size: 12px;
  font-weight: 500;
  color: #5FD68A;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-graph__chevron {
  width: 16px;
  height: 16px;
  color: rgba(200, 255, 220, 0.6);
  transition: transform 0.3s ease;

  &--up {
    transform: rotate(180deg);
  }
}

.mini-graph__body {
  padding: 8px 12px 12px;
  border-top: 1px solid rgba(94, 214, 138, 0.1);
}

.mini-graph__chart {
  height: 280px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.2);
}

.mini-graph__legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(94, 214, 138, 0.1);
}

.mini-graph__legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: rgba(200, 255, 220, 0.75);
}

.mini-graph__legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.mini-graph__collapsed {
  padding: 8px 14px 12px;
}

.mini-graph__preview-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mini-graph__node-chip {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.25);
  border: 1px solid;
  color: rgba(200, 255, 220, 0.85);
}

.mini-graph__more {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 12px;
  background: rgba(94, 214, 138, 0.1);
  color: rgba(200, 255, 220, 0.6);
}
</style>
