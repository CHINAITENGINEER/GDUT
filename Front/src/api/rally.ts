import request from '@/utils/request'
import type { RallyActivityVO, RallyMemberVO, RallyMessageVO } from '@/types'

export interface RallyCreateDTO {
  type: 1 | 2
  title: string
  recruitCount: number
  startTime: number
  remark?: string | null
}

export const rallyApi = {
  list: () => request.get<RallyActivityVO[]>('/rally/list'),

  create: (data: RallyCreateDTO) => request.post<RallyActivityVO>('/rally', data),

  join: (id: string) => request.post<RallyActivityVO>(`/rally/${id}/join`),

  quit: (id: string) => request.post<RallyActivityVO>(`/rally/${id}/quit`),

  end: (id: string) => request.post<void>(`/rally/${id}/end`),

  members: (id: string) => request.get<RallyMemberVO[]>(`/rally/${id}/members`),

  history: (id: string) => request.get<RallyMessageVO[]>(`/rally/${id}/history`),
}

export type RallyWsEvent =
  | { type: 'rally_created'; activity: RallyActivityVO }
  | { type: 'rally_updated'; activity: RallyActivityVO }
  | { type: 'rally_ended'; rallyId: string }
  | { type: 'member_joined'; rallyId: string; userId: string; nickname: string; avatar?: string; currentCount: number }
  | { type: 'member_quit'; rallyId: string; userId: string; nickname: string; currentCount: number }
  | ({ type: 'message' } & RallyMessageVO)
  | { type: 'joined'; rallyId: string; userId: string; nickname: string }
  | { type: 'error'; msg: string }
