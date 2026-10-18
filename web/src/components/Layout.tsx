import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { topicApi } from '../api'
import type { Topic } from '../models'
import { timeAgo, useAuth } from '../store/auth'
import FollowButton from './FollowButton'

export default function Layout() {
  const user = useAuth((s) => s.user)
  const logout = useAuth((s) => s.logout)
  const navigate = useNavigate()
  const [topics, setTopics] = useState<Topic[]>([])
  const [dark, setDark] = useState(() => localStorage.getItem('xls-theme') === 'dark')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    localStorage.setItem('xls-theme', dark ? 'dark' : 'light')
  }, [dark])

  useEffect(() => {
    topicApi
      .list()
      .then(setTopics)
      .catch(() => {})
  }, [])

  const navItems = [
    { to: '/', label: '首页', icon: '🏠' },
    { to: '/discover', label: '发现', icon: '🔍' },
    { to: '/publish', label: '发布', icon: '✏️' },
    { to: '/messages', label: '消息', icon: '💬' },
    { to: '/albums', label: '专辑', icon: '📁' },
    { to: '/history', label: '浏览记录', icon: '🕘' },
    { to: '/profile/me', label: '我的', icon: '👤' },
  ]

  return (
    <div className="app-shell">
      <aside className="left-nav">
        <div className="logo" onClick={() => navigate('/')}>
          <span className="logo-mark">蓝</span>
          <span className="nav-label">小蓝书</span>
        </div>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <span>{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </NavLink>
        ))}
      </aside>

      <main className="content">
        <Outlet />
      </main>

      <aside className="right-side">
        {user ? (
          <div className="side-card">
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <img
                className="avatar"
                src={user.avatar ?? ''}
                alt=""
                onClick={() => navigate('/profile/me')}
                style={{ cursor: 'pointer' }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 600 }}>{user.nickname}</div>
                <div className="text-3" style={{ fontSize: 12 }}>
                  加入于 {timeAgo(user.createdAt) || '近期'}
                </div>
              </div>
              <button className="btn" onClick={() => navigate('/settings')}>
                设置
              </button>
              <button className="btn" onClick={() => { logout(); navigate('/') }}>
                退出
              </button>
            </div>
            <div style={{ display: 'flex', gap: 16, marginTop: 12, color: 'var(--text-2)', fontSize: 13 }}>
              <span><b>{user.followingCount}</b> 关注</span>
              <span><b>{user.followerCount}</b> 粉丝</span>
              <span><b>{user.postCount}</b> 笔记</span>
            </div>
          </div>
        ) : (
          <div className="side-card">
            <div style={{ marginBottom: 12 }}>登录后发布笔记、点赞收藏、关注创作者</div>
            <button className="btn btn-primary" onClick={() => navigate('/login')}>
              登录 / 注册
            </button>
          </div>
        )}

        {topics.length > 0 && (
          <div className="side-card">
            <h3 className="side-title">热门话题</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {topics.slice(0, 5).map((t) => (
                <div key={t.id} className="text-2" style={{ cursor: 'pointer' }} onClick={() => navigate(`/discover?topic=${t.id}`)}>
                  <span style={{ color: 'var(--brand)' }}>#</span> {t.name}
                  <span className="text-3" style={{ float: 'right', fontSize: 12 }}>
                    {t.postCount} 篇
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="side-card text-3" style={{ fontSize: 12 }}>
          小蓝书 · 记录生活的美好<br />
          演示工程：未使用任何品牌素材与接口
        </div>

        <div className="side-card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span className="text-3" style={{ fontSize: 13 }}>外观</span>
          <button className="theme-toggle" onClick={() => setDark((d) => !d)}>
            {dark ? '☀️ 浅色' : '🌙 深色'}
          </button>
        </div>
      </aside>
    </div>
  )
}
