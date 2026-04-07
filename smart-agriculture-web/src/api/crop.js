import request from '@/utils/request'

export function getCropPageApi(params) {
  return request({
    url: '/crops/page',
    method: 'get',
    params
  })
}

export function getCropDetailApi(id) {
  return request({
    url: `/crops/${id}`,
    method: 'get'
  })
}

export function createCropApi(data) {
  return request({
    url: '/crops',
    method: 'post',
    data
  })
}

export function updateCropApi(id, data) {
  return request({
    url: `/crops/${id}`,
    method: 'put',
    data
  })
}

export function deleteCropApi(id) {
  return request({
    url: `/crops/${id}`,
    method: 'delete'
  })
}
