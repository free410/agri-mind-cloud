<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  text: { type: String, default: '' },
  /** 为 true 时从空串逐字显示；为 false 时立即显示全文（历史消息、动画结束） */
  active: { type: Boolean, default: false },
  msPerChar: { type: Number, default: 26 },
  /** 每输出若干字通知父级滚动，避免长文顶出视口 */
  progressEvery: { type: Number, default: 8 }
})

const emit = defineEmits(['complete', 'progress'])

const shown = ref('')
let timer = null

function clearTimer() {
  if (timer != null) {
    clearTimeout(timer)
    timer = null
  }
}

function run() {
  clearTimer()
  const full = props.text ?? ''
  if (!props.active) {
    shown.value = full
    return
  }
  if (!full.length) {
    shown.value = ''
    emit('complete')
    return
  }
  shown.value = ''
  let i = 0
  const step = () => {
    if (i >= full.length) {
      emit('complete')
      return
    }
    i += 1
    shown.value = full.slice(0, i)
    if (props.progressEvery > 0 && i % props.progressEvery === 0) {
      emit('progress')
    }
    timer = setTimeout(step, props.msPerChar)
  }
  step()
}

watch(() => [props.text, props.active], run, { immediate: true })

onUnmounted(() => clearTimer())
</script>

<template>
  <span class="typewriter-text">
    {{ shown }}<span
      v-if="active && shown.length < (text?.length ?? 0)"
      class="typewriter-text__caret"
      aria-hidden="true"
    />
  </span>
</template>

<style scoped>
.typewriter-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.typewriter-text__caret {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 1px;
  vertical-align: -0.12em;
  background: currentColor;
  opacity: 0.55;
  animation: typewriter-blink 0.9s step-end infinite;
}

@keyframes typewriter-blink {
  50% {
    opacity: 0;
  }
}
</style>
