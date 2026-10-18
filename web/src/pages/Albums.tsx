import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { albumApi } from '../api'
import type { Album, PostCard } from '../models'
import Masonry from '../components/Masonry'

/** 收藏专辑：专辑列表 + 新建/删除 + 查看专辑内笔记 */
export default function Albums() {
  const navigate = useNavigate()
  const [albums, setAlbums] = useState<Album[]>([])
  const [current, setCurrent] = useState<Album | null>(null)
  const [posts, setPosts] = useState<PostCard[]>([])
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)

  const loadAlbums = async () => {
    try {
      setAlbums(await albumApi.list())
    } catch {
      // 忽略
    }
  }

  useEffect(() => {
    loadAlbums()
  }, [])

  useEffect(() => {
    if (current) {
      albumApi
        .posts(current.id)
        .then((r) => setPosts(r.list))
        .catch(() => {})
    }
  }, [current])

  async function create() {
    if (!name.trim()) return
    setBusy(true)
    try {
      await albumApi.create(name.trim())
      setName('')
      await loadAlbums()
    } catch {
      // 忽略
    } finally {
      setBusy(false)
    }
  }

  async function removeAlbum(a: Album) {
    if (!window.confirm(`删除专辑「${a.name}」？专辑内的收藏会被一并移除（收藏本身不受影响）。`)) return
    try {
      await albumApi.remove(a.id)
      if (current?.id === a.id) {
        setCurrent(null)
        setPosts([])
      }
      await loadAlbums()
    } catch {
      // 忽略
    }
  }

  async function removePost(postId: number) {
    if (!current) return
    try {
      await albumApi.removePost(current.id, postId)
      setPosts((p) => p.filter((x) => x.id !== postId))
    } catch {
      // 忽略
    }
  }

  return (
    <div>
      <div className="side-card" style={{ marginBottom: 16 }}>
        <h2 className="side-title">收藏专辑</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <input
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="新建专辑名称（最多 50 字）"
            maxLength={50}
            onKeyDown={(e) => e.key === 'Enter' && create()}
          />
          <button className="btn btn-primary" onClick={create} disabled={busy || !name.trim()}>
            新建
          </button>
        </div>
        {albums.length === 0 && <div className="text-3" style={{ marginTop: 12 }}>还没有专辑，创建一个来整理你的收藏吧</div>}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 12 }}>
          {albums.map((a) => (
            <button
              key={a.id}
              className="btn"
              style={{
                background: current?.id === a.id ? 'var(--brand)' : undefined,
                color: current?.id === a.id ? '#fff' : undefined,
              }}
              onClick={() => setCurrent(current?.id === a.id ? null : a)}
            >
              {a.name}（{a.postCount}）
            </button>
          ))}
        </div>
      </div>

      {current ? (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
            <h3 className="side-title" style={{ margin: 0 }}>
              专辑「{current.name}」的笔记
            </h3>
            <button className="btn btn-danger" onClick={() => removeAlbum(current)}>
              删除专辑
            </button>
          </div>
          {posts.length === 0 ? (
            <div className="loading">这个专辑还没有笔记</div>
          ) : (
            <div className="masonry">
              {posts.map((p) => (
                <div key={p.id} className="masonry-item" style={{ position: 'relative' }}>
                  <div
                    onClick={() => navigate(`/post/${p.id}`)}
                    style={{ cursor: 'pointer' }}
                  >
                    <MasonryItem post={p} />
                  </div>
                  <button
                    className="btn btn-danger"
                    style={{ position: 'absolute', top: 8, right: 8, fontSize: 12, padding: '2px 8px' }}
                    onClick={() => removePost(p.id)}
                  >
                    移出
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      ) : (
        <div className="text-3">选择上方专辑查看其中的收藏</div>
      )}
    </div>
  )
}

function MasonryItem({ post }: { post: PostCard }) {
  return (
    <div className="feed-card">
      {post.videoUrl ? (
        <div className="feed-cover" style={{ position: 'relative', aspectRatio: '3 / 4', background: '#000' }}>
          <img src={post.cover ?? ''} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          <span className="video-badge">▶</span>
        </div>
      ) : (
        <img className="feed-cover" src={post.cover ?? ''} alt="" />
      )}
      <div className="feed-title">{post.title || ''}</div>
    </div>
  )
}
