<template>
  <article class="metric-card" :class="[`tone-${tone}`]">
    <div class="metric-head">
      <span class="metric-title">{{ title }}</span>
      <div class="metric-icon">
        <el-icon><component :is="icon" /></el-icon>
      </div>
    </div>

    <div class="metric-main">
      <strong class="metric-value">{{ value }}</strong>
      <el-tag v-if="tag" size="small" effect="plain" :type="tagType">{{ tag }}</el-tag>
    </div>

    <p class="metric-desc">{{ desc }}</p>

    <div class="metric-fill">
      <div class="metric-spark">
        <span
          v-for="(item, index) in sparkValues"
          :key="`${title}-${index}`"
          :style="{ height: `${item}%` }"
        ></span>
      </div>
      <div class="metric-progress">
        <div class="metric-progress__meta">
          <span>{{ progressLabel }}</span>
          <strong>{{ normalizedProgress }}%</strong>
        </div>
        <div class="metric-progress__track">
          <div class="metric-progress__bar" :style="{ width: `${normalizedProgress}%` }"></div>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  value: {
    type: [String, Number],
    default: ''
  },
  icon: {
    type: [Object, Function],
    required: true
  },
  tone: {
    type: String,
    default: 'primary'
  },
  tag: {
    type: String,
    default: ''
  },
  tagType: {
    type: String,
    default: 'success'
  },
  desc: {
    type: String,
    default: ''
  },
  progress: {
    type: Number,
    default: 68
  },
  progressLabel: {
    type: String,
    default: '完成率'
  },
  trendValues: {
    type: Array,
    default: () => []
  }
})

const normalizedProgress = computed(() => {
  const value = Number(props.progress || 0)
  return Math.max(0, Math.min(100, Math.round(value)))
})

const sparkValues = computed(() => {
  const values = props.trendValues?.map((item) => Number(item || 0)).filter((item) => Number.isFinite(item)) || []
  if (values.length >= 8) {
    const max = Math.max(...values, 1)
    return values.slice(-12).map((item) => Math.max(18, Math.min(92, Math.round((item / max) * 90))))
  }

  const seed = Math.max(Number(props.value) || 6, 6)
  return Array.from({ length: 12 }, (_, index) => {
    const wave = Math.sin((index / 11) * Math.PI * 1.8) * 18
    const drift = ((index + seed) % 5) * 4
    return Math.max(20, Math.min(88, Math.round(36 + wave + drift)))
  })
})
</script>

<style scoped>
.metric-card {
  display: flex;
  min-height: 176px;
  flex-direction: column;
  gap: 10px;
  padding: 16px 18px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 18px;
  background: #fff;
  box-shadow: var(--app-shadow-xs);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.metric-card:hover {
  transform: translateY(-1px);
  border-color: rgba(23, 138, 98, 0.12);
  box-shadow: var(--app-shadow);
}

.metric-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.metric-title {
  font-size: 13px;
  color: var(--app-text-secondary);
}

.metric-icon {
  display: flex;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-size: 18px;
}

.metric-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-value {
  display: block;
  font-size: 28px;
  line-height: 1;
  color: var(--app-text);
}

.metric-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--app-text-tertiary);
}

.metric-fill {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: 12px;
  margin-top: auto;
}

.metric-spark {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 34px;
  padding: 6px 8px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 10px;
  background: #f9fbfa;
}

.metric-spark span {
  flex: 1;
  min-width: 3px;
  border-radius: 999px;
  opacity: 0.82;
  background: currentColor;
}

.metric-progress {
  display: grid;
  gap: 6px;
}

.metric-progress__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: var(--app-text-secondary);
}

.metric-progress__meta strong {
  font-size: 12px;
  color: var(--app-text);
}

.metric-progress__track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #ebf1ee;
}

.metric-progress__bar {
  height: 100%;
  border-radius: inherit;
  background: currentColor;
  transition: width 0.3s ease;
}

.tone-primary .metric-icon {
  color: var(--app-primary);
  background: var(--app-primary-soft);
}

.tone-tech .metric-icon {
  color: var(--app-tech);
  background: var(--app-tech-soft);
}

.tone-accent .metric-icon {
  color: #a7740a;
  background: var(--app-accent-soft);
}

.tone-danger .metric-icon {
  color: var(--app-danger);
  background: var(--app-danger-soft);
}

.tone-ai .metric-icon {
  color: #127590;
  background: linear-gradient(135deg, rgba(76, 139, 245, 0.12), rgba(15, 118, 110, 0.08));
}

.tone-primary .metric-spark,
.tone-primary .metric-progress__track {
  color: var(--app-primary);
}

.tone-tech .metric-spark,
.tone-tech .metric-progress__track {
  color: var(--app-tech);
}

.tone-accent .metric-spark,
.tone-accent .metric-progress__track {
  color: #df9d24;
}

.tone-danger .metric-spark,
.tone-danger .metric-progress__track {
  color: var(--app-danger);
}

.tone-ai .metric-spark,
.tone-ai .metric-progress__track {
  color: #1f79c9;
}

@media (max-width: 1480px) {
  .metric-card {
    min-height: 168px;
  }
}
</style>
