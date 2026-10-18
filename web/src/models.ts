// 与后端 API 对齐的类型定义
export interface UserBrief {
  id: number
  nickname: string
  avatar: string | null
  isFollowing: boolean
}

export interface User {
  id: number
  nickname: string
  avatar: string | null
  bio: string | null
  gender: string
  followingCount: number
  followerCount: number
  postCount: number
  createdAt: string
  isFollowing: boolean
}

export interface TopicBrief {
  id: number
  name: string
}

export interface Topic {
  id: number
  name: string
  cover: string | null
  postCount: number
}

export interface PostCard {
  id: number
  title: string | null
  cover: string | null
  coverThumb?: string | null
  images: string[]
  videoUrl?: string | null
  likeCount: number
  collectCount: number
  commentCount: number
  viewCount: number
  createdAt: string
  author: UserBrief
  topic: TopicBrief | null
  isLiked: boolean
  isCollected: boolean
}

export interface PostDetail extends PostCard {
  content: string | null
}

export interface CommentItem {
  id: number
  content: string
  likeCount: number
  createdAt: string
  user: UserBrief
  replyTo: UserBrief | null
  replies: CommentItem[]
}

export interface PageResult<T> {
  list: T[]
  page: number
  size: number
  total: number
  hasMore: boolean
}

export interface NotificationItem {
  id: number
  type: string
  refId: number | null
  content: string
  isRead: boolean
  createdAt: string
  actor: UserBrief | null
}

export interface Conversation {
  id: number
  lastMessage: string | null
  updatedAt: string
  peer: UserBrief
  unreadCount?: number
}

export interface Message {
  id: number
  conversationId: number
  senderId: number
  content: string
  isRead: boolean
  createdAt: string
}

export interface SearchResult {
  posts: PostCard[]
  users: User[]
}

export interface HotWord {
  word: string
  cnt: number
}

/** 收藏专辑 */
export interface Album {
  id: number
  name: string
  postCount: number
  createdAt: string
}

// ---- 举报 / 管理后台 ----
export interface ReportItem {
  id: number
  post_id: number
  reason: string
  detail: string | null
  status: 'PENDING' | 'APPROVED' | 'IGNORED'
  created_at: string
  handled_at: string | null
  post_title: string | null
  post_status: string
  reporter_nickname: string
}

export interface AdminStats {
  userCount: number
  postCount: number
  pendingReports: number
  commentCount: number
}

export interface AdminPost {
  id: number
  title: string | null
  status: string
  like_count: number
  collect_count: number
  comment_count: number
  view_count: number
  created_at: string
  author_nickname: string
}

export interface AdminPage<T> {
  list: T[]
  page: number
  size: number
  total: number
}
