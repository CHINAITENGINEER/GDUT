import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 计算 baseURL：优先使用环境变量；否则
// - 若在 Vite 开发服务器（默认端口 5173），走 '/api' 供代理转发
// - 否则直连后端完整地址，避免非 5173 场景下代理失效导致路径错
const computedBaseURL = (() => {
  const envUrl = import.meta.env.VITE_API_BASE_URL as string | undefined
  const normalizeBaseURL = (raw: string) => {
    const u = raw.trim().replace(/\/+$/, '')
    // 后端所有接口都挂在 /api 下；若环境变量只配到了域名/端口（没有路径），自动补上 /api
    // 例如：http://localhost:8080  -> http://localhost:8080/api
    //      https://example.com     -> https://example.com/api
    const hasPath = (() => {
      try {
        const parsed = new URL(u)
        return parsed.pathname && parsed.pathname !== '/'
      } catch {
        // 兼容非标准输入：若包含 '/'（且不是协议分隔的 '//'），认为有路径
        return /https?:\/\/[^/]+\/.+/i.test(u)
      }
    })()
    return hasPath ? u : `${u}/api`
  }

  if (envUrl && envUrl.trim().length > 0) return normalizeBaseURL(envUrl)
  const isViteDev = typeof window !== 'undefined' && window.location && window.location.port === '5173'
  return isViteDev ? '/api' : 'http://localhost:8080/api'
})()
const baseURL = computedBaseURL as string

const instance = axios.create({
  baseURL,
  timeout: 10000
})

// 开发期快速定位：确认 baseURL 和每次请求的最终 URL（发布构建不会打印）
if (import.meta.env.DEV) {
  // eslint-disable-next-line no-console
  console.log('[request] baseURL =', baseURL)
}

instance.interceptors.request.use(
  (config) => {
    if (import.meta.env.DEV) {
      // eslint-disable-next-line no-console
      console.log('[request]', config.method?.toUpperCase(), (config.baseURL || '') + (config.url || ''))
    }
    const token = localStorage.getItem('campus_token')
    // 不给认证相关接口（/auth/*）附带 Authorization，避免过期旧 Token 干扰登录/发码
    const isAuthEndpoint = typeof config.url === 'string' && /^\/auth\//.test(config.url)
    if (token && !isAuthEndpoint) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return Promise.resolve(res.data)
    }

    if (res.code === 401) {
      const url = response?.config?.url as string | undefined
      const isAuthEndpoint = typeof url === 'string' && /^\/auth\//.test(url)
      // 登录/注册/发码接口返回 401 时，不应提示“token失效”，直接展示后端 message
      if (isAuthEndpoint) {
        ElMessage.error(res.message || '登录失败')
        return Promise.reject(new Error(res.message || '未登录'))
      }
      localStorage.removeItem('campus_token')
      ElMessage.error(res.message || '登录已失效，请重新登录')
      router.push('/login')
      return Promise.reject(new Error(res.message || '未登录'))
    }

    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    const url = error?.config?.url as string | undefined
    const isAuthEndpoint = typeof url === 'string' && /^\/auth\//.test(url)
    if (error?.response?.status === 401) {
      if (isAuthEndpoint) {
        ElMessage.error(error?.response?.data?.message || '登录失败')
        return Promise.reject(error)
      }
      localStorage.removeItem('campus_token')
      ElMessage.error(error?.response?.data?.message || '登录已失效，请重新登录')
      router.push('/login')
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export interface R<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// Helper type: unwraps R<T> -> T, used by typed API helpers
export type Unwrap<T> = T extends R<infer U> ? U : T

// Typed request helpers — the interceptor already unwraps R<T>, so we return Promise<T> directly
const request = {
  get<T = unknown>(url: string, config?: Parameters<typeof instance.get>[1]): Promise<T> {
    return instance.get(url, config) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: unknown, config?: Parameters<typeof instance.post>[2]): Promise<T> {
    return instance.post(url, data, config) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, config?: Parameters<typeof instance.put>[2]): Promise<T> {
    return instance.put(url, data, config) as unknown as Promise<T>
  },
  delete<T = unknown>(url: string, config?: Parameters<typeof instance.delete>[1]): Promise<T> {
    return instance.delete(url, config) as unknown as Promise<T>
  },
}

export default request
