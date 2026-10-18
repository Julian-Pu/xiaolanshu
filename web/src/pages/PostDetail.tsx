import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { postApi } from '../api'
import type { CommentItem, PostDetail } from '../models'
import { timeAgo, useAuth } from '../store/auth'
import CommentItemView from '../components/CommentItem'
import FollowButton from '../components/FollowButton'

export default function PostDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const token = useAuth((s) => s.token)
  const user = useAuth((s) => s.user)

  const [post, setPost] = useState<PostDetail | null>(null)
  const [activeImg, setActiveImg] = useState(0)
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  const [collected, setCollected] = useState(false)
  const [collectCount, setCollectCount] = useState(0)
  const [comments, setComments] = useState<CommentItem[]>([])
  const [commentText, setCommentText] = useState('')
  const [replyTo, setReplyTo] = useState<{ commentId: number; name: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const loaded = useRef(false)

  const load = useCallback(async () => {
    if (!id) return
    try {
      const p = await postApi.detail(Number(id))
      setPost(p)
      setLiked(p.isLiked)
      setLikeCount(p.likeCount)
      setCollected(p.isCollected)
      setCollectCount(p.collectCount)
      const c = await postApi.comments(Number(id))
      setComments(c.list)
    } catch {
      // 笔记不存在
    }
  }, [id])

  useEffect(() => {
    if (!loaded.current) {
      loaded.current = true
      load()
    }
  }, [load])

  async function toggleLike() {
    if (!token) {
      navigate('/login')
      return
    }
    try {
      if (liked) {
        const r = await postApi.unlike(Number(id))
        setLikeCount(r.likeCount)
      } else {
        const r = await postApi.like(Number(id))
        setLikeCount(r.likeCount)
      }
      setLiked(!liked)
    } catch {
      // 忽略
    }
  }

  async function toggleCollect() {
    if (!token) {
      navigate('/login')
      return
    }
    try {
      if (collected) {
        const r = await postApi.uncollect(Number(id))
        setCollectCount(r.collectCount)
      } else {
        const r = await postApi.collect(Number(id))
        setCollectCount(r.collectCount)
      }
      setCollected(!collected)
    } catch {
      // 忽略
    }
  }

  async function submitComment() {
    if (!token) {
      navigate('/login')
      return
    }
    if (!commentText.trim()) return
    setBusy(true)
    try {
      await postApi.addComment(Number(id), {
        content: commentText.trim(),
        parentId: replyTo?.commentId,
      })
      setCommentText('')
      setReplyTo(null)
      const c = await postApi.comments(Number(id))
      setComments(c.list)
      setPost((p) => (p ? { ...p, commentCount: p.commentCount + 1 } : p))
    } catch {
      // 忽略
    } finally {
      setBusy(false)
    }
  }

  async function removePost() {
    if (!post || !window.confirm('确定删除这篇笔记吗？')) return
    try {
      await postApi.remove(post.id)
      navigate('/', { replace: true })
    } catch {
      // 忽略
    }
  }

  if (!post) {
    return <div className="loading">加载中…</div>
  }

  return (
    <div className="detail-grid">
      <div className="detail-main">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
          <img
            className="avatar"
            src={post.author.avatar ?? ''}
            alt=""
            style={{ cursor: 'pointer' }}
            onClick={() => navigate(`/profile/${post.author.id}`)}
          />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div
              style={{ fontWeight: 600, cursor: 'pointer' }}
              onClick={() => navigate(`/profile/${post.author.id}`)}
            >
              {post.author.nickname}
            </div>
            <div className="text-3" style={{ fontSize: 12 }}>
              {timeAgo(post.createdAt)} 发布 · {post.viewCount} 浏览
            </div>
          </div>
          <FollowButton userId={post.author.id} isFollowing={post.author.isFollowing} />
          {user?.id === post.author.id ? (
            <>
              <button className="btn" onClick={() => navigate(`/post/${post.id}/edit`)}>
                编辑
              </button>
              <button className="btn btn-danger" onClick={removePost}>
                删除
              </button>
            </>
          ) : (
            <button
              className="btn"
              onClick={async () => {
                try {
                  await navigator.clipboard.writeText(`【小蓝书】${post.title ?? '分享一条笔记'} ${location.origin}/post/${post.id}`)
                  alert('链接已复制，去分享吧～')
                } catch {
                  alert('复制失败，请手动复制地址栏链接')
                }
              }}
            >
              分享
            </button>
          )}
        </div>

        {post.images.length > 1 && (
          <div className="thumb-row">
            {post.images.map((img, i) => (
              <img
                key={img + i}
                className={`thumb${activeImg === i ? ' active' : ''}`}
                src={img}
                alt=""
                onClick={() => setActiveImg(i)}
              />
            ))}
          </div>
        )}
        {post.images.length > 0 && (
          <div className="detail-cover">
            <img src={post.images[Math.min(activeImg, post.images.length - 1)]} alt="" />
          </div>
        )}
        {post.videoUrl && (
          <div className="detail-cover" style={{ aspectRatio: '3 / 4', background: '#000' }}>
            <video
              src={post.videoUrl}
              controls
              preload="metadata"
              style={{ width: '100%', height: '100%', objectFit: 'contain' }}
            />
          </div>
        )}

        <h1 className="detail-title">{post.title || '分享生活点滴'}</h1>
        {post.topic && <span className="topic-tag">#{post.topic.name}</span>}
        {post.content && <div className="detail-content">{post.content}</div>}

        <div className="action-bar" style={{ marginTop: 16 }}>
          <button className={`icon-btn${liked ? ' active' : ''}`} onClick={toggleLike}>
            ♥ {likeCount}
          </button>
          <button className={`icon-btn${collected ? ' active' : ''}`} onClick={toggleCollect}>
            ⭐ 收藏 {collectCount}
          </button>
          <span className="icon-btn" style={{ cursor: 'default' }}>💬 {post.commentCount}</span>
        </div>

        <div className="comment-list">
          {comments.map((c) => (
            <div key={c.id}>
              <div
                style={{ textAlign: 'right', fontSize: 12, color: 'var(--brand)', cursor: 'pointer' }}
                onClick={() => {
                  if (!token) {
                    navigate('/login')
                    return
                  }
                  setReplyTo({ commentId: c.id, name: c.user.nickname })
                }}
              >
                回复
              </div>
              <CommentItemView comment={c} />
            </div>
          ))}
        </div>

        <div className="comment-input-row">
          <input
            className="input"
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            placeholder={
              replyTo ? `回复 @${replyTo.name}…（Esc 取消）` : '友善评论，理性发言…'
            }
            onKeyDown={(e) => {
              if (e.key === 'Escape') setReplyTo(null)
            }}
          />
          <button className="btn btn-primary" onClick={submitComment} disabled={busy || !commentText.trim()}>
            发表
          </button>
        </div>
        {replyTo && (
          <div className="text-3" style={{ fontSize: 12, marginTop: 4 }}>
            正在回复 @{replyTo.name}
          </div>
        )}
      </div>

      <div className="side-card">
        <h3 className="side-title">关于创作者</h3>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 12 }}>
          <img
            className="avatar"
            src={post.author.avatar ?? ''}
            alt=""
            style={{ cursor: 'pointer' }}
            onClick={() => navigate(`/profile/${post.author.id}`)}
          />
          <div>
            <div style={{ fontWeight: 600 }}>{post.author.nickname}</div>
            <div className="text-3" style={{ fontSize: 12 }}>
              去 TA 的主页看看
            </div>
          </div>
        </div>
        <FollowButton userId={post.author.id} isFollowing={post.author.isFollowing} />
      </div>
    </div>
  )
}
