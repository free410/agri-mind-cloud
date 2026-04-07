import request from '@/utils/request'

export function getWarningRulePageApi(params) {
  return request({
    url: '/warning-rules/page',
    method: 'get',
    params
  })
}

export function getWarningRuleSummaryApi() {
  return request({
    url: '/warning-rules/summary',
    method: 'get'
  })
}

export function createWarningRuleApi(data) {
  return request({
    url: '/warning-rules',
    method: 'post',
    data
  })
}

export function updateWarningRuleApi(id, data) {
  return request({
    url: `/warning-rules/${id}`,
    method: 'put',
    data
  })
}

export function updateWarningRuleEnabledApi(id, data) {
  return request({
    url: `/warning-rules/${id}/enabled`,
    method: 'put',
    data
  })
}

export function deleteWarningRuleApi(id) {
  return request({
    url: `/warning-rules/${id}`,
    method: 'delete'
  })
}

