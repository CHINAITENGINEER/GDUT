import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserProfileVO } from '@/types'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
// 后端已兼容 BCrypt 和 SHA256+salt，前端无需再二次加密

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('campus_token'))
  const userInfo = ref<UserProfileVO | null>(null)
  const currentRole = ref<'publisher' | 'acceptor'>('publisher')

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 1)
  const isPublisher = computed(() => currentRole.value === 'publisher')
  const isAcceptor = computed(() => currentRole.value === 'acceptor')

  async function login(data: { account: string; password?: string; smsCode?: string; loginType: 0 | 1; remember: boolean }) {
    const loginData = { ...data }
    // 直接传明文密码，交由后端进行 BCrypt / SHA256+salt 校验
    // interceptor already unwraps R<LoginVO> -> LoginVO
    const res = await authApi.login(loginData)
    token.value = res.token
    userInfo.value = {
      id: res.userId,
      nickname: res.nickname,
      avatar: res.avatar,
      role: res.role,
      level: res.level,
      creditScore: res.creditScore
    } as UserProfileVO
    localStorage.setItem('campus_token', res.token)
    currentRole.value = res.role === 1 ? 'acceptor' : 'publisher'
  }

  async function getProfile() {
    try {
      // interceptor already unwraps R<UserProfileVO> -> UserProfileVO
      const res = await userApi.getProfile()
      userInfo.value = res
      currentRole.value = res.currentRole || 'publisher'
    } catch (e) {
      logout()
    }
  }

  async function switchRole(role: 'publisher' | 'acceptor') {
    await userApi.switchRole({ role })
    currentRole.value = role
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {}
    token.value = null
    userInfo.value = null
    currentRole.value = 'publisher'
    localStorage.removeItem('campus_token')
  }

  return {
    token,
    userInfo,
    currentRole,
    isLogin,
    isAdmin,
    isPublisher,
    isAcceptor,
    login,
    getProfile,
    switchRole,
    logout
  }
})
