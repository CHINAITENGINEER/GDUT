<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <span class="brand-icon">🎓</span>
        <h1>校园任务接单平台</h1>
      </div>

      <el-tabs v-model="loginType" class="login-tabs">
        <el-tab-pane label="密码登录" :name="0" />
        <el-tab-pane label="验证码登录" :name="1" />
      </el-tabs>

      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="手机号 / 学号" prop="account" :error="errors.account">
          <el-input v-model="form.account" placeholder="请输入手机号或学号" size="large" />
        </el-form-item>

        <el-form-item v-if="loginType === 0" label="密码" prop="password" :error="errors.password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>

        <el-form-item v-else label="验证码" prop="smsCode" :error="errors.smsCode">
          <div class="sms-row">
            <el-input v-model="form.smsCode" placeholder="请输入验证码" size="large" />
            <el-button :disabled="smsCooldown > 0" @click="sendSms" size="large">
              {{ smsCooldown > 0 ? `${smsCooldown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="form.remember">记住登录</el-checkbox>
        </el-form-item>

        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>

      <div class="footer-links">
        <router-link to="/register">还没有账号？立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const loginType = ref<0 | 1>(0)
const smsCooldown = ref(0)

const form = reactive({
  account: '',
  password: '',
  smsCode: '',
  remember: false
})

const errors = reactive({
  account: '',
  password: '',
  smsCode: ''
})

function clearErrors() {
  errors.account = ''
  errors.password = ''
  errors.smsCode = ''
}

watch(loginType, () => {
  clearErrors()
  formRef.value?.clearValidate?.()
})

function validateCurrentForm() {
  clearErrors()

  if (!form.account.trim()) {
    errors.account = '请输入账号'
    return false
  }

  if (loginType.value === 0) {
    if (!form.password.trim()) {
      errors.password = '请输入密码'
      return false
    }
  } else {
    if (!form.smsCode.trim()) {
      errors.smsCode = '请输入验证码'
      return false
    }
  }

  return true
}

async function sendSms() {
  if (!form.account) {
    errors.account = '请输入账号'
    ElMessage.warning('请先输入手机号')
    return
  }
  try {
    await authApi.sendSms({ phone: form.account, scene: 'login' })
    ElMessage.success('验证码已发送')
    smsCooldown.value = 60
    const t = setInterval(() => {
      smsCooldown.value--
      if (smsCooldown.value <= 0) clearInterval(t)
    }, 1000)
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  }
}

async function handleLogin() {
  if (!validateCurrentForm()) return

  loading.value = true
  try {
    await userStore.login({
      account: form.account,
      password: loginType.value === 0 ? form.password : undefined,
      smsCode: loginType.value === 1 ? form.smsCode : undefined,
      loginType: loginType.value,
      remember: form.remember
    })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect as string || '/'
    router.push(redirect)
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1f2e 0%, #252d3d 100%);
}

.login-card {
  width: 420px;
  background: #1e2536;
  border: 1px solid #3a4556;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.4);
}

.brand {
  text-align: center;
  margin-bottom: 28px;
  .brand-icon { font-size: 40px; }
  h1 {
    margin-top: 10px;
    font-size: 20px;
    font-weight: 700;
    color: #e8eaed;
  }
}

.login-tabs {
  margin-bottom: 16px;
  :deep(.el-tabs__nav-wrap::after) { display: none; }
  :deep(.el-tabs__item) { color: #a8adb5; }
  :deep(.el-tabs__item.is-active) { color: #5b9ef5; }
  :deep(.el-tabs__active-bar) { background: #5b9ef5; }
}

.sms-row {
  display: flex;
  gap: 10px;
  width: 100%;
  .el-input { flex: 1; }
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  font-size: 16px;
  letter-spacing: 4px;
}

.footer-links {
  text-align: center;
  margin-top: 20px;
  a {
    color: #5b9ef5;
    font-size: 14px;
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
}

:deep(.el-form-item__label) { color: #a8adb5; }
:deep(.el-input__wrapper) {
  background: #252d3d;
  border-color: #3a4556;
  box-shadow: none;
  &:hover, &.is-focus { border-color: #5b9ef5 !important; }
}
:deep(.el-input__inner) { color: #e8eaed; }
</style>
