import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'
import { hasToken } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta?.title || '智慧农业管理系统'} - 智慧农业管理系统`

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
