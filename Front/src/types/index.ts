export interface UserProfileVO {
  id: string
  studentId: string
  phone: string
  nickname: string
  avatar: string
  bio: string
  skills: string[]
  role: 0 | 1
  currentRole: 'publisher' | 'acceptor'
  creditScore: number
  level: number
  exp: number
  balance: number
  totalEarned: number
  status: 0 | 1
  createdAt: number
}

export interface LoginVO {
  token: string
  userId: string
  nickname: string
  avatar: string
  role: 0 | 1
  level: number
  creditScore: number
}

export interface LevelInfoVO {
  level: number
  levelName: string
  exp: number
  nextLevelExp: number | null
  expToNext: number
  progress: number
  feeDiscount: number
  totalEarned: number
  levelTable: LevelItem[]
}

export interface LevelItem {
  level: number
  levelName: string
  requiredExp: number
  feeDiscount: number
}

export interface BalanceVO {
  balance: number
  totalEarned: number
  frozenAmount: number
}

export interface TaskCardVO {
  id: string
  title: string
  category: number
  categoryName: string
  amount: number
  deliveryType: 0 | 1
  deadline: number
  status: number
  statusName: string
  taskImages: string[]
  publisher: {
    id: string
    nickname: string
    avatar: string
    creditScore: number
  }
  createdAt: number
}

export interface TaskDetailVO {
  id: string
  title: string
  category: number
  categoryName: string
  description: string
  amount: number
  deliveryType: 0 | 1
  deadline: number
  status: number
  statusName: string
  needAudit: boolean
  taskImages: string[]
  deliveryProof: string[]
  rejectReason: string
  lockExpireAt: number
  grabRecordId?: string
  publisher: {
    id: string
    nickname: string
    avatar: string
    creditScore: number
    level: number
  }
  acceptor?: {
    id: string
    nickname: string
    avatar: string
    creditScore: number
    level: number
    levelName: string
    feeDiscount: number
  }
  estimatedFee?: number
  estimatedIncome?: number
  createdAt: number
  updatedAt: number
}

export interface GrabVO {
  lockExpireAt: number
  message: string
}

export interface GrabConfirmVO {
  payUrl: string
  message: string
}

export interface VerifyVO {
  message: string
  settlement?: {
    taskAmount: number
    feeDiscount: number
    feeAmount: number
    realAmount: number
    expGained: number
  }
}

export interface PaymentRecordVO {
  id: string
  taskId: string
  taskTitle: string
  amount: number
  payType: 0 | 1
  payStatus: 0 | 1 | 2
  tradeNo: string
  createdAt: number
}

export interface SettlementRecordVO {
  id: string
  taskId: string
  taskTitle: string
  taskAmount: number
  baseFeeRate: number
  levelAtSettle: number
  feeDiscount: number
  feeRate: number
  feeAmount: number
  realAmount: number
  expGained: number
  status: 0 | 1
  settledAt: number
}

export interface ReviewItemVO {
  id: string
  taskId: string
  taskTitle: string
  reviewer: {
    id: string
    nickname: string
    avatar: string
  }
  score: number
  content: string
  type: 0 | 1
  createdAt: number
}

export interface MessageVO {
  id: string
  senderId: string
  senderNickname: string
  senderAvatar: string
  taskId?: string
  taskTitle?: string
  type: 0 | 1
  content: string
  isRead: boolean
  createdAt: number
}

export interface AdminUserVO {
  id: string
  studentId: string
  phone: string
  nickname: string
  role: 0 | 1
  level: number
  creditScore: number
  balance: number
  totalEarned: number
  status: 0 | 1
  createdAt: number
}

export interface FeeConfigItem {
  id: number
  minAmount: number
  maxAmount: number | null
  feeRate: number
  isActive: boolean
  updatedBy: number
  updatedAt: number
}

export interface TradeRecordVO {
  settlementId: string
  taskId: string
  taskAmount: number
  feeAmount: number
  realAmount: number
  levelAtSettle: number
  feeDiscount: number
  settledAt: number
}

export interface TradeStatsVO {
  totalFeeIncome: number
  totalTaskCount: number
  totalTaskAmount: number
  avgFeeRate: number
  statsByCategory: {
    category: number
    categoryName: string
    count: number
    feeIncome: number
  }[]
}

export interface RallyMemberVO {
  userId: string
  nickname: string
  avatar: string
  role: 0 | 1
  joinedAt: number
}

export interface RallyActivityVO {
  id: string
  type: 1 | 2
  typeName: string
  title: string
  recruitCount: number
  currentCount: number
  startTime: number
  remark?: string
  status: 0 | 1
  organizerId: string
  organizerNickname: string
  organizerAvatar: string
  createdAt: number
  members?: RallyMemberVO[]
}

export interface RallyMessageVO {
  id: string
  rallyId: string
  senderId: string
  senderNickname: string
  senderAvatar: string
  content: string
  createdAt: number
}
