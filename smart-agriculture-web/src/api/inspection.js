import request from '@/utils/request'

export function getInspectionSummaryApi(params) {
  return request({
    url: '/inspection/tasks/summary',
    method: 'get',
    params
  })
}

export function runInspectionOnceApi() {
  return request({
    url: '/inspection/tasks/run-once',
    method: 'post'
  })
}

