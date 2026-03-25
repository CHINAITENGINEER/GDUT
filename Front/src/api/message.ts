import request from '@/utils/request'
import type { PageResult } from '@/utils/request'
import type { MessageVO } from '@/types'

export interface MessageQueryDTO {
  page: number
  pageSize: number
  type?: 0 | 1
}

export interface SendMessageDTO {
  receiverId: string
  taskId?: string
  content: string
}

export const messageApi = {
  list: (params: MessageQueryDTO) =>
    request.get<PageResult<MessageVO>>('/message/list', { params }),

  read: (id: string) =>
    request.put<void>(`/message/read/${id}`),

  readAll: () =>
    request.put<void>('/message/read-all'),

  delete: (id: string) =>
    request.delete<void>(`/message/${id}`),

  send: (data: SendMessageDTO) =>
    request.post<{ messageId: string }>('/message/send', data),

  getUnreadCount: () =>
    request.get<{ count: number }>('/message/unread-count'),
}
