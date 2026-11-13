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
  position: relative;
  z-index: 1;
  display: flex;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  gap: var(--app-grid-gap);
  padding: var(--app-content-padding-x);
  align-items: stretch;
}

.layout-aside {
  width: 248px;
  flex: 0 0 248px;
  height: 100%;
  min-height: 0;
  position: relative;
  padding: 12px 10px;
  background: var(--app-menu-bg);
  border: 1px solid rgba(255, 255, 255, 0.10);
  box-shadow: 0 10px 24px rgba(15, 52, 40, 0.16);
  overflow: visible;
  transition:
    width 0.24s ease,
    flex-basis 0.24s ease,
    padding 0.24s ease;
}

/* 菜单在侧边栏内部滚动，侧边栏本身固定在视口 */
.layout-aside :deep(.aside-menu) {
  height: 100%;
  min-height: 0;
}

.layout-aside :deep(.menu-panel) {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
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
  height: 100%;
  min-height: 0;
  overflow: hidden;
  flex-direction: column;
  gap: var(--app-card-content-gap);
  border-radius: var(--app-radius);
}

.header-wrapper {
  width: 100%;
  max-width: none;
  margin: 0;
  flex-shrink: 0;
  backdrop-filter: saturate(110%) blur(1.2px);
}

.layout-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-wrapper {
  width: 100%;
  max-width: none;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  container-type: inline-size;
  container-name: app-content;
  margin: 0;
  padding: var(--app-content-padding);
  overflow-y: auto;
  overscroll-behavior: contain;
  background: rgba(255, 255, 255, 0.90);
  border: 1px solid rgba(17, 82, 62, 0.08);
  box-shadow: var(--app-shadow);
  backdrop-filter: blur(1.6px) saturate(108%);
}

@media (max-width: 1024px) {
  .layout-shell {
    height: auto;
    min-height: 100vh;
    overflow: visible;
    flex-direction: column;
  }

  .layout-aside {
    width: 100%;
    height: auto;
    flex: none;
    overflow: visible;
  }

  .layout-aside :deep(.menu-panel) {
    overflow: visible;
  }

  .aside-toggle {
    display: none;
  }

  .layout-main,
  .layout-content,
  .content-wrapper {
    height: auto;
    overflow: visible;
  }
}
</style>
