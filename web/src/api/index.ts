import { del, get, post, put } from './http'
import axios from 'axios'
import type {
  AdminPage,
  AdminPost,
  AdminStats,
  Album,
  CommentItem,
  Conversation,
  HotWord,
  Message,
  NotificationItem,
  PageResult,
  PostCard,
  PostDetail,
  ReportItem,
  SearchResult,
  Topic,
  User,
} from '../models'

export const authApi = {
  register: (d: { phone: string; password: string; nickname: string; smsCode: string }) =>
    post<{ token: string; user: User }>('/auth/register', d),
  sendSmsCode: (d: { phone: string }) =>
    post<{ code: string }>('/auth/sms-code', d),
  login: (d: { phone: string; password: string }) =>
    post<{ token: string; user: User }>('/auth/login', d),
  resetPassword: (d: { phone: string; smsCode: string; newPassword: string }) =>
    post<{ message: string }>('/auth/reset-password', d),
  me: () => get<User>('/auth/me'),
}

export const reportApi = {
  create: (d: { postId: number; reason: string; detail?: string }) =>
    post<{ id: number; duplicated: boolean }>('/reports', d),
}

export const userApi = {
  profile: (id: number) => get<User>(`/users/${id}`),
  updateMe: (d: { nickname?: string; bio?: string; avatar?: string; gender?: string }) =>
    put<User>('/users/me', d),
  changePassword: (d: { oldPassword: string; newPassword: string }) =>
    post<void>('/users/me/password', d),
  follow: (id: number) => post<void>(`/users/${id}/follow`),
  unfollow: (id: number) => del<void>(`/users/${id}/follow`),
  following: (id: number, page = 1, size = 20) =>
    get<PageResult<User>>(`/users/${id}/following`, { page, size }),
  followers: (id: number, page = 1, size = 20) =>
    get<PageResult<User>>(`/users/${id}/followers`, { page, size }),
  posts: (id: number, page = 1, size = 12) =>
    get<PageResult<PostCard>>(`/users/${id}/posts`, { page, size }),
  myCollects: (page = 1, size = 30) =>
    get<PageResult<PostCard>>('/users/me/collects', { page, size }),
  myLikes: (page = 1, size = 30) =>
    get<PageResult<PostCard>>('/users/me/likes', { page, size }),
  myHistory: (page = 1, size = 30) =>
    get<PageResult<PostCard>>('/users/me/history', { page, size }),
}

export const albumApi = {
  list: () => get<Album[]>('/albums'),
  create: (name: string) => post<Album>('/albums', { name }),
  remove: (id: number) => del<void>(`/albums/${id}`),
  posts: (id: number, page = 1, size = 30) =>
    get<PageResult<PostCard>>(`/albums/${id}/posts`, { page, size }),
  addPost: (albumId: number, postId: number) =>
    post<void>(`/albums/${albumId}/posts`, { postId }),
  removePost: (albumId: number, postId: number) =>
    del<void>(`/albums/${albumId}/posts/${postId}`),
}

export const feedApi = {
  feed: (tab: string, page = 1, size = 10, topicId?: number) =>
    get<PageResult<PostCard>>('/feed', { tab, page, size, ...(topicId ? { topicId } : {}) }),
}

export const postApi = {
  detail: (id: number) => get<PostDetail>(`/posts/${id}`),
  create: (d: {
    title?: string
    content?: string
    images?: string[]
    videoUrl?: string
    videoCover?: string
    topicId?: number
  }) => post<PostDetail>('/posts', d),
  update: (
    id: number,
    d: {
      title?: string
      content?: string
      images?: string[]
      videoUrl?: string
      videoCover?: string
      topicId?: number
    },
  ) => put<PostDetail>(`/posts/${id}`, d),
  remove: (id: number) => del<void>(`/posts/${id}`),
  like: (id: number) =>
    post<{ liked: boolean; likeCount: number }>(`/posts/${id}/like`),
  unlike: (id: number) =>
    del<{ liked: boolean; likeCount: number }>(`/posts/${id}/like`),
  collect: (id: number) =>
    post<{ collected: boolean; collectCount: number }>(`/posts/${id}/collect`),
  uncollect: (id: number) =>
    del<{ collected: boolean; collectCount: number }>(`/posts/${id}/collect`),
  comments: (id: number, page = 1, size = 20) =>
    get<PageResult<CommentItem>>(`/posts/${id}/comments`, { page, size }),
  addComment: (id: number, d: { content: string; parentId?: number }) =>
    post<CommentItem>(`/posts/${id}/comments`, d),
  deleteComment: (id: number) => del<void>(`/comments/${id}`),
}

export const topicApi = {
  list: () => get<Topic[]>('/topics'),
}

export const searchApi = {
  search: (q: string, type = 'all', page = 1, size = 10) =>
    get<SearchResult>('/search', { q, type, page, size }),
  hot: () => get<HotWord[]>('/search/hot'),
}

export const notifApi = {
  list: (page = 1, size = 20) =>
    get<PageResult<NotificationItem>>('/notifications', { page, size }),
  readAll: () => post<number>('/notifications/read-all'),
}

export const messageApi = {
  conversations: () => get<Conversation[]>('/conversations'),
  start: (userId: number) => post<Conversation>('/conversations', { userId }),
  messages: (id: number, page = 1, size = 30) =>
    get<PageResult<Message>>(`/conversations/${id}/messages`, { page, size }),
  send: (id: number, content: string) =>
    post<Message>(`/conversations/${id}/messages`, { content }),
  markRead: (id: number) => post<{ changed: number }>(`/conversations/${id}/read`),
}

interface ApiBody<T> {
  code: number
  message: string
  data: T
}

/** 管理后台请求：全部携带 X-Admin-Key，密钥来自管理登录 */
async function adminReq<T>(
  method: 'get' | 'post' | 'delete',
  url: string,
  key: string,
  data?: unknown,
  params?: Record<string, unknown>,
): Promise<T> {
  const res = await axios.request<ApiBody<T>>({
    method,
    url: '/api' + url,
    headers: { 'X-Admin-Key': key },
    data,
    params,
    timeout: 15000,
  })
  const body = res.data
  if (body && typeof body.code === 'number' && body.code !== 0) {
    throw new Error(body.message || '请求失败')
  }
  return body.data
}

export const adminApi = {
  stats: (key: string) => adminReq<AdminStats>('get', '/admin/stats', key),
  reports: (key: string, status = '', page = 1, size = 20) =>
    adminReq<AdminPage<ReportItem>>('get', '/admin/reports', key, undefined, { status, page, size }),
  review: (key: string, id: number, action: 'APPROVE' | 'IGNORE') =>
    adminReq<{ id: number }>('post', `/admin/reports/${id}/review`, key, { action }),
  posts: (key: string, page = 1, size = 20) =>
    adminReq<AdminPage<AdminPost>>('get', '/admin/posts', key, undefined, { page, size }),
  deletePost: (key: string, id: number) =>
    adminReq<{ id: number }>('delete', `/admin/posts/${id}`, key),
}

export const uploadApi = {
  image: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return post<{ url: string; thumbUrl?: string | null }>('/upload/image', fd)
  },
  video: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return post<{ url: string; thumbUrl?: string | null }>('/upload/video', fd)
  },
}
