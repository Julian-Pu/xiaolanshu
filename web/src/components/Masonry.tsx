import type { PostCard } from '../models'
import FeedCard from './FeedCard'

export default function Masonry({ posts }: { posts: PostCard[] }) {
  if (posts.length === 0) {
    return <div className="empty">这里还空空如也，快去发布第一篇笔记吧～</div>
  }
  return (
    <div className="feed-masonry">
      {posts.map((p) => (
        <FeedCard key={p.id} post={p} />
      ))}
    </div>
  )
}
