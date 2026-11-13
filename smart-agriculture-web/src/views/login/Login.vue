<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="login-side">
        <div class="hero-badge">智慧农业平台</div>

        <div class="hero-copy">
          <h1>智慧农业管理系统</h1>
          <p>
            面向比赛展示场景，聚焦农田、作物、设备、环境监测、预警处置与 AI
            农业分析，构建一个整齐、清晰、可演示的农业数字化管理平台。
          </p>
        </div>

        <div class="feature-grid">
          <div class="feature-card">
            <div class="feature-icon">田</div>
            <div class="feature-content">
              <strong>农田管理</strong>
              <span>统一查看农田面积、位置、土壤类型与种植状态。</span>
            </div>
          </div>
          <div class="feature-card">
            <div class="feature-icon warning">警</div>
            <div class="feature-content">
              <strong>智能预警</strong>
              <span>联动监测数据与设备状态，快速识别异常并辅助处置。</span>
            </div>
          </div>
          <div class="feature-card">
            <div class="feature-icon tech">AI</div>
            <div class="feature-content">
              <strong>AI 农业分析</strong>
              <span>支持农业问答、预警解释和环境数据分析建议。</span>
            </div>
          </div>
        </div>
      </section>

      <section class="login-panel page-card">
        <div class="panel-head">
          <div class="panel-logo">智农</div>
          <div class="panel-title">
            <h2>管理员登录</h2>
            <p>进入比赛演示后台，查看农业监测数据与智能分析结果。</p>
          </div>
        </div>

        <div class="panel-tip">
          <span class="panel-tip__dot"></span>
          <span>当前环境已接入真实业务数据演示</span>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="请输入管理员账号"
              :prefix-icon="User"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              size="large"
              placeholder="请输入登录密码"
              :prefix-icon="Lock"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            进入系统
          </el-button>
        </el-form>

        <div class="demo-tip">
          <span class="demo-tip__label">演示账号</span>
          <span class="demo-tip__value">admin / 123456</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
})

const rules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度应在 3 到 20 位之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应在 6 到 20 位之间', trigger: 'blur' }
  ]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功，正在进入首页')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error?.message || '登录失败，请检查后端服务与数据库连接')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background:
    radial-gradient(circle at 18% 18%, rgba(23, 138, 98, 0.10), transparent 24%),
    radial-gradient(circle at 82% 12%, rgba(76, 139, 245, 0.05), transparent 18%),
    linear-gradient(135deg, #eef6f2 0%, #f8fbfd 52%, #eef4f1 100%);
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(380px, 440px);
  align-items: center;
  gap: 48px;
  width: min(1180px, 100%);
}

.login-side {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 36px 0;

  &::before {
    content: '';
    position: absolute;
    inset: 0 36px 0 0;
    border-radius: 28px;
    background:
      linear-gradient(rgba(23, 138, 98, 0.04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(23, 138, 98, 0.04) 1px, transparent 1px);
    background-size: 26px 26px;
    pointer-events: none;
    opacity: 0.8;
  }
}

.hero-badge,
.hero-copy,
.feature-grid {
  position: relative;
  z-index: 1;
}

.hero-badge {
  width: fit-content;
  padding: 8px 14px;
  border: 1px solid rgba(23, 138, 98, 0.12);
  border-radius: 999px;
  background: rgba(23, 138, 98, 0.08);
  color: #126847;
  font-size: 12px;
  font-weight: 700;
}

.hero-copy {
  h1 {
    margin: 0 0 14px;
    font-size: 48px;
    line-height: 1.12;
    letter-spacing: 0.2px;
    color: #15342b;
  }

  p {
    max-width: 620px;
    margin: 0;
    font-size: 16px;
    line-height: 1.75;
    color: #5b726c;
  }
}

.feature-grid {
  display: grid;
  gap: 14px;
}

.feature-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 18px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 8px 18px rgba(16, 49, 40, 0.05);
}

.feature-icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 14px;
  background: rgba(23, 138, 98, 0.10);
  color: var(--app-primary-dark);
  font-size: 14px;
  font-weight: 800;

  &.warning {
    background: rgba(242, 181, 68, 0.16);
    color: #9b6a08;
  }

  &.tech {
    background: rgba(76, 139, 245, 0.12);
    color: var(--app-tech);
  }
}

.feature-content {
  strong {
    display: block;
    margin-bottom: 6px;
    font-size: 16px;
    color: #18352d;
  }

  span {
    display: block;
    font-size: 13px;
    line-height: 1.75;
    color: #617670;
  }
}

.login-panel {
  padding: 32px 30px 28px;
  background: rgba(255, 255, 255, 0.96);
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-logo {
  display: grid;
  width: 62px;
  height: 62px;
  flex-shrink: 0;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, #1c8f65, #3fa67d);
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  box-shadow: 0 14px 26px rgba(23, 138, 98, 0.16);
}

.panel-title {
  h2 {
    margin: 0 0 6px;
    font-size: 28px;
    color: #17352e;
  }

  p {
    margin: 0;
    font-size: 13px;
    line-height: 1.7;
    color: var(--app-text-secondary);
  }
}

.panel-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px 14px;
  border: 1px solid rgba(17, 82, 62, 0.08);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(234, 246, 240, 0.72), rgba(255, 255, 255, 0.92));
  font-size: 13px;
  color: var(--app-text-secondary);
}

.panel-tip__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #19bf74;
  box-shadow: 0 0 10px rgba(25, 191, 116, 0.5);
}

.login-form {
  :deep(.el-form-item__label) {
    font-weight: 700;
    color: #28473f;
  }

  :deep(.el-input__wrapper) {
    min-height: 50px;
    border-radius: 16px;
    box-shadow: 0 0 0 1px rgba(18, 104, 71, 0.08) inset;
  }
}

.login-btn {
  width: 100%;
  height: 52px;
  margin-top: 10px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #178a62, #26966d);
  box-shadow: 0 14px 26px rgba(23, 138, 98, 0.18);
  font-size: 16px;
  font-weight: 700;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    filter 0.18s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(23, 138, 98, 0.20);
  filter: saturate(1.03);
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 10px 18px rgba(23, 138, 98, 0.16);
}

.demo-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
  padding: 12px 14px;
  border: 1px dashed rgba(23, 138, 98, 0.18);
  border-radius: 14px;
  background: rgba(23, 138, 98, 0.05);
}

.demo-tip__label {
  font-size: 12px;
  font-weight: 700;
  color: #5f7770;
}

.demo-tip__value {
  font-size: 15px;
  font-weight: 800;
  color: #146546;
}

@media (max-width: 1100px) {
  .login-shell {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .login-side {
    padding: 16px 0 0;
  }
}

@media (max-width: 768px) {
  .login-page {
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 36px;
  }

  .hero-copy p {
    font-size: 15px;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-panel {
    padding: 26px 20px 22px;
  }

  .demo-tip {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
