import request from '@/utils/request'

export function loginApi(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getCurrentUserApi() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

export function logoutApi() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function updatePasswordApi(data) {
  return request({
    url: '/auth/password',
    method: 'put',
    data
  })
}
