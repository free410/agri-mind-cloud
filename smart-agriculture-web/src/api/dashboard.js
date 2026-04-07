import request from '@/utils/request'

export function getDashboardHomeApi() {
  return request({
    url: '/dashboard/home',
    method: 'get'
  })
}
