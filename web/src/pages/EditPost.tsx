import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { postApi, topicApi } from '../api'
import type { Topic } from '../models'

/** 编辑笔记：标题 / 内容 / 话题（图片与视频保持原样，由后端保留未传字段） */
export default function EditPost() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [topics, setTopics] = useState<Topic[]>([])
  const [topicId, setTopicId] = useState<number | null>(null)
  const [busy, setBusy] = useState(false)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    if (!id) return
    ;(async () => {
      try {
        const p = await postApi.detail(Number(id))
        setTitle(p.title ?? '')
        setContent(p.content ?? '')
        setTopicId(p.topic?.id ?? null)
      } catch {
        navigate('/', { replace: true })
      }
      try {
        setTopics(await topicApi.list())
      } catch {
        // 忽略
      }
      setLoaded(true)
    })()
  }, [id, navigate])

  async function save() {
    if (!id) return
    setBusy(true)
    try {
      await postApi.update(Number(id), {
        title,
        content,
        topicId: topicId ?? undefined,
      })
      navigate(`/post/${id}`)
    } catch {
      // 忽略
    } finally {
      setBusy(false)
    }
  }

  if (!loaded) {
    return <div className="loading">加载中…</div>
  }

  return (
    <div className="side-card" style={{ maxWidth: 560 }}>
      <h2 className="side-title">编辑笔记</h2>
      <div className="form-item">
        <label>标题</label>
        <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={50} />
      </div>
      <div className="form-item">
        <label>内容</label>
        <textarea className="input" value={content} onChange={(e) => setContent(e.target.value)} rows={5} />
      </div>
      <div className="form-item">
        <label>话题</label>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {topics.map((t) => (
            <button
              key={t.id}
              className={`topic-tag${topicId === t.id ? ' active' : ''}`}
              style={{
                background: topicId === t.id ? 'var(--brand)' : 'transparent',
                color: topicId === t.id ? '#fff' : 'var(--brand)',
                border: '1px solid var(--brand)',
                borderRadius: 16,
                padding: '4px 12px',
                cursor: 'pointer',
              }}
              onClick={() => setTopicId(topicId === t.id ? null : t.id)}
            >
              #{t.name}
            </button>
          ))}
        </div>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <button className="btn" onClick={() => navigate(-1)}>取消</button>
        <button className="btn btn-primary" onClick={save} disabled={busy || (!title.trim() && !content.trim())}>
          {busy ? '保存中…' : '保存'}
        </button>
      </div>
    </div>
  )
}
