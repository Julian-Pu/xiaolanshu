import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { userApi } from '../api'
import type { PostCard, User } from '../models'
import { useAuth } from '../store/auth'
import Masonry from '../components/Masonry'
import FollowButton from '../components/FollowButton'

export default function Profile() {
  const { id } = useParams()
  const current = useAuth((s) => s.user)
  const setUser = useAuth((s) => s.setUser)
  const [profile, setProfile] = useState<User | null>(null)
  const [posts, setPosts] = useState<PostCard[]>([])
  const [editing, setEditing] = useState(false)
  const [nickname, setNickname] = useState('')
  const [bio, setBio] = useState('')
  const [avatar, setAvatar] = useState('')
  const [gender, setGender] = useState('secret')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    if (!id) return
    if (id === 'me') {
      if (!current) return
      setProfile(current)
      const r = await userApi.posts(current.id)
      setPosts(r.list)
      return
    }
    try {
      const p = await userApi.profile(Number(id))
      setProfile(p)
      const r = await userApi.posts(Number(id))
      setPosts(r.list)
    } catch {
      setProfile(null)
    }
  }, [id, current])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (profile) {
      setNickname(profile.nickname)
      setBio(profile.bio ?? '')
      setAvatar(profile.avatar ?? '')
      setGender(profile.gender)
    }
  }, [profile])

  async function save() {
    setBusy(true)
    try {
      const u = await userApi.updateMe({
        nickname: nickname.trim() || undefined,
        bio: bio.trim() || undefined,
        avatar: avatar || undefined,
        gender,
      })
      setProfile(u)
      setUser(u)
      setEditing(false)
    } catch {
      // 忽略
    } finally {
      setBusy(false)
    }
  }

  if (!profile) {
    return <div className="loading">用户不存在或已注销</div>
  }

  const isMe = id === 'me' || (current && current.id === profile.id)

  return (
    <div>
      <div className="profile-head">
        <img className="avatar" src={profile.avatar ?? ''} alt="" />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="profile-name">{profile.nickname}</div>
          <div className="text-2">{profile.bio || '这个人很懒，什么都没写～'}</div>
          <div className="profile-stats">
            <span><b>{profile.followingCount}</b>关注</span>
            <span><b>{profile.followerCount}</b>粉丝</span>
            <span><b>{profile.postCount}</b>笔记</span>
          </div>
        </div>
        {isMe ? (
          <div style={{ display: 'flex', gap: 8 }}>
            {!editing ? (
              <button className="btn" onClick={() => setEditing(true)}>编辑资料</button>
            ) : (
              <button className="btn btn-primary" onClick={save} disabled={busy}>保存</button>
            )}
          </div>
        ) : (
          <FollowButton userId={profile.id} isFollowing={profile.isFollowing} />
        )}
      </div>

      {editing && (
        <div className="side-card" style={{ marginBottom: 20 }}>
          <div className="form-item">
            <label>头像 URL</label>
            <input className="input" value={avatar} onChange={(e) => setAvatar(e.target.value)} placeholder="图片地址（可留空）" />
          </div>
          <div className="form-item">
            <label>昵称</label>
            <input className="input" value={nickname} onChange={(e) => setNickname(e.target.value)} maxLength={20} />
          </div>
          <div className="form-item">
            <label>简介</label>
            <textarea className="input" value={bio} onChange={(e) => setBio(e.target.value)} maxLength={200} />
          </div>
          <div className="form-item">
            <label>性别</label>
            <select className="input" value={gender} onChange={(e) => setGender(e.target.value)}>
              <option value="secret">保密</option>
              <option value="male">男</option>
              <option value="female">女</option>
            </select>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn" onClick={() => setEditing(false)}>取消</button>
            <button className="btn btn-primary" onClick={save} disabled={busy}>保存修改</button>
          </div>
        </div>
      )}

      <h3 className="side-title">TA 的笔记</h3>
      <Masonry posts={posts} />
    </div>
  )
}
