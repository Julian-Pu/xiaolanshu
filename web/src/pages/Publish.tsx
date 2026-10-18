import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { postApi, topicApi, uploadApi } from '../api'
import type { Topic } from '../models'

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
        <label>图片（最多 9 张）</label>
        <div className="preview-grid">
          {images.map((url, i) => (
            <img
              key={url + i}
              className="preview-img"
              src={url}
              alt=""
              onClick={() => setImages((prev) => prev.filter((_, idx) => idx !== i))}
              title="点击移除"
            />
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
