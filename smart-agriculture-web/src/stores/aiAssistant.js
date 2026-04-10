import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { getUserInfo } from '@/utils/storage'

const STORAGE_PREFIX = 'smart-agri-ai-chat'
const SCHEMA_V = 1
/** 控制 localStorage 体积；仅持久化最近若干条 */
const MAX_PERSISTED_MESSAGES = 200

const VALID_MODES = new Set(['auto', 'business', 'knowledge', 'hybrid'])

function storageKeyForUser(userId) {
  const id = userId != null && userId !== '' ? String(userId) : 'guest'
  return `${STORAGE_PREFIX}:${id}`
}

function currentUserId() {
  return getUserInfo()?.id
}

function formatMessageTime() {
  const now = new Date()
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

function computeNextId(msgs) {
  if (!msgs?.length) return 1
  let max = 0
  for (const m of msgs) {
    if (typeof m?.id === 'number' && m.id > max) max = m.id
  }
  return max + 1
}

function loadSnapshot(userId) {
  try {
    const raw = localStorage.getItem(storageKeyForUser(userId))
    if (!raw) return null
    const data = JSON.parse(raw)
    if (!data || data.v !== SCHEMA_V || !Array.isArray(data.messages)) return null
    const mode = VALID_MODES.has(data.currentMode) ? data.currentMode : 'auto'
    const messages = data.messages.slice(-MAX_PERSISTED_MESSAGES)
    return {
      messages,
      currentMode: mode,
      nextMessageId: computeNextId(messages)
    }
  } catch {
    return null
  }
}

/**
 * AI 助手会话：路由切换不丢；刷新页面后从 localStorage 恢复（按当前登录管理员 id 分键存储）。
 */
export const useAiAssistantStore = defineStore('aiAssistant', () => {
  const snap = loadSnapshot(currentUserId())
  const messages = ref(snap?.messages ?? [])
  const currentMode = ref(snap?.currentMode ?? 'auto')
  let nextMessageId = snap?.nextMessageId ?? 1

  function persist() {
    try {
      const uid = currentUserId()
      const toSave = messages.value.slice(-MAX_PERSISTED_MESSAGES)
      localStorage.setItem(
        storageKeyForUser(uid),
        JSON.stringify({
          v: SCHEMA_V,
          messages: toSave,
          currentMode: currentMode.value,
          nextMessageId: computeNextId(messages.value)
        })
      )
    } catch (e) {
      console.warn('[aiAssistant] 写入 localStorage 失败', e)
    }
  }

  function appendMessage(role, content, structured = null) {
    const id = nextMessageId++
    messages.value.push({
      id,
      role,
      content,
      time: formatMessageTime(),
      structured
    })
    persist()
    return id
  }

  /**
   * @param {string|number|undefined} explicitUserId 登出/401 时已清空 localStorage 用户信息，需传入清前用户 id
   */
  function clearSession(explicitUserId) {
    const id = explicitUserId != null ? explicitUserId : currentUserId()
    messages.value = []
    nextMessageId = 1
    currentMode.value = 'auto'
    try {
      localStorage.removeItem(storageKeyForUser(id))
    } catch {
      /* ignore */
    }
  }

  function rehydrateFromStorage() {
    const next = loadSnapshot(currentUserId())
    if (!next) {
      messages.value = []
      currentMode.value = 'auto'
      nextMessageId = 1
      return
    }
    messages.value = next.messages
    currentMode.value = next.currentMode
    nextMessageId = next.nextMessageId
  }

  watch(currentMode, () => {
    persist()
  })

  return {
    messages,
    currentMode,
    appendMessage,
    clearSession,
    rehydrateFromStorage
  }
})
