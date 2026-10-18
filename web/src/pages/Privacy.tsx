import { Link } from 'react-router-dom'

export default function Privacy() {
  return (
    <div className="legal-wrap">
      <div className="legal-card side-card">
        <h3 className="side-title">小蓝书隐私政策</h3>
        <p className="text-3">更新日期：2026-10-18 · 生效日期：2026-10-18</p>
        <p>本隐私政策说明小蓝书（以下称"我们"）如何收集、使用、存储和保护您的个人信息。</p>
        <h4>一、我们收集的信息</h4>
        <p>1. 注册信息：手机号、昵称、密码（经 BCrypt 加密存储，不可逆）。</p>
        <p>2. 内容信息：您发布的笔记（图文/视频）、评论、收藏、点赞、浏览历史。</p>
        <p>3. 设备与日志：IP、请求时间与路径（用于限流与安全审计）。</p>
        <h4>二、信息的使用</h4>
        <p>1. 提供账号注册登录、笔记发布与互动、私信与通知等功能。</p>
        <p>2. 用于内容安全审核（敏感内容拦截）与推荐排序。</p>
        <p>3. 短信验证码仅用于验证您的手机号归属，不向第三方提供。</p>
        <h4>三、信息的存储与保护</h4>
        <p>1. 密码采用 BCrypt 单向加密；验证码一次性使用并设有效期。</p>
        <p>2. 管理接口使用独立密钥保护，日志不含明文密码。</p>
        <h4>四、您的权利</h4>
        <p>您可以在「我的 - 设置」中修改密码、清除本地缓存；注销账号请联系平台处理。</p>
        <h4>五、联系我们</h4>
        <p>如对本政策有疑问，请通过站内私信联系客服。</p>
        <div style={{ marginTop: 16 }}>
          <Link className="btn" to="/login">返回</Link>
        </div>
      </div>
    </div>
  )
}
