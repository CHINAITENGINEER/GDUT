export const TASK_STATUS = {
  PENDING_AUDIT: 0,
  PENDING_GRAB: 1,
  GRABBED: 2,
  PENDING_PAYMENT: 3,
  IN_PROGRESS: 4,
  COMPLETED: 5,
  SETTLED: 6,
  REVIEWED: 7,
  CANCELLED: 8
} as const

export const TASK_STATUS_MAP: Record<number, string> = {
  0: '待审核',
  1: '待接单',
  2: '已抢单待协商',
  3: '待支付',
  4: '进行中',
  5: '已完成',
  6: '已结算',
  7: '已互评',
  8: '已取消'
}

export const CATEGORY_MAP: Record<number, string> = {
  1: '代取快递',
  2: '资料整理',
  3: '编程',
  4: '代课占位',
  5: '其他'
}

export const LEVEL_TABLE = [
  { level: 1, levelName: '新手接单者', requiredExp: 0, feeDiscount: 1.00 },
  { level: 2, levelName: '初级接单者', requiredExp: 200, feeDiscount: 0.95 },
  { level: 3, levelName: '熟练接单者', requiredExp: 1500, feeDiscount: 0.90 },
  { level: 4, levelName: '资深接单者', requiredExp: 4500, feeDiscount: 0.85 },
  { level: 5, levelName: '专业接单者', requiredExp: 10800, feeDiscount: 0.80 },
  { level: 6, levelName: '顶级接单者', requiredExp: 28800, feeDiscount: 0.75 }
]

export const LEVEL_COLORS: Record<number, string> = {
  1: '#909399',
  2: '#909399',
  3: '#409EFF',
  4: '#409EFF',
  5: '#9C27B0',
  6: '#E6A23C'
}
