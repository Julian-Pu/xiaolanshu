import { useEffect, useState } from 'react'
import { userApi } from '../api'
import type { PostCard } from '../models'
import Masonry from '../components/Masonry'

/** 浏览记录：最近看过的笔记（推荐流已读排斥的数据来源） */
export default function History() {
  const [posts, setPosts] = useState<PostCard[]>([])
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    ;(async () => {
      try {
        const r = await userApi.myHistory()
        setPosts(r.list)
      } catch {
        // 忽略
      }
      setLoaded(true)
    })()
  }, [])

  if (!loaded) {
    return <div className="loading">加载中…</div>
  }

  return (
    <div>
      <h2 className="side-title">浏览记录</h2>
      {posts.length === 0 ? (
        <div className="loading">还没有浏览记录，去看看首页吧</div>
      ) : (
        <Masonry posts={posts} />
      )}
    </div>
  )
}
