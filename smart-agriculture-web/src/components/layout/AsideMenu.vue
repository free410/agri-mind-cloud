<template>
  <div class="aside-menu" :class="{ collapsed: props.collapsed }">
    <div class="brand-block">
      <div class="brand-logo">智农</div>
      <div v-show="!props.collapsed" class="brand-meta">
        <h1>智慧农业管理系统</h1>
        <p>Smart Agriculture Console</p>
      </div>
    </div>

    <el-menu
      router
      :collapse="props.collapsed"
      :collapse-transition="false"
      :default-active="route.path"
      class="menu-panel"
      background-color="transparent"
      text-color="rgba(255,255,255,0.80)"
      active-text-color="#ffffff"
    >
      <el-menu-item v-for="item in menuList" :key="item.path" :index="item.path" :title="item.meta.title">
        <el-icon><component :is="item.meta.icon" /></el-icon>
        <span v-show="!props.collapsed">{{ item.meta.title }}</span>
      </el-menu-item>
    </el-menu>

    <div v-show="!props.collapsed" class="aside-footer">
      <div class="footer-card">
        <p>今日目标</p>
        <strong>监测稳定、预警及时、分析可视</strong>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { adminRoutes } from '@/router/routes'

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const menuList = adminRoutes
</script>

<style scoped lang="scss">
.aside-menu {
  display: flex;
  height: 100%;
  flex-direction: column;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px 18px;
  color: #fff;
}

.brand-meta {
  min-width: 0;
}

.brand-meta h1 {
  margin: 0 0 2px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.25;
}

.brand-meta p {
  margin: 0;
  font-size: 11px;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.56);
}

.brand-logo {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #229267, #4cb887);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 8px 18px rgba(20, 93, 72, 0.16);
  font-size: 15px;
  font-weight: 800;
}

.menu-panel {
  flex: 1;
  padding: 4px 0;
  border-right: none;
}

:deep(.el-menu-item) {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  margin-bottom: 6px;
  padding: 0 12px !important;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  transition:
    transform 0.18s ease,
    background-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

:deep(.el-menu-item .el-icon) {
  margin-right: 0;
  font-size: 16px;
}

.aside-menu.collapsed .brand-block {
  justify-content: center;
  padding-top: 28px;
  padding-bottom: 12px;
}

.aside-menu.collapsed .brand-logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 14px;
}

.aside-menu.collapsed :deep(.el-menu) {
  border-right: none;
}

.aside-menu.collapsed :deep(.el-menu-item) {
  justify-content: center;
  gap: 0;
  padding: 0 !important;
  border-radius: 10px;
}

:deep(.el-menu-item:hover) {
  transform: translateX(1px);
  background: rgba(255, 255, 255, 0.045) !important;
}

.aside-menu.collapsed :deep(.el-menu-item:hover) {
  transform: none;
}

:deep(.el-menu-item.is-active) {
  background: rgba(42, 166, 116, 0.22);
  box-shadow: inset 0 0 0 1px rgba(101, 213, 151, 0.18);
}

.aside-footer {
  padding: 12px 6px 4px;
}

.footer-card {
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
}

.footer-card p {
  margin: 0 0 6px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.56);
}

.footer-card strong {
  display: block;
  font-size: 12px;
  line-height: 1.6;
  font-weight: 600;
}
</style>
