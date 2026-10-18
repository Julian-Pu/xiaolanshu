import axios from 'axios'
import { clearAuth, getToken } from '../store/auth'

export const http = axios.create({ baseURL: '/api', timeout: 15000 })

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      clearAuth()
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  },
)

interface ApiBody<T> {
  code: number
  message: string
  data: T
}

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await http.get<ApiBody<T>>(url, { params })
  return res.data.data
}

export async function post<T>(url: string, data?: unknown): Promise<T> {
  const res = await http.post<ApiBody<T>>(url, data)
  return res.data.data
}

export async function put<T>(url: string, data?: unknown): Promise<T> {
  const res = await http.put<ApiBody<T>>(url, data)
  return res.data.data
}

export async function del<T>(url: string): Promise<T> {
  const res = await http.delete<ApiBody<T>>(url)
  return res.data.data
}
