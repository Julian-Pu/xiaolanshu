import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { messageApi, notifApi } from '../api'
import type { Conversation, Message, NotificationItem } from '../models'
import { timeAgo, useAuth } from '../store/auth'

type Panel = 'notif' | 'chat'

export default function Messages() {
  const navigate = useNavigate()
  const me = useAuth((s) => s.user)
  const [panel, setPanel] = useState<Panel>('notif')
  const [notifs, setNotifs] = useState<NotificationItem[]>([])
  const [convs, setConvs] = useState<Conversation[]>([])
  const [activeConv, setActiveConv] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [draft, setDraft] = useState('')
  const [busy, setBusy] = useState(false)
  const [typingConv, setTypingConv] = useState<number | null>(null)
  const wsRef = useRef<WebSocket | null>(null)

  const loadNotifs = useCallback(async () => {
    try {
      const r = await notifApi.list()
      setNotifs(r.list)
    } catch {
      // 忽略
    }
  }, [])

  const loadConvs = useCallback(async () => {
    try {
      setConvs(await messageApi.conversations())
    } catch {
      // 忽略
    }
  }, [])

  useEffect(() => {
    loadNotifs()
    loadConvs()
  }, [loadNotifs, loadConvs])

  // WebSocket 实时收私信 + typing / read_ack 帧
  useEffect(() => {
    const token = localStorage.getItem('xls_token')
    if (!token) return
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    const ws = new WebSocket(`${proto}://${location.host}/ws`)
    wsRef.current = ws
    ws.onopen = () => ws.send(JSON.stringify({ type: 'auth', token }))
    ws.onmessage = (ev) => {
      try {
        const data = JSON.parse(ev.data)
        if (data.type === 'message') {
          const msg = data.message as Message
          setMessages((prev) => {
            if (prev.some((m) => m.id === msg.id)) return prev
            return [...prev, msg]
          })
          loadConvs()
        } else if (data.type === 'typing') {
          setTypingConv(data.conversationId as number)
          setTimeout(() => setTypingConv((cur) => (cur === data.conversationId ? null : cur)), 3000)
        } else if (data.type === 'read_ack') {
          const convId = data.conversationId as number
          setMessages((prev) => prev.map((m) => (m.conversationId === convId ? { ...m, isRead: true } : m)))
        } else if (data.type === 'notify') {
          // 多端实时通知推送：新通知到达，刷新列表（完整数据走 REST）
          loadNotifs()
        }
      } catch {
        // 忽略非私信帧
      }
    }
    return () => {
      ws.close()
      wsRef.current = null
    }
  }, [loadConvs])

  async function openConv(conv: Conversation) {
    setActiveConv(conv)
    try {
      const r = await messageApi.messages(conv.id)
      setMessages(r.list)
      // 已读回执：WS read 帧 + REST 兜底
      wsRef.current?.send(JSON.stringify({ type: 'read', conversationId: conv.id }))
      messageApi.markRead(conv.id).then(loadConvs).catch(() => {})
    } catch {
      // 忽略
    }
  }

  async function send() {
    if (!activeConv || !draft.trim()) return
    setBusy(true)
    try {
      await messageApi.send(activeConv.id, draft.trim())
      setDraft('')
      const r = await messageApi.messages(activeConv.id)
      setMessages(r.list)
      loadConvs()
    } catch {
      // 忽略
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        <button className={`btn${panel === 'notif' ? ' btn-primary' : ''}`} onClick={() => setPanel('notif')}>
          通知
        </button>
        <button className={`btn${panel === 'chat' ? ' btn-primary' : ''}`} onClick={() => setPanel('chat')}>
          私信
        </button>
      </div>

      {panel === 'notif' && (
        <div className="side-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <h3 className="side-title" style={{ margin: 0 }}>互动通知</h3>
            <button
              className="btn"
              onClick={async () => {
                try {
                  await notifApi.readAll()
                  loadNotifs()
                } catch {
                  // 忽略
                }
              }}
            >
              全部已读
            </button>
          </div>
          {notifs.length === 0 && <div className="empty">暂无通知</div>}
          {notifs.map((n) => (
            <div key={n.id} className="notif-item" style={{ cursor: n.refId ? 'pointer' : 'default' }}
              onClick={() => n.refId && navigate(`/post/${n.refId}`)}>
              {!n.isRead && <span className="notif-dot" />}
              <img className="avatar-sm" src={n.actor?.avatar ?? ''} alt="" />
              <div style={{ flex: 1, minWidth: 0 }}>
                <span style={{ fontWeight: 500 }}>{n.actor?.nickname ?? '系统'}</span>{' '}
                {n.content}
                <div className="text-3" style={{ fontSize: 12 }}>{timeAgo(n.createdAt)}</div>
              </div>
            </div>
          ))}
        </div>
      )}

      {panel === 'chat' && (
        <div className="msg-layout">
          <div className="side-card" style={{ padding: 8 }}>
            {convs.length === 0 && <div className="empty">还没有会话，去别人的主页发私信吧</div>}
            {convs.map((c) => (
              <div
                key={c.id}
                className={`msg-list-item${activeConv?.id === c.id ? ' active' : ''}`}
                onClick={() => openConv(c)}
                style={{ display: 'flex', alignItems: 'center', gap: 10 }}
              >
                <img className="avatar-sm" src={c.peer.avatar ?? ''} alt="" />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 500 }}>{c.peer.nickname}</div>
                  <div className="text-3" style={{ fontSize: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {c.lastMessage || '开始聊天'}
                  </div>
                </div>
                {!!c.unreadCount && c.unreadCount > 0 && (
                  <span
                    style={{
                      background: 'var(--danger, #e5484d)',
                      color: '#fff',
                      borderRadius: 10,
                      fontSize: 11,
                      padding: '1px 7px',
                      minWidth: 18,
                      textAlign: 'center',
                    }}
                  >
                    {c.unreadCount > 99 ? '99+' : c.unreadCount}
                  </span>
                )}
              </div>
            ))}
          </div>
          <div className="side-card" style={{ display: 'flex', flexDirection: 'column', minHeight: 420 }}>
            {!activeConv ? (
              <div className="empty">选择一个会话开始聊天</div>
            ) : (
              <>
                <div style={{ fontWeight: 600, borderBottom: '1px solid var(--border)', paddingBottom: 10, marginBottom: 10 }}>
                  {activeConv.peer.nickname}
                  {typingConv === activeConv.id && (
                    <span style={{ color: 'var(--brand)', fontSize: 12, fontWeight: 400, marginLeft: 10 }}>
                      对方正在输入…
                    </span>
                  )}
                </div>
                <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {messages.map((m) => {
                    const mine = me?.id === m.senderId
                    return (
                      <div
                        key={m.id}
                        style={{
                          alignSelf: mine ? 'flex-end' : 'flex-start',
                          background: mine ? 'var(--brand)' : 'var(--bg)',
                          color: mine ? '#fff' : 'var(--text)',
                          borderRadius: 12,
                          padding: '8px 12px',
                          maxWidth: '70%',
                          fontSize: 14,
                        }}
                      >
                        {m.content}
                        <div style={{ fontSize: 11, opacity: 0.7, marginTop: 2 }}>
                          {mine && (m.isRead ? '已读 · ' : '未读 · ')}
                          {timeAgo(m.createdAt)}
                        </div>
                      </div>
                    )
                  })}
                </div>
                <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
                  <input
                    className="input"
                    value={draft}
                    onChange={(e) => {
                      setDraft(e.target.value)
                      if (activeConv) wsRef.current?.send(JSON.stringify({ type: 'typing', conversationId: activeConv.id }))
                    }}
                    placeholder="输入消息…"
                    onKeyDown={(e) => e.key === 'Enter' && send()}
                  />
                  <button className="btn btn-primary" onClick={send} disabled={busy || !draft.trim()}>
                    发送
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
