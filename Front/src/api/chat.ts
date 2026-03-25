import request from '@/utils/request'

export interface ChatMessageVO {
  id: string
  grabRecordId: string
  taskId: string
  senderId: string
  senderNickname: string
  senderAvatar: string
  content: string
  images?: string[]
  createdAt: number
}

export const chatApi = {
  history: (grabRecordId: string) =>
    request.get<ChatMessageVO[]>(`/chat/history/${grabRecordId}`),
}
