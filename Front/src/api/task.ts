import request from '@/utils/request'
import type { PageResult } from '@/utils/request'
import type { TaskCardVO, TaskDetailVO, GrabVO, GrabConfirmVO, VerifyVO } from '@/types'

export interface TaskQueryDTO {
  page?: number
  pageSize?: number
  keyword?: string
  category?: number
  minAmount?: number
  maxAmount?: number
  deliveryType?: 0 | 1
  sortBy?: 'newest' | 'amount_asc' | 'amount_desc'
}

export interface TaskPublishDTO {
  title: string
  category: number
  description: string
  amount: number
  deliveryType: 0 | 1
  deadline: number
  taskImages?: string[]
}

export interface TaskSubmitDTO {
  proofUrls: string[]
  remark?: string
}

export interface TaskVerifyDTO {
  pass: boolean
  rejectReason?: string
}

export interface DisputeDTO {
  reason: string
}

export const taskApi = {
  list: (params: TaskQueryDTO) =>
    request.get<PageResult<TaskCardVO>>('/tasks', { params }),

  detail: (id: string) =>
    request.get<TaskDetailVO>(`/tasks/${id}`),

  publish: (data: TaskPublishDTO) =>
    request.post<any>('/tasks', data),

  grab: (id: string) =>
    request.post<GrabVO>(`/tasks/${id}/grab`),

  confirm: (id: string) =>
    request.post<GrabConfirmVO>(`/tasks/${id}/grab/confirm`),

  reject: (id: string) =>
    request.post<any>(`/tasks/${id}/grab/reject`),

  cancelGrab: (id: string) =>
    request.post<any>(`/tasks/${id}/grab/cancel`),

  submit: (id: string, data: TaskSubmitDTO) =>
    request.post<any>(`/tasks/${id}/submit`, data),

  verify: (id: string, data: TaskVerifyDTO) =>
    request.post<VerifyVO>(`/tasks/${id}/verify`, data),

  cancel: (id: string) =>
    request.post<any>(`/tasks/${id}/cancel`),

  dispute: (id: string, data: DisputeDTO) =>
    request.post<any>(`/tasks/${id}/dispute`, data),

  myPublished: (params: { page: number; pageSize: number; status?: number }) =>
    request.get<PageResult<TaskCardVO>>('/tasks/my/published', { params }),

  myGrabbed: (params: { page: number; pageSize: number; status?: number }) =>
    request.get<PageResult<TaskCardVO>>('/tasks/my/grabbed', { params }),
}
