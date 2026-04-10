import request from '@/utils/request'

export function getKnowledgeGraphStatsApi() {
  return request({ url: '/knowledge-graph/stats', method: 'get' })
}

export function searchKnowledgeGraphApi(params) {
  return request({ url: '/knowledge-graph/search', method: 'get', params })
}

export function getKnowledgeGraphSubgraphApi(params) {
  return request({ url: '/knowledge-graph/subgraph', method: 'get', params })
}

export function seedKnowledgeGraphDemoApi() {
  return request({ url: '/knowledge-graph/seed-demo', method: 'post', data: {} })
}

/** 批量导入：body 为 { triples: [...], entities?: { id: { description, aliases } } }（需登录） */
export function importKnowledgeGraphBundleApi(data) {
  return request({
    url: '/knowledge-graph/import',
    method: 'post',
    data,
    timeout: 120000
  })
}

/** 实体详情：description、aliases、一度关系、度 */
export function getKnowledgeGraphEntityApi(id) {
  return request({ url: `/knowledge-graph/entity/${encodeURIComponent(id)}`, method: 'get' })
}

/** 最短路径子图 */
export function getKnowledgeGraphPathApi(params) {
  return request({ url: '/knowledge-graph/path', method: 'get', params })
}
