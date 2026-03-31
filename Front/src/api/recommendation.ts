import request from '@/utils/request'
import type { RecommendedTaskVO, RecommendationProfileVO } from '@/types'

export interface RecommendationProfileDTO {
  abilityTags?: string[]
  preferredCategoryIds?: number[]
  preferredDeliveryType?: 0 | 1
  minAcceptAmount?: number
  maxAcceptAmount?: number
  dailyRecommendLimit?: number
}

export const recommendationApi = {
  getProfile: () =>
    request.get<RecommendationProfileVO>('/recommendation/profile'),

  saveProfile: (data: RecommendationProfileDTO) =>
    request.put<RecommendationProfileVO>('/recommendation/profile', data),

  recommendTasks: (limit = 6) =>
    request.get<RecommendedTaskVO[]>('/recommendation/tasks', { params: { limit } }),

  refreshWeights: (taskId: string) =>
    request.post<void>(`/recommendation/tasks/${taskId}/weights/refresh`),

  // 用户点击任务详情时触发轻量权重反馈（学习率 0.01）
  onTaskClick: (taskId: number | string) =>
    request.post<void>(`/recommendation/tasks/${taskId}/click`),

  // 融合协同过滤的个性化推荐
  recommendTasksWithCF: (limit = 10) =>
    request.get<RecommendedTaskVO[]>('/recommendation/tasks/cf', { params: { limit } }),
}
