<template>
  <div class="register-page">
    <div class="register-card">
      <div class="brand">
        <span class="brand-icon">🎓</span>
        <h1>注册账号</h1>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large" />
        </el-form-item>

        <el-form-item label="学号（可选）" prop="studentId">
          <el-input v-model="form.studentId" placeholder="请输入学号" size="large" />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" size="large" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>

        <el-form-item label="短信验证码" prop="smsCode">
          <div class="sms-row">
            <el-input v-model="form.smsCode" placeholder="请输入验证码" size="large" />
            <el-button :disabled="smsCooldown > 0" @click="sendSms" size="large">
              {{ smsCooldown > 0 ? `${smsCooldown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-button type="primary" size="large" class="submit-btn" :loading="loading" native-type="submit">
          注 册
        </el-button>
      </el-form>

      <div class="footer-links">
        <router-link to="/login">已有账号？立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import md5 from 'md5'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const smsCooldown = ref(0)

const form = reactive({
  phone: '',
  studentId: '',
  nickname: '',
  password: '',
  smsCode: ''
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  smsCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function sendSms() {
  if (!form.phone) { ElMessage.warning('请先输入手机号'); return }
  try {
    await authApi.sendSms({ phone: form.phone, scene: 'register' })
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

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authApi.register({
      phone: form.phone,
      studentId: form.studentId || undefined,
      nickname: form.nickname,
      password: md5(form.password),
      smsCode: form.smsCode
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
  }
  loading.value = false
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1f2e 0%, #252d3d 100%);
}

.register-card {
  width: 440px;
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
