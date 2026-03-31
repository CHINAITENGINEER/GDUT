import request from '@/utils/request'
import type { PageResult } from '@/utils/request'
import type { UserProfileVO, LevelInfoVO, BalanceVO, UserPublicVO } from '@/types'

export interface UserUpdateDTO {
  nickname?: string
  avatar?: string
  bio?: string
  skills?: string[]
}

export interface SwitchRoleDTO {
  role: 'publisher' | 'acceptor'
}

export interface ChangePasswordDTO {
  oldPassword: string
  newPassword: string
}

export const userApi = {
  getProfile: () =>
    request.get<UserProfileVO>('/user/profile'),

  updateProfile: (data: UserUpdateDTO) =>
    request.put<UserProfileVO>('/user/profile', data),

  switchRole: (data: SwitchRoleDTO) =>
    request.put<{ currentRole: 'publisher' | 'acceptor' }>('/user/switch-role', data),

  changePassword: (data: ChangePasswordDTO) =>
    request.put<void>('/user/password', data),

  getLevelInfo: () =>
    request.get<LevelInfoVO>('/user/level-info'),

  getBalance: () =>
    request.get<BalanceVO>('/user/balance'),

  getUserById: (id: string) =>
    request.get<UserPublicVO>(`/user/${id}`),
}
