import request from '@/utils/request'
import type { LoginVO } from '@/types'

export interface RegisterDTO {
  phone: string
  studentId?: string
  password: string
  smsCode: string
  nickname: string
}

export interface LoginDTO {
  account: string
  password?: string
  smsCode?: string
  loginType: 0 | 1
  remember: boolean
}

export interface SmsSendDTO {
  phone: string
  scene: 'register' | 'login' | 'reset'
}

export interface ResetPasswordDTO {
  phone: string
  smsCode: string
  newPassword: string
}

export const authApi = {
  register: (data: RegisterDTO) =>
    request.post<LoginVO>('/auth/register', data),

  login: (data: LoginDTO) =>
    request.post<LoginVO>('/auth/login', data),

  logout: () =>
    request.post<void>('/user/logout', {}),

  sendSms: (data: SmsSendDTO) =>
    request.post<void>('/auth/sms/send', data),

  resetPassword: (data: ResetPasswordDTO) =>
    request.post<void>('/auth/password/reset', data),
}
