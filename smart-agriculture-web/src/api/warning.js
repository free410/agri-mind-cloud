import request from '@/utils/request'

export function getWarningPageApi(params) {
  return request({
    url: '/warnings/page',
    method: 'get',
    params
  })
}

export function getWarningDetailApi(id) {
  return request({
    url: `/warnings/${id}`,
    method: 'get'
  })
}

export function getWarningFlowApi(id) {
  return request({
    url: `/warnings/${id}/flow`,
    method: 'get'
  })
}

export function updateWarningStatusApi(data) {
  return request({
    url: '/warnings/status',
    method: 'put',
    data
  })
}

export function confirmWarningApi(id, data) {
  return request({
    url: `/warnings/${id}/confirm`,
    method: 'put',
    data
  })
}

export function markWarningProcessingApi(id, data) {
  return request({
    url: `/warnings/${id}/processing`,
    method: 'put',
    data
  })
}

export function submitWarningHandleResultApi(id, data) {
  return request({
    url: `/warnings/${id}/handle-result`,
    method: 'put',
    data
  })
}

export function closeWarningApi(id, data) {
  return request({
    url: `/warnings/${id}/close-loop`,
    method: 'put',
    data
  })
}

export function markWarningFalseAlarmApi(id, data) {
  return request({
    url: `/warnings/${id}/false-alarm`,
    method: 'put',
    data
  })
}

export function getWarningDisposalReportApi(id) {
  return request({
    url: `/warnings/${id}/disposal-report`,
    method: 'get'
  })
}
