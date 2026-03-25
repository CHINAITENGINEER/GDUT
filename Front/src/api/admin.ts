import request from '@/utils/request'
import type { PageResult } from '@/utils/request'
import type { AdminUserVO, TaskCardVO, FeeConfigItem, TradeRecordVO, TradeStatsVO } from '@/types'

export interface AuditDTO {
  pass: boolean
  rejectReason?: string
}

export interface FeeConfigUpdateDTO {
  configs: {
    minAmount: number
    maxAmount: number | null
    feeRate: number
    isActive: boolean
  }[]
}

export interface DisputeResolveDTO {
  decision: 'approve' | 'reject'
  remark: string
}

export interface FeeConfigVO {
  configs: FeeConfigItem[]
  updatedAt: number
  updatedByNickname: string
}

export const adminApi = {
  getUsers: (params: { page: number; pageSize: number; keyword?: string; status?: number }) =>
    request.get<PageResult<AdminUserVO>>('/admin/users', { params }),

  getUserDetail: (id: string) =>
    request.get<any>(`/admin/users/${id}`),

  updateUserStatus: (id: string, status: 0 | 1) =>
    request.put<void>(`/admin/users/${id}/status`, { status }),

  getPendingTasks: (params: { page: number; pageSize: number }) =>
    request.get<PageResult<TaskCardVO>>('/admin/tasks/pending', { params }),

  auditTask: (id: string, data: AuditDTO) =>
    request.put<void>(`/admin/tasks/${id}/audit`, data),

  deleteTask: (id: string) =>
    request.delete<void>(`/admin/tasks/${id}`),

  getTrades: (params: { page: number; pageSize: number; startTime?: number; endTime?: number }) =>
    request.get<PageResult<TradeRecordVO>>('/admin/trades', { params }),

  getTradeStats: (params?: { startTime?: number; endTime?: number }) =>
    request.get<TradeStatsVO>('/admin/trades/stats', { params }),

  getFeeConfig: () =>
    request.get<FeeConfigVO>('/admin/fee-config'),

  updateFeeConfig: (data: FeeConfigUpdateDTO) =>
    request.put<void>('/admin/fee-config', data),

  getDisputes: (params: { page: number; pageSize: number }) =>
    request.get<PageResult<any>>('/admin/disputes', { params }),

  resolveDispute: (taskId: string, data: DisputeResolveDTO) =>
    request.put<void>(`/admin/disputes/${taskId}/resolve`, data),

  getLogs: (params: { page: number; pageSize: number; type?: string; startTime?: number; endTime?: number }) =>
    request.get<PageResult<any>>('/admin/logs', { params }),
}
