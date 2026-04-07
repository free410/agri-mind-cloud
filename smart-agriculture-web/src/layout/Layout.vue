<template>
  <div class="layout-shell" :class="{ 'is-collapsed': isAsideCollapsed }">
    <aside class="layout-aside page-card">
      <button
        class="aside-toggle"
        :class="{ collapsed: isAsideCollapsed }"
        type="button"
        :title="isAsideCollapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="toggleAside"
      >
        <el-icon><component :is="isAsideCollapsed ? ArrowRightBold : ArrowLeftBold" /></el-icon>
      </button>
      <AsideMenu :collapsed="isAsideCollapsed" />
    </aside>
    <section class="layout-main">
      <div class="header-wrapper">
        <HeaderBar />
      </div>
      <main class="layout-content">
        <div class="content-wrapper page-card">
          <RouterView />
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowLeftBold, ArrowRightBold } from '@element-plus/icons-vue'
import AsideMenu from '@/components/layout/AsideMenu.vue'
import HeaderBar from '@/components/layout/HeaderBar.vue'

const isAsideCollapsed = ref(false)

function toggleAside() {
  isAsideCollapsed.value = !isAsideCollapsed.value
}
</script>

<style scoped lang="scss">
.layout-shell {
  display: flex;
  height: 100vh;
  gap: var(--app-grid-gap);
  padding: var(--app-content-padding-x);
  overflow: hidden;
}

.layout-aside {
  width: 248px;
  height: 100%;
  flex: 0 0 248px;
  position: relative;
  padding: 12px 10px;
  background: var(--app-menu-bg);
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 12px 30px rgba(12, 36, 29, 0.14);
  overflow: visible;
  transition:
    width 0.24s ease,
    flex-basis 0.24s ease,
    padding 0.24s ease;
}

.layout-shell.is-collapsed .layout-aside {
  width: 88px;
  flex-basis: 88px;
  padding: 12px 8px;
}

.aside-toggle {
  position: absolute;
  top: 82px;
  right: -15px;
  z-index: 6;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(137, 242, 187, 0.45);
  border-radius: 50%;
  background: linear-gradient(135deg, #29a770, #1f8f5f);
  color: #effff7;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    transform 0.22s ease,
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.22s ease;
  box-shadow:
    0 6px 16px rgba(22, 108, 80, 0.35),
    0 0 0 4px rgba(63, 179, 123, 0.12);
}

.aside-toggle::before {
  content: '';
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 1px solid rgba(144, 240, 194, 0.28);
  pointer-events: none;
}

.aside-toggle:hover {
  transform: translateX(1px) scale(1.04);
  border-color: rgba(169, 247, 207, 0.62);
  box-shadow:
    0 8px 20px rgba(22, 108, 80, 0.42),
    0 0 0 6px rgba(63, 179, 123, 0.16);
}

.aside-toggle:active {
  transform: translateX(0) scale(0.97);
}

.aside-toggle :deep(svg) {
  font-size: 13px;
  transition: transform 0.2s ease;
}

.aside-toggle.collapsed :deep(svg) {
  transform: translateX(1px);
}

.layout-main {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  gap: var(--app-card-content-gap);
  overflow: hidden;
}

.header-wrapper {
  width: 100%;
  max-width: none;
  margin: 0;
}

.layout-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
}

.content-wrapper {
  width: 100%;
  max-width: none;
  min-height: 100%;
  margin: 0;
  padding: var(--app-content-padding);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: var(--app-shadow);
}

@media (max-width: 1024px) {
  .layout-shell {
    height: auto;
    flex-direction: column;
    overflow: visible;
  }

  .layout-aside {
    width: 100%;
    height: auto;
    flex: none;
    overflow: visible;
  }

  .aside-toggle {
    display: none;
  }

  .layout-main,
  .layout-content {
    overflow: visible;
  }
}
</style>
