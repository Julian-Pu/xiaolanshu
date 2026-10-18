# 04 · API 设计

## 1. 通用约定

- Base URL：`/api`（开发：`http://localhost:3000/api`）
- 数据格式：JSON；请求头 `Content-Type: application/json`（上传除外）
- **统一响应**：

```json
{ "code": 0, "message": "ok", "data": { } }
```

- 错误时 `code ≠ 0`，`message` 为人类可读信息，HTTP 状态码同步（400/401/403/404/500）。
- **鉴权**：`Authorization: Bearer <accessToken>`；未登录访问需鉴权接口返回 401。
- **分页**：`?page=1&size=10`，返回 `{ list, page, size, total, hasMore }`。
- **时间**：ISO 8601 字符串。

## 2. 错误码（部分）

| code | 含义 |
|---|---|
| 0 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 / token 失效 |
| 403 | 无权限（如删除他人笔记） |
| 404 | 资源不存在 |
| 409 | 冲突（如重复关注、手机号已注册） |
| 500 | 服务端错误 |

## 3. 接口清单

### 3.1 鉴权 Auth

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | /api/auth/sms-code | 发送短信验证码 `{phone}` → `{code}`（开发模式 code 为明文，便于联调） | - |
| POST | /api/auth/register | 注册 `{phone, password, nickname, smsCode}` → `{token, user}` | - |
| POST | /api/auth/login | 登录 `{phone, password}` → `{token, user}` | - |
| GET | /api/auth/me | 当前用户信息 | ✅ |

> 短信验证码：当前为开发模式（未接运营商短信通道）。验证码由后端生成（6 位数字），优先写入 Redis（key `sms:code:{phone}`，TTL 默认 5 分钟），并打印到后端控制台日志；Redis 不可用时自动降级进程内存并告警。`xiaolanshu.sms.debug=true` 时验证码随接口响应返回；生产置 `false` 并接入真实短信网关。注册校验通过后验证码即作废（一次性）。

### 3.2 用户 User

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/users/:id | 用户主页信息（含 is_following） | 可选 |
| PUT | /api/users/me | 修改资料 `{nickname, bio, avatar, gender}` | ✅ |
| POST | /api/users/:id/follow | 关注 | ✅ |
| DELETE | /api/users/:id/follow | 取消关注 | ✅ |
| GET | /api/users/:id/following | 关注列表（分页） | - |
| GET | /api/users/:id/followers | 粉丝列表（分页） | - |
| GET | /api/users/:id/posts | 该用户笔记（分页） | - |
| GET | /api/users/me/collects | 我收藏的笔记（分页，按收藏时间倒序） | ✅ |
| GET | /api/users/me/likes | 我赞过的笔记（分页，按点赞时间倒序） | ✅ |
| POST | /api/users/me/password | 修改密码 `{oldPassword, newPassword}`（新密码 6~32 位；旧密码错误返回 400「原密码错误」） | ✅ |

### 3.3 信息流 Feed

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/feed | `tab=recommend\|following\|topic&page&size`；`tab=topic` 需带 `topicId`；recommend 按时间衰减热度 `(like*3+collect*2+comment*5+view*0.2)/pow(小时+2,1.5)` 排序，**登录态叠加兴趣标签加权（个性化 v1：Top1 ×2.0 → Top5 ×1.2）** | 可选 |

### 3.4 笔记 Post

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/posts/:id | 详情（含作者、互动状态、浏览量+1） | 可选 |
| POST | /api/posts | 发布 `{title, content, images[], videoUrl?, videoCover?, topicId?}`（视频笔记：videoUrl 非空，封面优先 videoCover 否则 images[0]） | ✅ |
| PUT | /api/posts/:id | 编辑（仅作者；未传的 images/videoUrl/videoCover 保留原值，Web 只改文本时媒体不变） | ✅ |
| DELETE | /api/posts/:id | 删除（仅作者） | ✅ |
| POST | /api/posts/:id/like | 点赞 | ✅ |
| DELETE | /api/posts/:id/like | 取消点赞 | ✅ |
| POST | /api/posts/:id/collect | 收藏 | ✅ |
| DELETE | /api/posts/:id/collect | 取消收藏 | ✅ |
| GET | /api/posts/:id/comments | 评论列表（分页，含子回复） | - |
| POST | /api/posts/:id/comments | 发表评论 `{content, parentId?}` | ✅ |
| DELETE | /api/comments/:id | 删除评论（作者/笔记作者） | ✅ |
| GET | /api/posts/:id/related | 相关推荐（同话题→同作者→全站热度，排除自身，`limit` 默认 6、上限 20） | 可选 |

### 3.5 话题 Topic

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/topics | 话题列表（含 post_count） | - |
| GET | /api/topics/:id/posts | 话题下笔记（分页） | - |

### 3.6 搜索 Search

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/search | `q=&type=post\|user\|all&page&size`；标题/正文/昵称 LIKE + **拼音纠错**（全拼与首字母命中） | - |
| GET | /api/search/hot | 热搜词 Top 10（搜索时自动计数） | - |

### 3.7 通知 Notification

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/notifications | 通知列表（分页；`type=like\|collect\|comment\|follow` 可选分类） | ✅ |
| GET | /api/notifications/unread-count | 未读通知数（`type` 可选） | ✅ |
| POST | /api/notifications/read-all | 全部已读 | ✅ |

### 3.8 私信 Message

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/conversations | 会话列表（含 `unreadCount` 未读数） | ✅ |
| POST | /api/conversations | 发起/获取与某人会话 `{userId}` | ✅ |
| GET | /api/conversations/:id/messages | 消息列表（分页，拉取时自动标记对方消息已读） | ✅ |
| POST | /api/conversations/:id/messages | 发送消息 `{content}`（落库后经 WebSocket 实时推送给对端） | ✅ |
| POST | /api/conversations/:id/read | 标记会话中对方消息已读 → `{changed}`（WS read 帧的 REST 兜底） | ✅ |

> **WebSocket（私信实时推送）**：端点 `ws://<host>:3000/ws`（同端口、路径 `/ws`）。
> 协议为 JSON 文本帧：连接后先发 `{"type":"auth","token":"<jwt>"}`，服务端回 `{"type":"auth_ok","userId":N}`；
> 心跳 `{"type":"ping"}` → `{"type":"pong"}`；收到新私信时服务端推送
> `{"type":"message","conversationId":N,"message":{...}}`；发送 `{"type":"typing","conversationId":N}`
> 转发对端 `{"type":"typing","conversationId":N,"senderId":N}`；发送 `{"type":"read","conversationId":N}`
> 标记已读并回执对端 `{"type":"read_ack","conversationId":N,"senderId":N}`。断线后由 REST 接口拉取兜底。
> 注意：Vite 开发代理已配置 `'/ws': { target: 'ws://localhost:3000', ws: true }`；Android 端 URL 由 `BuildConfig.API_BASE_URL` 的 http→ws 拼 `/ws`。

### 3.10 管理接口（运维/演示）

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | /api/admin/reconcile-counts | 手动触发计数对账（重算 like/collect/comment 计数与话题笔记数）→ `{changedRows, evictedCount}`（对账后驱逐变更笔记热点缓存） | 无 |
| POST | /api/admin/rebuild-material | 手动重建推荐离线物料池（每用户 TopN 候选）→ `{users, candidates}` | 无 |
| POST | /api/admin/warm-hot-feed | 手动预热推荐热点榜（全局热度 TopN → Redis ZSET）→ `{warmed}` | 无 |
| GET | /api/admin/audit/logs | 内容安全审核日志（最近 50 条，含命中词/用户/片段，供人工复核） | 无 |

### 3.9 上传 Upload

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | /api/upload/image | multipart 单图 → `{url, thumbUrl}`（jpg/png/gif/webp/svg，最大 10MB；jpg/png 自动生成 ≤600px 缩略图 + 水印，thumbUrl 回落原图） | ✅ |
| POST | /api/upload/video | multipart 单视频 → `{url}`（mp4/webm/mov/3gp/m4v，最大 100MB，存 `uploads/videos/`） | ✅ |

### 3.11 收藏专辑 Album

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/albums | 我的专辑列表（含各专辑笔记数） | ✅ |
| POST | /api/albums | 创建专辑 `{name}`（1-50 字） | ✅ |
| DELETE | /api/albums/:id | 删除专辑（仅本人，级联清空关联） | ✅ |
| GET | /api/albums/:id/posts | 专辑内笔记（分页） | ✅ |
| POST | /api/albums/:id/posts | 把笔记加入专辑 `{postId}`（幂等） | ✅ |
| DELETE | /api/albums/:id/posts/:postId | 从专辑移除笔记 | ✅ |

### 3.12 浏览历史

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/users/me/history | 我的浏览记录（分页，按最近浏览倒序；查看详情时自动记录） | ✅ |

> 推荐流（`GET /api/feed?tab=recommend`）对登录用户自动**排斥已读**：看过（记录在 `read_posts`）的笔记不再出现在推荐列表。

### 3.10 其他

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /api/health | 健康检查 | - |

## 4. 示例

### 发送短信验证码

```http
POST /api/auth/sms-code
Content-Type: application/json

{ "phone": "13900000001" }
```

```json
{
  "code": 0,
  "message": "ok",
  "data": { "code": "483920" }
}
```

> 开发模式（`xiaolanshu.sms.debug=true`）下 `data.code` 为明文验证码；生产模式为 `""`，验证码仅打印在后端控制台日志。

### 注册

```http
POST /api/auth/register
Content-Type: application/json

{ "phone": "13800000001", "password": "123456", "nickname": "小蓝", "smsCode": "483920" }
```

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOi...",
    "user": { "id": 1, "nickname": "小蓝", "avatar": "/uploads/seed/a1.svg" }
  }
}
```

### 发布笔记

```http
POST /api/posts
Authorization: Bearer <token>
Content-Type: application/json

{ "title": "宜宾三江口夜景", "content": "晚饭后散步……", "images": ["/uploads/seed/p1.svg"], "topicId": 1 }
```

### 信息流

```http
GET /api/feed?tab=recommend&page=1&size=10
```

```json
{
  "code": 0, "message": "ok",
  "data": {
    "list": [
      {
        "id": 3,
        "title": "周末露营攻略",
        "cover": "/uploads/seed/p3.svg",
        "images": ["/uploads/seed/p3.svg"],
        "likeCount": 32, "collectCount": 8, "commentCount": 5, "viewCount": 120,
        "createdAt": "2026-09-28T08:00:00Z",
        "topic": { "id": 1, "name": "周末去哪儿" },
        "author": { "id": 2, "nickname": "青青", "avatar": "/uploads/seed/a2.svg" },
        "isLiked": false, "isCollected": false
      }
    ],
    "page": 1, "size": 10, "total": 32, "hasMore": true
  }
}
```

## 5. 鉴权与刷新说明

- 登录/注册返回 `token`（Access Token，示例有效期 7 天便于开发，生产建议 2h + Refresh Token 轮换）。
- 前端 axios 拦截器：请求自动带 token；401 时跳转登录页。
- 生产建议：Access 短效 + Refresh 长效，Refresh 走 `/api/auth/refresh` 换新（本仓库以单 token 简化，接口设计已预留扩展位）。
