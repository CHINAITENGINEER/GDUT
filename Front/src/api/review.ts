import request from '@/utils/request'
import type { PageResult } from '@/utils/request'
import type { ReviewItemVO } from '@/types'

export interface ReviewSubmitDTO {
  taskId: string
  score: 1 | 2 | 3 | 4 | 5
  content: string
}

export const reviewApi = {
  submit: (data: ReviewSubmitDTO) =>
    request.post<{ message: string; newCreditScore: number }>('/review/submit', data),

  getUserReviews: (userId: string, params: { page: number; pageSize: number }) =>
    request.get<PageResult<ReviewItemVO>>(`/review/user/${userId}`, { params }),

  getTaskReviews: (taskId: string) =>
    request.get<any>(`/review/task/${taskId}`),
}
