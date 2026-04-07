<template>
  <div class="page-shell profile-page">
    <section class="page-banner">
      <div class="page-banner__main">
        <span class="page-banner__badge">系统管理</span>
        <h1 class="page-banner__title">个人中心</h1>
        <p class="page-banner__desc">
          统一查看管理员基础资料、账号状态与最近登录信息，并在当前页面直接完成密码维护。
        </p>
      </div>
    </section>

    <section class="profile-main-grid">
      <el-card class="page-content-card profile-info-card" shadow="never">
        <template #header>
          <div class="page-panel-head">
            <div class="page-panel-title">
              <strong>管理员基本信息</strong>
              <span class="page-panel-tip">当前信息来自后端 `/auth/me` 接口，展示系统内真实管理员资料。</span>
            </div>
          </div>
        </template>

        <div v-loading="loading" class="profile-info-body">
          <div class="profile-identity">
            <div class="avatar-box">
              {{ displayInitial }}
            </div>

            <div class="identity-meta">
              <strong>{{ userInfo.nickname || '系统管理员' }}</strong>
              <span>{{ userInfo.username || '--' }}</span>
              <div class="identity-tags">
                <el-tag type="success" effect="light">管理员账号</el-tag>
                <el-tag type="info" effect="light">本地演示环境</el-tag>
              </div>
            </div>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span>账号</span>
              <strong>{{ userInfo.username || '--' }}</strong>
            </div>
            <div class="info-item">
              <span>昵称</span>
              <strong>{{ userInfo.nickname || '--' }}</strong>
            </div>
            <div class="info-item">
              <span>手机号</span>
              <strong>{{ userInfo.phone || '--' }}</strong>
            </div>
            <div class="info-item">
              <span>邮箱</span>
              <strong>{{ userInfo.email || '--' }}</strong>
            </div>
            <div class="info-item wide">
              <span>最近登录</span>
              <strong>{{ userInfo.lastLoginTime || '--' }}</strong>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="page-content-card profile-password-card" shadow="never">
        <template #header>
          <div class="page-panel-head">
            <div class="page-panel-title">
              <strong>修改密码</strong>
              <span class="page-panel-tip">直接调用后端 `/auth/password` 接口，完成当前管理员密码维护。</span>
            </div>
          </div>
        </template>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="92px"
          class="password-form"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="formData.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前登录密码"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="formData.newPassword"
              type="password"
              show-password
              placeholder="请输入新的登录密码"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="formData.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新的登录密码"
            />
          </el-form-item>

          <div class="password-tip-list">
            <div class="password-tip-item">
              <span>维护建议</span>
              <strong>建议使用 6 到 20 位组合密码</strong>
            </div>
            <div class="password-tip-item">
              <span>安全提示</span>
              <strong>修改后请使用新密码重新登录</strong>
            </div>
            <div class="password-tip-item">
              <span>密码强度</span>
              <strong>{{ passwordStrengthText }}</strong>
            </div>
            <div class="password-tip-item">
              <span>校验状态</span>
              <strong>{{ confirmStateText }}</strong>
            </div>
          </div>

          <div class="password-actions">
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认修改</el-button>
          </div>
        </el-form>
      </el-card>
    </section>

    <section class="page-overview-grid profile-summary-grid">
      <div class="page-overview-card">
        <span class="page-overview-card__label">系统版本</span>
        <strong class="page-overview-card__value">V1.0.0</strong>
        <p class="page-overview-card__desc">当前为比赛展示版本，前后端分离架构已完整打通。</p>
      </div>

      <div class="page-overview-card">
        <span class="page-overview-card__label">AI 服务状态</span>
        <strong class="page-overview-card__value">已接入</strong>
        <p class="page-overview-card__desc">支持农业知识问答、环境分析与预警解释能力。</p>
      </div>

      <div class="page-overview-card">
        <span class="page-overview-card__label">演示模式说明</span>
        <strong class="page-overview-card__value">本地演示</strong>
        <p class="page-overview-card__desc">已连接真实数据库数据，适合比赛答辩与本地功能演示。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentUserApi, updatePasswordApi } from '@/api/auth'

const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const userInfo = reactive({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  lastLoginTime: ''
})

const formData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const displayInitial = computed(() => {
  const value = userInfo.nickname || userInfo.username || '管'
  return value.slice(0, 1)
})

const passwordStrengthText = computed(() => {
  const value = formData.newPassword || ''
  if (!value) return '待输入'

  let score = 0
  if (value.length >= 8) score += 1
  if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score += 1
  if (/\d/.test(value)) score += 1
  if (/[^A-Za-z0-9]/.test(value)) score += 1

  if (score <= 1) return '较弱（建议增加复杂度）'
  if (score === 2) return '中等（可继续增强）'
  if (score === 3) return '良好（适合当前场景）'
  return '较强（复杂度较高）'
})

const confirmStateText = computed(() => {
  if (!formData.newPassword && !formData.confirmPassword) {
    return '待输入'
  }
  if (formData.newPassword && formData.confirmPassword && formData.newPassword === formData.confirmPassword) {
    return '两次密码一致'
  }
  return '两次密码暂不一致'
})

const formRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码长度需为 6 到 20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入新密码'))
        } else if (value !== formData.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  fetchUserInfo()
})

async function fetchUserInfo() {
  loading.value = true
  try {
    const res = await getCurrentUserApi()
    Object.assign(userInfo, res)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }

  await formRef.value.validate()
  submitLoading.value = true

  try {
    await updatePasswordApi({
      oldPassword: formData.oldPassword,
      newPassword: formData.newPassword,
      confirmPassword: formData.confirmPassword
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    handleReset()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  formData.oldPassword = ''
  formData.newPassword = ''
  formData.confirmPassword = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped lang="scss">
.profile-main-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 16px;
}

.profile-info-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-identity {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 18px;
  background: linear-gradient(180deg, #fbfdfc 0%, #f6faf8 100%);
}

.avatar-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  border-radius: 26px;
  background: linear-gradient(135deg, var(--app-primary-dark) 0%, var(--app-primary) 100%);
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  box-shadow: 0 16px 32px rgba(23, 138, 98, 0.18);
}

.identity-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.identity-meta strong {
  font-size: 22px;
  font-weight: 700;
  color: var(--app-text);
}

.identity-meta > span {
  font-size: 14px;
  color: var(--app-text-secondary);
}

.identity-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-item {
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  background: #fff;
}

.info-item.wide {
  grid-column: span 2;
}

.info-item span {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.info-item strong {
  display: block;
  font-size: 15px;
  line-height: 1.7;
  color: var(--app-text);
  word-break: break-all;
}

.password-form {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100%;
  justify-content: space-between;
}

.profile-password-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.password-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.password-form :deep(.el-input__wrapper) {
  min-height: 50px;
  border-radius: 14px;
  padding: 0 16px;
}

.password-tip-list {
  display: grid;
  gap: 14px;
  margin-top: 8px;
}

.password-tip-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 62px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fbfa;
  border: 1px solid rgba(17, 82, 62, 0.06);
}

.password-tip-item span {
  font-size: 12px;
  color: var(--app-text-secondary);
}

.password-tip-item strong {
  font-size: 13px;
  color: var(--app-text);
}

.password-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.password-actions :deep(.el-button) {
  min-width: 86px;
  height: 44px;
  border-radius: 14px;
}

.profile-summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 1100px) {
  .profile-main-grid,
  .profile-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-identity,
  .password-tip-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .info-item.wide {
    grid-column: span 1;
  }
}
</style>
