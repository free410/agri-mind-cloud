<template>
  <article class="field-card">
    <div class="card-head">
      <div class="card-head__main">
        <h3>{{ field.fieldName || '未命名农田' }}</h3>
        <p>{{ field.location || '暂无位置说明' }}</p>
      </div>
      <el-tag :type="statusTagType" effect="plain">{{ statusLabel }}</el-tag>
    </div>

    <div class="card-metrics">
      <div class="metric-item">
        <span>面积</span>
        <strong>{{ formatNumber(field.area, ' 亩', 2) }}</strong>
      </div>
      <div class="metric-item">
        <span>土壤类型</span>
        <strong>{{ field.soilType || '--' }}</strong>
      </div>
    </div>

    <div class="card-risk" :class="riskLevel">
      <span class="risk-dot"></span>
      <span>{{ riskText }}</span>
    </div>

    <div class="card-footer">
      <div class="footer-meta">
        <span>创建时间</span>
        <strong>{{ field.createTime || '--' }}</strong>
      </div>
      <div class="card-actions">
        <el-button link type="primary" @click="$emit('view', field)">查看</el-button>
        <el-button link type="primary" @click="$emit('edit', field)">编辑</el-button>
        <el-button link type="danger" @click="$emit('delete', field)">删除</el-button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { fieldStatusOptions, formatNumber, getOptionLabel, getOptionTagType } from '@/utils/agriDisplay'

const props = defineProps({
  field: {
    type: Object,
    required: true
  }
})

defineEmits(['view', 'edit', 'delete'])

const statusLabel = computed(() => props.field.statusName || getOptionLabel(fieldStatusOptions, props.field.status))
const statusTagType = computed(() => getOptionTagType(fieldStatusOptions, props.field.status))

const riskLevel = computed(() => {
  if (props.field.status === 2) return 'medium'
  if ((props.field.remark || '').includes('预警') || (props.field.remark || '').includes('风险')) return 'high'
  return 'low'
})

const riskText = computed(() => {
  if (riskLevel.value === 'high') return '重点关注'
  if (riskLevel.value === 'medium') return '维护关注'
  return '整体稳定'
})
</script>

<style scoped lang="scss">
.field-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 248px;
  padding: 20px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 20px;
  background: #fff;
  box-shadow: var(--app-shadow-xs);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.field-card:hover {
  transform: translateY(-2px);
  border-color: rgba(23, 138, 98, 0.14);
  box-shadow: var(--app-shadow);
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.card-head__main {
  min-width: 0;

  h3 {
    margin: 0 0 8px;
    font-size: 17px;
    color: var(--app-text);
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.65;
    color: var(--app-text-secondary);
  }
}

.card-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  padding: 14px;
  border-radius: 16px;
  background: var(--app-panel-soft);

  span {
    display: block;
    margin-bottom: 8px;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    font-size: 17px;
    color: var(--app-text);
  }
}

.card-risk {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 600;
}

.risk-dot {
  width: 8px;
  height: 8px;
  flex-shrink: 0;
  border-radius: 50%;
}

.card-risk.low {
  color: var(--app-primary-dark);
  background: var(--app-primary-soft);
}

.card-risk.low .risk-dot {
  background: var(--app-primary);
}

.card-risk.medium {
  color: #9b6a08;
  background: var(--app-accent-soft);
}

.card-risk.medium .risk-dot {
  background: var(--app-accent);
}

.card-risk.high {
  color: var(--app-danger);
  background: var(--app-danger-soft);
}

.card-risk.high .risk-dot {
  background: var(--app-danger);
}

.card-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-top: auto;
}

.footer-meta {
  span {
    display: block;
    margin-bottom: 6px;
    font-size: 12px;
    color: var(--app-text-secondary);
  }

  strong {
    font-size: 13px;
    color: var(--app-text);
  }
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .card-metrics {
    grid-template-columns: 1fr;
  }

  .card-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
