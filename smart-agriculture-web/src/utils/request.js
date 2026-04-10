import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuth } from '@/utils/auth'
import { getToken, getUserInfo } from '@/utils/storage'
import { useAiAssistantStore } from '@/stores/aiAssistant'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || 'http://localhost:8080/api',
  timeout: 10000
})

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(res)
    }
    return res.data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      const adminId = getUserInfo()?.id
      clearAuth()
      try {
        useAiAssistantStore().clearSession(adminId)
      } catch {
        /* Pinia 未就绪时忽略 */
      }
      window.location.href = '/login'
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
