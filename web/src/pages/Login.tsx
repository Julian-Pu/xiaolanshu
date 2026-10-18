import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api'
import { useAuth } from '../store/auth'

type Mode = 'login' | 'register'

export default function Login() {
  const navigate = useNavigate()
  const setAuth = useAuth((s) => s.setAuth)
  const [mode, setMode] = useState<Mode>('login')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [smsCode, setSmsCode] = useState('')
  const [smsHint, setSmsHint] = useState('')
  const [sendingSms, setSendingSms] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function sendSms() {
    setError('')
    if (!/^1\d{10}$/.test(phone)) {
      setError('请先输入正确的 11 位手机号')
      return
    }
    setSendingSms(true)
    try {
      const r = await authApi.sendSmsCode({ phone })
      setSmsHint(r.code ? `开发模式验证码：${r.code}（已打印到服务器日志）` : '验证码已发送，请查看后端控制台日志')
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setSendingSms(false)
    }
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    if (!/^1\d{10}$/.test(phone)) {
      setError('请输入正确的 11 位手机号')
      return
    }
    if (password.length < 6) {
      setError('密码至少 6 位')
      return
    }
    setBusy(true)
    try {
      if (mode === 'register') {
        if (!nickname.trim()) {
          setError('请填写昵称')
          return
        }
        if (!smsCode.trim()) {
          setError('请先获取并输入短信验证码')
          return
        }
        const r = await authApi.register({ phone, password, nickname: nickname.trim(), smsCode: smsCode.trim() })
        setAuth(r.token, r.user)
      } else {
        const r = await authApi.login({ phone, password })
        setAuth(r.token, r.user)
      }
      navigate('/', { replace: true })
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="login-logo">
          <span className="logo-mark">蓝</span>
          <div className="login-title">小蓝书</div>
          <div className="text-3" style={{ fontSize: 13 }}>记录生活的美好</div>
        </div>
        <form onSubmit={submit}>
          <div className="form-item">
            <label>手机号</label>
            <input className="input" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="11 位手机号" maxLength={11} />
          </div>
          {mode === 'register' && (
            <div className="form-item">
              <label>昵称</label>
              <input className="input" value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="给自己起个昵称" maxLength={20} />
            </div>
          )}
          {mode === 'register' && (
            <div className="form-item">
              <label>短信验证码</label>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  className="input"
                  value={smsCode}
                  onChange={(e) => setSmsCode(e.target.value)}
                  placeholder="6 位验证码"
                  maxLength={6}
                  style={{ flex: 1 }}
                />
                <button className="btn" type="button" style={{ whiteSpace: 'nowrap', padding: '0 14px' }} disabled={sendingSms} onClick={sendSms}>
                  {sendingSms ? '发送中…' : '获取验证码'}
                </button>
              </div>
              {smsHint && <div className="text-3" style={{ marginTop: 6, fontSize: 12 }}>{smsHint}</div>}
            </div>
          )}
          <div className="form-item">
            <label>密码</label>
            <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="至少 6 位" />
          </div>
          {error && <div style={{ color: 'var(--danger)', fontSize: 13, marginBottom: 12 }}>{error}</div>}
          <button className="btn btn-primary" style={{ width: '100%', padding: '10px' }} disabled={busy}>
            {busy ? '请稍候…' : mode === 'login' ? '登录' : '注册并登录'}
          </button>
        </form>
        <div style={{ textAlign: 'center', marginTop: 16, fontSize: 13 }}>
          {mode === 'login' ? '还没有账号？' : '已有账号？'}{' '}
          <a
            href="#"
            onClick={(e) => {
              e.preventDefault()
              setMode(mode === 'login' ? 'register' : 'login')
              setError('')
            }}
          >
            {mode === 'login' ? '立即注册' : '去登录'}
          </a>
        </div>
        <div className="text-3" style={{ marginTop: 12, fontSize: 12, textAlign: 'center' }}>
          演示账号：13800000001~5 / 123456
        </div>
        <div className="text-3" style={{ marginTop: 12, fontSize: 12, textAlign: 'center' }}>
          登录/注册即代表同意{' '}
          <a href="#/terms" style={{ color: 'var(--primary)' }}>《用户协议》</a>
          {' '}与{' '}
          <a href="#/terms" style={{ color: 'var(--primary)' }}>《隐私政策》</a>
        </div>
      </div>
    </div>
  )
}
