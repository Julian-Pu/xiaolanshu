import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { userApi } from '../api'
import { useAuth } from '../store/auth'

interface Props {
  userId: number
  isFollowing: boolean
  onChange?: (following: boolean) => void
  onFollowed?: () => void
}

export default function FollowButton({ userId, isFollowing, onChange, onFollowed }: Props) {
  const token = useAuth((s) => s.token)
  const user = useAuth((s) => s.user)
  const navigate = useNavigate()
  const [following, setFollowing] = useState(isFollowing)
  const [busy, setBusy] = useState(false)

  if (user?.id === userId) {
    return <button className="btn" disabled>我</button>
  }

  async function toggle() {
    if (!token) {
      navigate('/login')
      return
    }
    setBusy(true)
    try {
      if (following) {
        await userApi.unfollow(userId)
        setFollowing(false)
        onChange?.(false)
      } else {
        await userApi.follow(userId)
        setFollowing(true)
        onChange?.(true)
        onFollowed?.()
      }
    } catch {
      // 忽略
    } finally {
      setBusy(false)
    }
  }

  return (
    <button className={`btn follow-btn${following ? '' : ' btn-primary'}`} onClick={toggle} disabled={busy}>
      {following ? '已关注' : '关注'}
    </button>
  )
}
