import request from '@/utils/request'
import type { PageResult } from '@/utils/request'
import type { PaymentRecordVO, SettlementRecordVO } from '@/types'

export interface PayDTO {
  taskId: string
  payType: 0 | 1
}

export interface WithdrawDTO {
  amount: number
  withdrawType: 0 | 1
  account: string
}

export const paymentApi = {
  pay: (data: PayDTO) =>
    request.post<any>('/payment/pay', data),

  getRecords: (params: { page: number; pageSize: number }) =>
    request.get<PageResult<PaymentRecordVO>>('/payment/records', { params }),

  getSettlementRecords: (params: { page: number; pageSize: number }) =>
    request.get<PageResult<SettlementRecordVO>>('/settlement/records', { params }),

  withdraw: (data: WithdrawDTO) =>
    request.post<any>('/settlement/withdraw', data),
}
