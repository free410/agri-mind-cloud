import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'
import { hasToken } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta?.title || '智慧农业管理系统'} - 智慧农业管理系统`

  // 未登录用户直接访问根路径，重定向到登录页（解决首次进入闪现管理页）
  if (to.path === '/' && !hasToken()) {
    next('/login')
    return
  }

  if (to.path === '/login' && hasToken()) {
    next('/dashboard')
    return
  }

  if (to.meta?.requiresAuth && !hasToken()) {
    next('/login')
    return
  }

  next()
})

export default router
