import request from '@/utils/request'

export function getDevicePageApi(params) {
  return request({
    url: '/devices/page',
    method: 'get',
    params
  })
}

export function getDeviceDetailApi(id) {
  return request({
    url: `/devices/${id}`,
    method: 'get'
  })
}

export function createDeviceApi(data) {
  return request({
    url: '/devices',
    method: 'post',
    data
  })
}

export function updateDeviceApi(id, data) {
  return request({
    url: `/devices/${id}`,
    method: 'put',
    data
  })
}

export function deleteDeviceApi(id) {
  return request({
    url: `/devices/${id}`,
    method: 'delete'
  })
}
