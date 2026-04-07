import request from '@/utils/request'

export function getFieldPageApi(params) {
  return request({
    url: '/fields/page',
    method: 'get',
    params
  })
}

export function getFieldDetailApi(id) {
  return request({
    url: `/fields/${id}`,
    method: 'get'
  })
}

export function createFieldApi(data) {
  return request({
    url: '/fields',
    method: 'post',
    data
  })
}

export function updateFieldApi(id, data) {
  return request({
    url: `/fields/${id}`,
    method: 'put',
    data
  })
}

export function deleteFieldApi(id) {
  return request({
    url: `/fields/${id}`,
    method: 'delete'
  })
}
