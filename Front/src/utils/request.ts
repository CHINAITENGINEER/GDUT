import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const baseURL = (import.meta.env.VITE_API_BASE_URL || '/api') as string

const instance = axios.create({
  baseURL,
  timeout: 10000
})

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('campus_token')
    if (token) {
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
      localStorage.removeItem('campus_token')
      ElMessage.error('登录已失效，请重新登录')
      router.push('/login')
      return Promise.reject(new Error(res.message || '未登录'))
    }

    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
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
