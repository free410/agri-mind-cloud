import request from '@/utils/request'

export function sendAiChatApi(data) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data
  })
}

export function analyzeAiApi(data) {
  return request({
    url: '/ai/analyze',
    method: 'post',
    data
  })
}

export function getWarningAiAnalysisApi(warningId) {
  return request({
    url: '/ai/warning/analysis',
    method: 'get',
    params: { warningId }
  })
}

export function getWarningDisposalSuggestionApi(warningId) {
  return request({
    url: '/ai/warning/disposal',
    method: 'get',
    params: { warningId }
  })
}

export function getWarningQuickSummaryApi(mode = 'abnormal') {
  return request({
    url: '/ai/warning/quick-summary',
    method: 'get',
    params: { mode }
  })
}

export function sendAiRagChatApi(data) {
  return request({
    url: '/ai/rag/chat',
    method: 'post',
    data,
    // RAG：检索 + 大模型生成常超过全局 10s，单独放宽避免 axios 先超时
    timeout: 120000
  })
}
