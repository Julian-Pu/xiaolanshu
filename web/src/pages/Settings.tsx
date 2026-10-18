import { useState } from 'react'
import { userApi } from '../api'

/** 设置页：修改密码 / 清缓存 / 关于 */
export default function Settings() {
  const [oldPwd, setOldPwd] = useState('')
  const [newPwd, setNewPwd] = useState('')
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const [cacheMsg, setCacheMsg] = useState('')

  async function changePassword() {
    setMsg('')
    setErr('')
    if (!oldPwd.trim() || newPwd.length < 6) {
      setErr('请输入原密码，且新密码至少 6 位')
      return
    }
    setBusy(true)
    try {
      await userApi.changePassword({ oldPassword: oldPwd, newPassword: newPwd })
      setMsg('密码修改成功')
      setOldPwd('')
      setNewPwd('')
    } catch (e) {
      setErr((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  function clearCache() {
    try {
      localStorage.removeItem('xls-search-history')
      localStorage.removeItem('xls-publish-draft')
    } catch {
      /* ignore */
    }
    setCacheMsg('已清除搜索历史与发布草稿')
  }

  return (
    <div className="publish-form">
      <h2 style={{ marginTop: 0 }}>设置</h2>

      <div className="form-item">
        <h3 style={{ margin: '0 0 12px' }}>修改密码</h3>
        <div className="form-item">
          <label>原密码</label>
          <input
            className="input"
            type="password"
            value={oldPwd}
            onChange={(e) => setOldPwd(e.target.value)}
          />
        </div>
        <div className="form-item">
          <label>新密码（至少 6 位）</label>
          <input
            className="input"
            type="password"
            value={newPwd}
            onChange={(e) => setNewPwd(e.target.value)}
          />
        </div>
        {msg && <div style={{ color: 'var(--brand)', marginBottom: 10 }}>{msg}</div>}
        {err && <div style={{ color: 'var(--danger)', marginBottom: 10 }}>{err}</div>}
        <button className="btn btn-primary" onClick={changePassword} disabled={busy}>
          {busy ? '提交中…' : '确认修改'}
        </button>
      </div>

      <div className="form-item" style={{ marginTop: 24 }}>
        <h3 style={{ margin: '0 0 12px' }}>存储</h3>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span className="text-2">清除搜索历史、发布草稿</span>
          <button className="btn" onClick={clearCache}>
            清缓存
          </button>
        </div>
        {cacheMsg && <div style={{ color: 'var(--brand)', marginTop: 8 }}>{cacheMsg}</div>}
      </div>

      <div className="form-item" style={{ marginTop: 24 }}>
        <h3 style={{ margin: '0 0 12px' }}>关于</h3>
        <div className="text-2">
          小蓝书 · 对标小红书的全栈演示应用
          <br />
          <span className="text-3">演示账号：13800000001~5 / 123456</span>
        </div>
      </div>
    </div>
  )
}
