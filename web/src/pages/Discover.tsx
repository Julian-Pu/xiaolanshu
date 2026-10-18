import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { searchApi, topicApi } from '../api'
import type { HotWord, PostCard, Topic, User } from '../models'
import Masonry from '../components/Masonry'
import { useNavigate } from 'react-router-dom'

export default function Discover() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [topics, setTopics] = useState<Topic[]>([])
  const [hotWords, setHotWords] = useState<HotWord[]>([])
  const [posts, setPosts] = useState<PostCard[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [searched, setSearched] = useState(false)
  const [busy, setBusy] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    topicApi
      .list()
      .then(setTopics)
      .catch(() => {})
    searchApi
      .hot()
      .then(setHotWords)
      .catch(() => {})
  }, [])

  async function doSearch(keyword: string) {
    setBusy(true)
    try {
      const r = await searchApi.search(keyword)
      setPosts(r.posts)
      setUsers(r.users)
      setSearched(true)
    } finally {
      setBusy(false)
    }
  }

  function submit(e: React.FormEvent) {
    e.preventDefault()
    if (q.trim()) {
      doSearch(q.trim())
    }
  }

  return (
    <div>
      <form onSubmit={submit} style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
        <input
          ref={inputRef}
          className="input"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="搜索笔记 / 用户，如：露营、咖啡"
          style={{ flex: 1 }}
        />
        <button className="btn btn-primary" disabled={busy || !q.trim()}>
          搜索
        </button>
      </form>

      {!searched && (
        <>
          {hotWords.length > 0 && (
            <>
              <h3 className="side-title" style={{ fontSize: 16 }}>热搜榜</h3>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px 16px', marginBottom: 20 }}>
                {hotWords.map((h, i) => (
                  <div
                    key={h.word}
                    style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}
                    onClick={() => {
                      setQ(h.word)
                      doSearch(h.word)
                    }}
                  >
                    <span
                      style={{
                        fontWeight: 700,
                        color: i < 3 ? 'var(--danger)' : 'var(--text-3)',
                        fontSize: 14,
                      }}
                    >
                      {i + 1}
                    </span>
                    <span style={{ fontWeight: 500 }}>{h.word}</span>
                    <span className="text-3" style={{ fontSize: 12 }}>{h.cnt}</span>
                  </div>
                ))}
              </div>
            </>
          )}
          <h3 className="side-title" style={{ fontSize: 16 }}>话题广场</h3>
          <div className="topic-grid">
            {topics.map((t) => (
              <div
                key={t.id}
                className="topic-card"
                onClick={() => {
                  setQ(`#${t.name}`)
                  doSearch(t.name)
                }}
              >
                {t.cover && <img src={t.cover} alt="" />}
                <div style={{ fontWeight: 600 }}>#{t.name}</div>
                <div className="text-3" style={{ fontSize: 12 }}>{t.postCount} 篇笔记</div>
              </div>
            ))}
          </div>
        </>
      )}

      {searched && (
        <>
          {users.length > 0 && (
            <>
              <h3 className="side-title">用户</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {users.map((u) => (
                  <div
                    key={u.id}
                    className="side-card"
                    style={{ display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer' }}
                    onClick={() => navigate(`/profile/${u.id}`)}
                  >
                    <img className="avatar" src={u.avatar ?? ''} alt="" />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 600 }}>{u.nickname}</div>
                      <div className="text-3" style={{ fontSize: 12 }}>
                        {u.followerCount} 粉丝 · {u.postCount} 笔记
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
          <h3 className="side-title">笔记</h3>
          <Masonry posts={posts} />
        </>
      )}
    </div>
  )
}
