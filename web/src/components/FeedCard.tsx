import { useState, type MouseEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { postApi } from '../api'
import { timeAgo, useAuth } from '../store/auth'
import type { PostCard } from '../models'

export default function FeedCard({ post }: { post: PostCard }) {
  const navigate = useNavigate()
  const token = useAuth((s) => s.token)
  const [liked, setLiked] = useState(post.isLiked)
  const [likeCount, setLikeCount] = useState(post.likeCount)

  async function toggleLike(e: MouseEvent) {
    e.stopPropagation()
    if (!token) {
      navigate('/login')
      return
    }
    try {
      if (liked) {
        const r = await postApi.unlike(post.id)
        setLikeCount(r.likeCount)
      } else {
        const r = await postApi.like(post.id)
        setLikeCount(r.likeCount)
      }
      setLiked(!liked)
    } catch {
      // 忽略
    }
  }

  return (
    <div className="feed-card" onClick={() => navigate(`/post/${post.id}`)}>
      <div className="feed-cover">
        {post.cover && <img src={post.coverThumb ?? post.cover} alt={post.title ?? ''} loading="lazy" />}
        {post.videoUrl && (
          <span
            style={{
              position: 'absolute',
              left: '50%',
              top: '50%',
              transform: 'translate(-50%, -50%)',
              width: 34,
              height: 34,
              borderRadius: '50%',
              background: 'rgba(0,0,0,.55)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 14,
            }}
          >
            ▶
          </span>
        )}
      </div>
      <div className="feed-body">
        {post.topic && (
          <div style={{ marginBottom: 6 }}>
            <span className="topic-tag">#{post.topic.name}</span>
          </div>
        )}
        <div className="feed-title">{post.title || '分享生活点滴'}</div>
        <div className="feed-meta">
          <img className="avatar-sm" src={post.author.avatar ?? ''} alt="" />
          <span>{post.author.nickname}</span>
          <span className="text-3">{timeAgo(post.createdAt)}</span>
          <span className="feed-stats">
            <button
              className={`icon-btn${liked ? ' active' : ''}`}
              onClick={toggleLike}
              style={{ padding: '2px 8px', fontSize: 12 }}
            >
              ♥ {likeCount}
            </button>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              💬 {post.commentCount}
            </span>
          </span>
        </div>
      </div>
    </div>
  )
}
