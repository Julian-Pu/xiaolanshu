import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api'

/** 忘记密码：手机号 + 短信验证码（复用验证码通道）→ 重置密码 */
export default function Forgot() {
  const navigate = useNavigate()
  const [phone, setPhone] = useState('')
  const [smsCode, setSmsCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [smsHint, setSmsHint] = useState('')
  const [sendingSms, setSendingSms] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)

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
    if (newPassword.length < 6) {
      setError('新密码至少 6 位')
      return
    }
    setBusy(true)
    try {
      await authApi.resetPassword({ phone, smsCode: smsCode.trim(), newPassword })
      setDone(true)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <div className="login-wrap">
        <div className="login-card" style={{ textAlign: 'center' }}>
          <div className="login-logo">
            <span className="logo-mark">蓝</span>
            <div className="login-title">密码已重置</div>
            <div className="text-3" style={{ fontSize: 13 }}>请使用新密码重新登录</div>
          </div>
          <button className="btn btn-primary" style={{ width: '100%', padding: '10px' }} onClick={() => navigate('/login')}>
            去登录
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="login-logo">
          <span className="logo-mark">蓝</span>
          <div className="login-title">忘记密码</div>
          <div className="text-3" style={{ fontSize: 13 }}>通过短信验证码重置登录密码</div>
        </div>
        <form onSubmit={submit}>
          <div className="form-item">
            <label>手机号</label>
            <input className="input" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="11 位手机号" maxLength={11} />
          </div>
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
          <div className="form-item">
            <label>新密码</label>
            <input className="input" type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="至少 6 位" />
          </div>
          {error && <div style={{ color: 'var(--danger)', fontSize: 13, marginBottom: 12 }}>{error}</div>}
          <button className="btn btn-primary" style={{ width: '100%', padding: '10px' }} disabled={busy}>
            {busy ? '提交中…' : '重置密码'}
          </button>
        </form>
        <div style={{ textAlign: 'center', marginTop: 16, fontSize: 13 }}>
          <Link to="/login" style={{ color: 'var(--primary)' }}>返回登录</Link>
        </div>
      </div>
    </div>
  )
}
