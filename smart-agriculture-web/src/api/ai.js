import request from '@/utils/request'

export function sendAiChatApi(data) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data,
    // 真实大模型响应可能超过默认 10s，避免前端提前超时
    timeout: 120000
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

export function uploadKnowledgeApi(file, category = 'crop') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  return request({
    url: '/ai/rag/knowledge/upload',
    method: 'post',
    data: formData,
    // 不手动设置 Content-Type，交给浏览器自动补 multipart boundary
    // 上传后会触发切片 + 向量化，耗时较长
    timeout: 300000
  })
}

export function fetchKnowledgeDocumentsApi() {
  return request({
    url: '/ai/rag/knowledge/documents',
    method: 'get',
    timeout: 60000
  })
}

export function deleteKnowledgeDocumentApi(path) {
  return request({
    url: '/ai/rag/knowledge/documents',
    method: 'delete',
    params: { path },
    timeout: 60000
  })
}
