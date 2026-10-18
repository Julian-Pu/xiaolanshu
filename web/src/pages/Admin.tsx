import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../api'
import type { AdminPage, AdminPost, AdminStats, ReportItem } from '../models'

type Tab = 'stats' | 'reports' | 'posts'
const REASON_LABEL: Record<string, string> = {
  spam: '垃圾广告/营销',
  porn: '色情低俗',
  illegal: '违法违规',
  other: '其他问题',
}

const ADMIN_KEY_STORAGE = 'xls-admin-key'

/** 管理后台：X-Admin-Key 登录后查看概览 / 举报审核 / 内容管理（所有请求携带管理密钥头） */
export default function Admin() {
  const [key, setKey] = useState(() => localStorage.getItem(ADMIN_KEY_STORAGE) ?? '')
  const [authed, setAuthed] = useState(() => localStorage.getItem(ADMIN_KEY_STORAGE) != null)
  const [tab, setTab] = useState<Tab>('stats')
  const [loginError, setLoginError] = useState('')

  async function tryLogin() {
    setLoginError('')
    try {
      await adminApi.stats(key.trim())
      localStorage.setItem(ADMIN_KEY_STORAGE, key.trim())
      setAuthed(true)
    } catch (err) {
      setLoginError((err as Error).message || '密钥错误')
    }
  }

  function logout() {
    localStorage.removeItem(ADMIN_KEY_STORAGE)
    setKey('')
    setAuthed(false)
  }

  if (!authed) {
    return (
      <div className="login-wrap">
        <div className="login-card">
          <div className="login-logo">
            <span className="logo-mark">管</span>
            <div className="login-title">小蓝书管理后台</div>
            <div className="text-3" style={{ fontSize: 13 }}>请输入管理密钥（X-Admin-Key）</div>
          </div>
          <div className="form-item">
            <label>管理密钥</label>
            <input className="input" type="password" value={key} onChange={(e) => setKey(e.target.value)} placeholder="服务端 XIAOLANSHU_ADMIN_KEY" onKeyDown={(e) => e.key === 'Enter' && tryLogin()} />
          </div>
          {loginError && <div style={{ color: 'var(--danger)', fontSize: 13, marginBottom: 12 }}>{loginError}</div>}
          <button className="btn btn-primary" style={{ width: '100%', padding: '10px' }} onClick={tryLogin}>
            进入后台
          </button>
          <div style={{ textAlign: 'center', marginTop: 16, fontSize: 13 }}>
            <Link to="/" style={{ color: 'var(--primary)' }}>返回首页</Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="page">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>小蓝书管理后台</h2>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <Link to="/" className="btn" style={{ textDecoration: 'none', padding: '4px 12px', fontSize: 13 }}>返回首页</Link>
          <button className="btn" style={{ padding: '4px 12px', fontSize: 13 }} onClick={logout}>退出</button>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        {(
          [
            ['stats', '概览'],
            ['reports', '举报审核'],
            ['posts', '内容管理'],
          ] as [Tab, string][]
        ).map(([t, label]) => (
          <button
            key={t}
            className="btn"
            style={{ padding: '6px 16px', fontSize: 13, background: tab === t ? 'var(--primary)' : undefined, color: tab === t ? '#fff' : undefined }}
            onClick={() => setTab(t)}
          >
            {label}
          </button>
        ))}
      </div>
      {tab === 'stats' && <StatsPanel ak={key} />}
      {tab === 'reports' && <ReportsPanel ak={key} />}
      {tab === 'posts' && <PostsPanel ak={key} />}
    </div>
  )
}

function StatsPanel({ ak }: { ak: string }) {
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [error, setError] = useState('')
  const reload = useCallback(() => {
    adminApi
      .stats(ak)
      .then(setStats)
      .catch((e: Error) => setError(e.message))
  }, [ak])
  useEffect(reload, [reload])
  if (!stats) return <div className="text-3">{error || '加载中…'}</div>
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 12 }}>
      {(
        [
          ['注册用户', stats.userCount],
          ['笔记总数', stats.postCount],
          ['待审核举报', stats.pendingReports],
          ['评论总数', stats.commentCount],
        ] as [string, number][]
      ).map(([label, value]) => (
        <div key={label} className="card" style={{ padding: 20, textAlign: 'center' }}>
          <div style={{ fontSize: 28, fontWeight: 600, color: 'var(--primary)' }}>{value}</div>
          <div className="text-3" style={{ marginTop: 6, fontSize: 13 }}>{label}</div>
        </div>
      ))}
    </div>
  )
}

function ReportsPanel({ ak }: { ak: string }) {
  const [status, setStatus] = useState('PENDING')
  const [list, setList] = useState<ReportItem[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [error, setError] = useState('')
  const size = 20

  const load = useCallback(() => {
    adminApi
      .reports(ak, status, page, size)
      .then((r) => {
        setList(r.list)
        setTotal(r.total)
      })
      .catch((e: Error) => setError(e.message))
  }, [ak, status, page])

  useEffect(load, [load])

  async function review(id: number, action: 'APPROVE' | 'IGNORE') {
    try {
      await adminApi.review(ak, id, action)
      load()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 12, alignItems: 'center' }}>
        {(
          [
            ['PENDING', '待处理'],
            ['APPROVED', '已成立'],
            ['IGNORED', '已忽略'],
            ['', '全部'],
          ] as [string, string][]
        ).map(([v, label]) => (
          <button
            key={v}
            className="btn"
            style={{ padding: '4px 12px', fontSize: 13, background: status === v ? 'var(--primary)' : undefined, color: status === v ? '#fff' : undefined }}
            onClick={() => {
              setStatus(v)
              setPage(1)
            }}
          >
            {label}
          </button>
        ))}
      </div>
      {error && <div style={{ color: 'var(--danger)', fontSize: 13, marginBottom: 12 }}>{error}</div>}
      {list.length === 0 && <div className="text-3">暂无举报记录</div>}
      {list.map((r) => (
        <div key={r.id} className="card" style={{ padding: 14, marginBottom: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 500 }}>
                <Link to={`/post/${r.post_id}`} style={{ color: 'var(--primary)' }}>{r.post_title || `笔记 #${r.post_id}`}</Link>
                <span className="text-3" style={{ marginLeft: 8, fontSize: 12 }}>举报人：{r.reporter_nickname}</span>
              </div>
              <div className="text-3" style={{ marginTop: 4, fontSize: 12 }}>
                {REASON_LABEL[r.reason] || r.reason}
                {r.detail ? ` — ${r.detail}` : ''}
              </div>
              <div className="text-3" style={{ marginTop: 4, fontSize: 12 }}>
                {new Date(r.created_at).toLocaleString()} · {r.status === 'PENDING' ? '待处理' : r.status === 'APPROVED' ? '已成立（笔记已删除）' : '已忽略'}
              </div>
            </div>
            {r.status === 'PENDING' && (
              <div style={{ display: 'flex', gap: 8, whiteSpace: 'nowrap' }}>
                <button className="btn" style={{ padding: '4px 12px', fontSize: 12, background: 'var(--danger)', color: '#fff' }} onClick={() => review(r.id, 'APPROVE')}>
                  成立并删除
                </button>
                <button className="btn" style={{ padding: '4px 12px', fontSize: 12 }} onClick={() => review(r.id, 'IGNORE')}>
                  忽略
                </button>
              </div>
            )}
          </div>
        </div>
      ))}
      {total > size && (
        <div style={{ display: 'flex', gap: 8, marginTop: 12, alignItems: 'center' }}>
          <button className="btn" style={{ padding: '4px 12px', fontSize: 13 }} disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>上一页</button>
          <span className="text-3" style={{ fontSize: 13 }}>{page} / {Math.ceil(total / size)}</span>
          <button className="btn" style={{ padding: '4px 12px', fontSize: 13 }} disabled={page * size >= total} onClick={() => setPage((p) => p + 1)}>下一页</button>
        </div>
      )}
    </div>
  )
}

function PostsPanel({ ak }: { ak: string }) {
  const [list, setList] = useState<AdminPost[]>([])
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [error, setError] = useState('')
  const size = 20

  const load = useCallback(() => {
    adminApi
      .posts(ak, page, size)
      .then((r: AdminPage<AdminPost>) => {
        setList(r.list)
        setTotal(r.total)
      })
      .catch((e: Error) => setError(e.message))
  }, [ak, page])

  useEffect(load, [load])

  async function remove(id: number) {
    if (!window.confirm('确认删除该笔记？删除后前端不可见，不可恢复。')) return
    try {
      await adminApi.deletePost(ak, id)
      load()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <div>
      {error && <div style={{ color: 'var(--danger)', fontSize: 13, marginBottom: 12 }}>{error}</div>}
      {list.length === 0 && <div className="text-3">暂无笔记</div>}
      {list.map((p) => (
        <div key={p.id} className="card" style={{ padding: 14, marginBottom: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                <Link to={`/post/${p.id}`} style={{ color: 'var(--primary)' }}>{p.title || `无标题笔记 #${p.id}`}</Link>
                <span className="text-3" style={{ marginLeft: 8, fontSize: 12 }}>作者：{p.author_nickname}</span>
              </div>
              <div className="text-3" style={{ marginTop: 4, fontSize: 12 }}>
                {p.status === 'published' ? '已发布' : p.status} · 赞 {p.like_count} · 藏 {p.collect_count} · 评 {p.comment_count} · 览 {p.view_count} · {new Date(p.created_at).toLocaleString()}
              </div>
            </div>
            {p.status === 'published' && (
              <button className="btn" style={{ padding: '4px 12px', fontSize: 12, background: 'var(--danger)', color: '#fff', whiteSpace: 'nowrap' }} onClick={() => remove(p.id)}>
                删除
              </button>
            )}
          </div>
        </div>
      ))}
      {total > size && (
        <div style={{ display: 'flex', gap: 8, marginTop: 12, alignItems: 'center' }}>
          <button className="btn" style={{ padding: '4px 12px', fontSize: 13 }} disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>上一页</button>
          <span className="text-3" style={{ fontSize: 13 }}>{page} / {Math.ceil(total / size)}</span>
          <button className="btn" style={{ padding: '4px 12px', fontSize: 13 }} disabled={page * size >= total} onClick={() => setPage((p) => p + 1)}>下一页</button>
        </div>
      )}
    </div>
  )
}
