import { create } from 'zustand'
import type { User } from '../models'

const TOKEN_KEY = 'xls_token'
const USER_KEY = 'xls_user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

interface AuthState {
  token: string | null
  user: User | null
  setAuth: (token: string, user: User) => void
  setUser: (user: User) => void
  logout: () => void
}

function loadUser(): User | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

export const useAuth = create<AuthState>((set) => ({
  token: getToken(),
  user: loadUser(),
  setAuth: (token, user) => {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    set({ token, user })
  },
  setUser: (user) => {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    set({ user })
  },
  logout: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    set({ token: null, user: null })
  },
}))

export function clearAuth(): void {
  useAuth.getState().logout()
}

/** 格式化相对时间 */
export function timeAgo(iso: string): string {
  const t = new Date(iso).getTime()
  if (Number.isNaN(t)) return ''
  const diff = Date.now() - t
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const hour = Math.floor(min / 60)
  if (hour < 24) return `${hour} 小时前`
  const day = Math.floor(hour / 24)
  if (day < 30) return `${day} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}
