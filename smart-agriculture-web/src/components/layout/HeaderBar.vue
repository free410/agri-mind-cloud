<template>
  <header class="header-bar page-card">
    <div class="header-left">
      <div class="header-title">
        <h2>{{ currentTitle }}</h2>
        <p>{{ currentDateText }}</p>
      </div>
    </div>

    <div class="header-right">
      <div class="status-pill">
        <span class="dot"></span>
        <span>比赛演示模式</span>
      </div>

      <div class="status-pill subtle">
        <span class="dot neutral"></span>
        <span>本地环境</span>
      </div>

      <el-dropdown @command="handleCommand">
        <div class="user-card">
          <div class="avatar">{{ avatarText }}</div>
          <div class="user-meta">
            <strong>{{ displayName }}</strong>
            <span>系统管理员</span>
          </div>
          <el-icon class="user-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentTitle = computed(() => route.meta?.title || '智慧农业管理系统')
const currentDateText = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})

const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员')
const avatarText = computed(() => displayName.value.slice(0, 1))

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }

  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出当前登录状态吗？', '退出提示', {
      type: 'warning'
    })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 68px;
  padding: 14px 20px;
  border-color: rgba(17, 82, 62, 0.08);
  background: rgba(255, 255, 255, 0.92);
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.header-title h2 {
  margin: 0 0 4px;
  font-size: 20px;
  line-height: 1.2;
  color: var(--app-text);
}

.header-title p {
  margin: 0;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(23, 138, 98, 0.12);
  border-radius: 999px;
  background: rgba(23, 138, 98, 0.07);
  color: var(--app-primary-dark);
  font-size: 12px;
  font-weight: 600;
}

.status-pill.subtle {
  border-color: rgba(17, 82, 62, 0.08);
  background: rgba(17, 82, 62, 0.04);
  color: var(--app-text-secondary);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #20b56a;
}

.dot.neutral {
  background: var(--app-tech);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 7px 10px 7px 8px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.user-card:hover {
  transform: translateY(-1px);
  border-color: rgba(23, 138, 98, 0.14);
  box-shadow: var(--app-shadow-xs);
}

.avatar {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #1a8e63, #3fa378);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.user-meta {
  display: flex;
  min-width: 88px;
  flex-direction: column;
}

.user-meta strong {
  font-size: 13px;
  line-height: 1.3;
  color: var(--app-text);
}

.user-meta span {
  font-size: 11px;
  line-height: 1.4;
  color: var(--app-text-secondary);
}

.user-arrow {
  color: var(--app-text-tertiary);
}

@media (max-width: 960px) {
  .header-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
