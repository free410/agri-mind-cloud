import request from '@/utils/request'

export function getEnvironmentDataPageApi(params) {
  return request({
    url: '/environment-data/page',
    method: 'get',
    params
  })
}

export function getEnvironmentTrendApi(params) {
  return request({
    url: '/environment-data/trend',
    method: 'get',
    params
  })
}
