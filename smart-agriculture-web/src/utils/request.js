import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuth } from '@/utils/auth'
import { getToken, getUserInfo } from '@/utils/storage'
import { useAiAssistantStore } from '@/stores/aiAssistant'

function resolveBaseUrl() {
  const raw = String(import.meta.env.VITE_APP_BASE_API || '').trim()
  // 默认走同源 /api：开发环境由 Vite 代理转发，生产环境由 Nginx 反向代理转发
  if (!raw) {
    return '/api'
  }
  // 容错：有些环境会误配成 ":8080/api"，补齐本机主机名
  if (raw.startsWith(':')) {
    return `http://localhost${raw}`
  }
  return raw
}

let redirectingToLogin = false

function handleUnauthorized() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  const adminId = getUserInfo()?.id
  clearAuth()
  try {
    useAiAssistantStore().clearSession(adminId)
  } catch {
    /* Pinia 未就绪时忽略 */
  }
  window.location.href = '/login'
}

const service = axios.create({
  baseURL: resolveBaseUrl(),
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
    if (res.code === 401) {
      ElMessage.error(res.message || '登录状态已失效，请重新登录')
      handleUnauthorized()
      return Promise.reject(res)
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(res)
    }
    return res.data
  },
  (error) => {
    const status = error.response?.status
    const businessCode = error.response?.data?.code
    // 登录接口自身的 401 是"账号或密码错误"，不能触发登录态失效跳转，否则页面会直接刷新导致看不到提示
    const isLoginRequest = String(error.config?.url || '').includes('/auth/login')
    if (isLoginRequest) {
      ElMessage.error(error.response?.data?.message || '账号或密码错误')
      return Promise.reject(error)
    }
    if (status === 401 || businessCode === 401) {
      ElMessage.error(error.response?.data?.message || '登录状态已失效，请重新登录')
      handleUnauthorized()
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
