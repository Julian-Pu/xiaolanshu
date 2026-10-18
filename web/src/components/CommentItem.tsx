import type { CommentItem } from '../models'
import { timeAgo } from '../store/auth'

export default function CommentItem({ comment }: { comment: CommentItem }) {
  return (
    <div className="comment-item">
      <img className="avatar" src={comment.user.avatar ?? ''} alt="" />
      <div className="comment-content">
        <div>
          <span className="comment-name">{comment.user.nickname}</span>
          {comment.replyTo && (
            <span className="text-3">
              {' '}
              回复 <span style={{ color: 'var(--brand)' }}>@{comment.replyTo.nickname}</span>
            </span>
          )}
          <span className="text-3" style={{ marginLeft: 8, fontSize: 12 }}>
            {timeAgo(comment.createdAt)}
          </span>
        </div>
        <div className="comment-text">{comment.content}</div>
        {comment.replies.length > 0 && (
          <div className="comment-replies">
            {comment.replies.map((r) => (
              <CommentItem key={r.id} comment={r} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
