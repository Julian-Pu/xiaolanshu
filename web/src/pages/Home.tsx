import { useCallback, useEffect, useRef, useState } from 'react'
import { feedApi, topicApi } from '../api'
import type { PostCard } from '../models'
import Masonry from '../components/Masonry'

interface Channel {
  label: string
  tab: string
  topicId?: number
}

export default function Home() {
  const [channels, setChannels] = useState<Channel[]>([
    { label: '推荐', tab: 'recommend' },
    { label: '关注', tab: 'following' },
  ])
  const [selected, setSelected] = useState(0)
  const [posts, setPosts] = useState<PostCard[]>([])
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(false)
  const [initializing, setInitializing] = useState(true)
  const mounted = useRef(true)

  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  // 加载话题频道
  useEffect(() => {
    topicApi
      .list()
      .then((topics) => {
        setChannels([
          { label: '推荐', tab: 'recommend' },
          { label: '关注', tab: 'following' },
          ...topics.map((t) => ({ label: `#${t.name}`, tab: 'topic', topicId: t.id })),
        ])
      })
      .catch(() => {})
  }, [])

  const load = useCallback(
    async (p: number, append: boolean) => {
      setLoading(true)
      try {
        const ch = channels[selected]
        const r = await feedApi.feed(ch.tab, p, 10, ch.topicId)
        if (!mounted.current) return
        setPosts((prev) => (append ? [...prev, ...r.list] : r.list))
        setHasMore(r.hasMore)
        setPage(p)
      } catch {
        // 忽略
      } finally {
        if (mounted.current) {
          setLoading(false)
          setInitializing(false)
        }
      }
    },
    [selected, channels],
  )

  useEffect(() => {
    setInitializing(true)
    load(1, false)
  }, [load])

  return (
    <div>
      <div
        style={{
          display: 'flex',
          gap: 8,
          marginBottom: 16,
          overflowX: 'auto',
          paddingBottom: 4,
        }}
      >
        {channels.map((ch, i) => (
          <button
            key={`${ch.tab}-${ch.topicId ?? ''}-${i}`}
            className={`btn${i === selected ? ' btn-primary' : ''}`}
            style={{ whiteSpace: 'nowrap', flexShrink: 0, padding: '4px 14px' }}
            onClick={() => setSelected(i)}
          >
            {ch.label}
          </button>
        ))}
      </div>

      {initializing ? (
        <div className="loading">加载中…</div>
      ) : (
        <>
          <Masonry posts={posts} />
          {hasMore ? (
            <div style={{ textAlign: 'center', padding: '16px 0 32px' }}>
              <button className="btn" onClick={() => load(page + 1, true)} disabled={loading}>
                {loading ? '加载中…' : '加载更多'}
              </button>
            </div>
          ) : (
            posts.length > 0 && (
              <div style={{ textAlign: 'center', color: 'var(--text-3)', padding: '16px 0 32px', fontSize: 13 }}>
                — 已经到底啦 —
              </div>
            )
          )}
        </>
      )}
    </div>
  )
}
