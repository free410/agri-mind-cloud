export const adminRoutes = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/Dashboard.vue'),
    meta: { title: '首页看板', icon: 'DataBoard' }
  },
  {
    path: '/field',
    name: 'FieldManage',
    component: () => import('@/views/field/FarmlandManage.vue'),
    meta: { title: '农田管理', icon: 'MapLocation' }
  },
  {
    path: '/crop',
    name: 'CropManage',
    component: () => import('@/views/crop/CropManage.vue'),
    meta: { title: '作物管理', icon: 'Cherry' }
  },
  {
    path: '/device',
    name: 'DeviceManage',
    component: () => import('@/views/device/DeviceManage.vue'),
    meta: { title: '设备管理', icon: 'Monitor' }
  },
  {
    path: '/environment',
    name: 'EnvironmentMonitor',
    component: () => import('@/views/environment/EnvironmentMonitor.vue'),
    meta: { title: '环境监测', icon: 'TrendCharts' }
  },
  {
    path: '/warning',
    name: 'WarningManage',
    component: () => import('@/views/warning/WarningManage.vue'),
    meta: { title: '预警管理', icon: 'Warning' }
  },
  {
    path: '/warning-rule-center',
    name: 'WarningRuleCenter',
    component: () => import('@/views/warning/WarningRuleCenter.vue'),
    meta: { title: '规则中心', icon: 'Operation' }
  },
  {
    path: '/ai',
    name: 'AiAssistant',
    component: () => import('@/views/ai/AiAssistant.vue'),
    meta: { title: 'AI 助手', icon: 'ChatDotRound' }
  },
  {
    path: '/knowledge-graph',
    name: 'KnowledgeGraph',
    component: () => import('@/views/knowledge/KnowledgeGraphExplorer.vue'),
    meta: { title: '知识图谱', icon: 'Share' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/Profile.vue'),
    meta: { title: '个人中心', icon: 'User' }
  }
]

export default [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '管理员登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: adminRoutes
  }
]

