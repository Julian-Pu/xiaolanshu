import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { postApi, topicApi, uploadApi } from '../api'
import type { Topic } from '../models'

/** 预设滤镜（CSS filter，与 Android 端 ColorMatrix 6 种对应） */
const FILTERS: { name: string; css: string }[] = [
  { name: '原图', css: 'none' },
  { name: '鲜明', css: 'saturate(1.5) contrast(1.1)' },
  { name: '暖阳', css: 'sepia(0.25) saturate(1.3) hue-rotate(-10deg)' },
  { name: '冷调', css: 'saturate(1.1) hue-rotate(10deg) brightness(1.05)' },
  { name: '黑白', css: 'grayscale(1)' },
  { name: '胶片', css: 'sepia(0.15) contrast(1.05) brightness(1.02)' },
]

export default function Publish() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [images, setImages] = useState<string[]>([])
  const [videoUrl, setVideoUrl] = useState<string | undefined>()
  const [videoCover, setVideoCover] = useState<string | undefined>()
  const [topics, setTopics] = useState<Topic[]>([])
  const [topicId, setTopicId] = useState<number | undefined>()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [uploadingVideo, setUploadingVideo] = useState(false)
  const [uploadingCover, setUploadingCover] = useState(false)
  const [hasDraft, setHasDraft] = useState(false)
  const [filterTarget, setFilterTarget] = useState<number | null>(null) // 正在加滤镜的图片下标
  const [applyingFilter, setApplyingFilter] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)
  const videoRef = useRef<HTMLInputElement>(null)
  const coverRef = useRef<HTMLInputElement>(null)

  // 恢复草稿 + 自动保存草稿（localStorage）
  useEffect(() => {
    try {
      const d = JSON.parse(localStorage.getItem('xls-publish-draft') ?? 'null')
      if (d) {
        if (d.title) setTitle(d.title)
        if (d.content) setContent(d.content)
        if (d.images) setImages(d.images)
        if (d.videoUrl) setVideoUrl(d.videoUrl)
        if (d.videoCover) setVideoCover(d.videoCover)
        if (d.topicId) setTopicId(d.topicId)
        setHasDraft(true)
      }
    } catch {
      /* 忽略损坏草稿 */
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    const dirty =
      title.trim() || content.trim() || images.length > 0 || videoUrl || topicId
    if (!dirty) return
    localStorage.setItem(
      'xls-publish-draft',
      JSON.stringify({ title, content, images, videoUrl, videoCover, topicId }),
    )
    setHasDraft(true)
  }, [title, content, images, videoUrl, videoCover, topicId])

  function clearDraft() {
    localStorage.removeItem('xls-publish-draft')
    setTitle('')
    setContent('')
    setImages([])
    setVideoUrl(undefined)
    setVideoCover(undefined)
    setTopicId(undefined)
    setHasDraft(false)
  }

  useEffect(() => {
    topicApi
      .list()
      .then((list) => {
        setTopics(list)
        if (list.length > 0) setTopicId(list[0].id)
      })
      .catch(() => {})
  }, [])

  async function onPickFiles(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? [])
    if (files.length === 0) return
    setUploading(true)
    try {
      const urls: string[] = []
      for (const f of files) {
        const r = await uploadApi.image(f)
        urls.push(r.url)
      }
      setImages((prev) => [...prev, ...urls].slice(0, 9))
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setUploading(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  async function onPickVideo(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (!f) return
    setUploadingVideo(true)
    try {
      const r = await uploadApi.video(f)
      setVideoUrl(r.url)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setUploadingVideo(false)
      if (videoRef.current) videoRef.current.value = ''
    }
  }

  async function onPickCover(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (!f) return
    setUploadingCover(true)
    try {
      const r = await uploadApi.image(f)
      setVideoCover(r.url)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setUploadingCover(false)
      if (coverRef.current) coverRef.current.value = ''
    }
  }

  /** 设为封面：移到列表首位（第一张即封面） */
  function setCover(idx: number) {
    setImages((prev) => {
      if (prev.length <= 1 || idx === 0) return prev
      const [target] = prev.splice(idx, 1)
      return [target, ...prev]
    })
  }

  /** Canvas 应用滤镜 → 上传替换原图 */
  async function applyFilter(idx: number, css: string) {
    const url = images[idx]
    if (!url || css === 'none') {
      setFilterTarget(null)
      return
    }
    setApplyingFilter(true)
    try {
      const img = new Image()
      img.src = url
      await img.decode()
      const canvas = document.createElement('canvas')
      canvas.width = img.naturalWidth
      canvas.height = img.naturalHeight
      const ctx = canvas.getContext('2d')
      if (!ctx) throw new Error('画布不可用')
      ctx.filter = css
      ctx.drawImage(img, 0, 0)
      const blob: Blob = await new Promise((resolve, reject) =>
        canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('导出失败'))), 'image/jpeg', 0.9),
      )
      const r = await uploadApi.image(new File([blob], `edited_${Date.now()}.jpg`, { type: 'image/jpeg' }))
      setImages((prev) => prev.map((u, i) => (i === idx ? r.url : u)))
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setApplyingFilter(false)
      setFilterTarget(null)
    }
  }

  async function submit() {
    setError('')
    if (!title.trim() && !content.trim()) {
      setError('标题或正文至少填写一项')
      return
    }
    if (images.length === 0 && !videoUrl) {
      setError('请至少添加一张图片或一个视频')
      return
    }
    setBusy(true)
    try {
      const post = await postApi.create({
        title: title.trim() || undefined,
        content: content.trim() || undefined,
        images,
        videoUrl: videoUrl || undefined,
        videoCover: videoCover || undefined,
        topicId,
      })
      localStorage.removeItem('xls-publish-draft')
      navigate(`/post/${post.id}`, { replace: true })
    } catch (err) {
      setError((err as Error).message)
      setBusy(false)
    }
  }

  return (
    <div className="publish-form">
      <h2 style={{ marginTop: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        发布笔记
        {hasDraft && (
          <span
            className="text-3"
            style={{ fontSize: 13, cursor: 'pointer', fontWeight: 400 }}
            onClick={clearDraft}
            title="清除草稿"
          >
            ● 草稿已保存（点击清除）
          </span>
        )}
      </h2>
      <div className="form-item">
        <label>标题</label>
        <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} placeholder="一句话介绍你的分享" />
      </div>
      <div className="form-item">
        <label>正文</label>
        <textarea className="input" value={content} onChange={(e) => setContent(e.target.value)} placeholder="写点什么…" />
      </div>
      <div className="form-item">
        <label>图片（最多 9 张，第一张为封面）</label>
        <div className="preview-grid">
          {images.map((url, i) => (
            <div key={url + i} style={{ position: 'relative' }}>
              <img
                className="preview-img"
                src={url}
                alt=""
                onClick={() => setFilterTarget(i)}
                title="点击加滤镜"
                style={filterTarget === i ? { outline: '2px solid var(--primary)' } : undefined}
              />
              {i === 0 && (
                <span
                  style={{
                    position: 'absolute',
                    left: 4,
                    bottom: 4,
                    background: 'var(--primary)',
                    color: '#fff',
                    fontSize: 11,
                    borderRadius: 4,
                    padding: '1px 5px',
                  }}
                >
                  封面
                </span>
              )}
              <span
                style={{
                  position: 'absolute',
                  top: 2,
                  right: 2,
                  width: 18,
                  height: 18,
                  borderRadius: 9,
                  background: 'rgba(0,0,0,0.55)',
                  color: '#fff',
                  fontSize: 12,
                  lineHeight: '18px',
                  textAlign: 'center',
                  cursor: 'pointer',
                }}
                onClick={(e) => {
                  e.stopPropagation()
                  setImages((prev) => prev.filter((_, idx) => idx !== i))
                }}
                title="移除"
              >
                ×
              </span>
              {i > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    left: 4,
                    top: 2,
                    fontSize: 11,
                    color: 'var(--primary)',
                    background: 'rgba(255,255,255,0.85)',
                    borderRadius: 4,
                    padding: '1px 5px',
                    cursor: 'pointer',
                  }}
                  onClick={(e) => {
                    e.stopPropagation()
                    setCover(i)
                  }}
                  title="设为封面"
                >
                  设为封面
                </span>
              )}
            </div>
          ))}
          {images.length < 9 && (
            <div
              style={{
                width: 96,
                height: 96,
                border: '1px dashed var(--border)',
                borderRadius: 8,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                color: 'var(--text-3)',
              }}
              onClick={() => fileRef.current?.click()}
            >
              {uploading ? '上传中…' : '+ 添加'}
            </div>
          )}
        </div>
        {filterTarget !== null && (
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 8 }}>
            {FILTERS.map((f) => (
              <button
                key={f.name}
                className="btn"
                style={{ padding: '2px 10px', fontSize: 12 }}
                disabled={applyingFilter}
                onClick={() => applyFilter(filterTarget, f.css)}
              >
                {f.name}
              </button>
            ))}
            <button
              className="btn"
              style={{ padding: '2px 10px', fontSize: 12 }}
              onClick={() => setFilterTarget(null)}
            >
              取消
            </button>
          </div>
        )}
        <input ref={fileRef} type="file" accept="image/*" multiple hidden onChange={onPickFiles} />
      </div>
      <div className="form-item">
        <label>视频（可选，添加后仅保留视频笔记）</label>
        {!videoUrl ? (
          <div
            style={{
              width: 180,
              height: 96,
              border: '1px dashed var(--border)',
              borderRadius: 8,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              color: 'var(--text-3)',
            }}
            onClick={() => videoRef.current?.click()}
          >
            {uploadingVideo ? '上传中…' : '+ 添加视频'}
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {videoCover ? (
              <img className="preview-img" src={videoCover} alt="封面" style={{ width: 96, height: 96, objectFit: 'cover' }} />
            ) : (
              <div
                style={{
                  width: 96,
                  height: 96,
                  borderRadius: 8,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: 'var(--bg-2)',
                  color: 'var(--brand)',
                  fontSize: 28,
                }}
              >
                ▶
              </div>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <span style={{ fontSize: 13 }}>视频已添加</span>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn" onClick={() => coverRef.current?.click()}>
                  {uploadingCover ? '上传中…' : '更换封面'}
                </button>
                <button
                  className="btn btn-danger"
                  onClick={() => {
                    setVideoUrl(undefined)
                    setVideoCover(undefined)
                  }}
                >
                  移除
                </button>
              </div>
            </div>
          </div>
        )}
        <input ref={videoRef} type="file" accept="video/*" hidden onChange={onPickVideo} />
        <input ref={coverRef} type="file" accept="image/*" hidden onChange={onPickCover} />
      </div>
      <div className="form-item">
        <label>话题</label>
        <select className="input" value={topicId ?? ''} onChange={(e) => setTopicId(Number(e.target.value) || undefined)}>
          {topics.map((t) => (
            <option key={t.id} value={t.id}>
              #{t.name}
            </option>
          ))}
        </select>
      </div>
      {error && <div style={{ color: 'var(--danger)', marginBottom: 12 }}>{error}</div>}
      <button className="btn btn-primary" onClick={submit} disabled={busy || uploading}>
        {busy ? '发布中…' : '发布'}
      </button>
    </div>
  )
}
