import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatTime(ts: number | string): string {
  if (!ts) return '-'
  return dayjs(Number(ts)).format('YYYY-MM-DD HH:mm:ss')
}

export function formatRelative(ts: number | string): string {
  if (!ts) return '-'
  return dayjs(Number(ts)).fromNow()
}

export function formatDate(ts: number | string): string {
  if (!ts) return '-'
  return dayjs(Number(ts)).format('YYYY-MM-DD')
}

export function toTimestamp(date: Date | string): number {
  return dayjs(date).valueOf()
}

export function formatMoney(amount: number): string {
  if (amount == null) return '¥0.00'
  return `¥${amount.toFixed(2)}`
}

export function formatTaskStatus(status: number): string {
  const map: Record<number, string> = {
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
  return map[status] || '未知'
}

export function formatCategory(category: number): string {
  const map: Record<number, string> = {
    1: '代取快递',
    2: '资料整理',
    3: '编程',
    4: '代课占位',
    5: '其他'
  }
  return map[category] || '其他'
}

export function formatLevel(level: number): string {
  const map: Record<number, string> = {
    1: '新手接单者',
    2: '初级接单者',
    3: '熟练接单者',
    4: '资深接单者',
    5: '专业接单者',
    6: '顶级接单者'
  }
  return map[level] || '新手接单者'
}

export function formatLevelName(level: number): string {
  return `Lv.${level} ${formatLevel(level)}`
}
