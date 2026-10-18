# 小蓝书（XiaoLanShu）

一款模仿小红书产品形态的内容种草社区 App，蓝色主题。本仓库包含 **Android 原生端（Kotlin + Jetpack Compose）**、**网页端（React）** 与 **后端服务（Java Spring Boot）** 的完整源码、架构设计与部署方案。

> 定位：内容社区 MVP 全栈工程 —— 信息流、笔记发布、互动（点赞/收藏/评论）、关注关系、搜索、通知、私信，多端共用一套 API。

---

## 一、技术栈总览

| 端 | 技术栈 | 说明 |
|---|---|---|
| 移动端 `android/` | Kotlin + Jetpack Compose（Material 3） | Android 原生 App，MVVM 架构，单 Activity + Navigation |
| 网页端 `web/` | React 18 + Vite + TypeScript + Zustand + React Router | 小红书网页版式布局，蓝色主题 |
| 后端 `server/` | Java 17 + Spring Boot 4 + JDBC（JdbcTemplate） | 模块化单体，REST API + JWT 鉴权 + 限流 |
| 数据库 | H2（开发，MODE=MySQL 兼容）/ MySQL（生产） | 表结构见 `server/src/main/resources/schema.sql` |
| 对象存储 | 本地磁盘（开发，`./uploads`）/ OSS（生产） | 图片上传 |
| 缓存/中间件 | 内存限流（开发）/ Redis（生产，见 docs/05） | 见部署文档 |

## 二、目录结构

```
小蓝书/
├── docs/                     # 架构设计文档
│   ├── 01-总体架构.md
│   ├── 02-技术选型.md
│   ├── 03-数据库设计.md
│   ├── 04-API设计.md
│   ├── 05-部署架构.md
│   ├── 06-消息队列异步化设计.md
│   ├── 07-云端部署指南.md       # 云端部署完整操作手册
│   └── architecture.html     # 可视化架构图（浏览器打开）
├── server/                   # 后端服务（Java Spring Boot）
│   ├── pom.xml               # Maven 构建（含 Maven Wrapper，无需预装 Maven）
│   └── src/main/...          # 鉴权/用户/笔记/评论/信息流/互动/话题/搜索/通知/私信/上传
├── web/                      # 网页端（React + Vite）
└── android/                  # Android 原生端（Kotlin + Compose）
```

## 三、快速启动（开发环境）

> 前置要求：JDK 17+、Node.js 18+、Android Studio（构建 Android 端）。

### 1. 启动后端（默认端口 3000）

```bash
cd server
.\mvnw.cmd spring-boot:run    # Windows；macOS/Linux 用 ./mvnw
```

启动后：

- API 地址：`http://localhost:3000/api`
- 健康检查：`http://localhost:3000/api/health`
- 演示账号：`13800000001` ~ `13800000005`，密码均为 `123456`
- 首次启动自动执行 `schema.sql` 建表并播种演示数据（5 话题 / 5 用户 / 12 篇笔记，图片自动生成到 `uploads/seed/`）
- 默认使用 H2 文件库（`data/xiaolanshu`），零配置可跑；切 MySQL 见 `application.yml` 与 docs/05

> 后端检测到 `web/dist` 存在时，自动托管网页端静态文件，实现单端口访问。

### 2. 启动网页端（默认端口 5173）

```bash
cd web
npm install
npm run dev     # http://localhost:5173
```

> 网页端默认代理 `/api` 与 `/uploads` 到 `http://localhost:3000`（见 `vite.config.ts`）；也可 `npm run build` 后由后端直接托管。

### 3. Android 端

1. 用 **Android Studio**（Ladybug 或更新）打开 `android/` 目录，等待 Gradle Sync 完成。
2. 确保已安装 Android SDK Platform 35 / Build Tools。
3. 后端地址默认已配置为**本地电脑做服务器**（真机与电脑连同一 WiFi，直连 `http://电脑局域网IP:3000/`）；模拟器用 `-PapiBaseUrl=http://10.0.2.2:3000/` 覆盖，云端用 `-PapiBaseUrl=https://域名/`（详见 `android/README.md`）。
4. 点击 Run 安装到模拟器/真机。

> 本机未预装 Android SDK，Android 端源码在 Android Studio 中打开即可构建运行；命令行构建需先安装 JDK + Android SDK。
> 本地电脑做服务器的防火墙需放行 3000 端口；云端部署完整步骤见 `docs/07-云端部署指南.md`。

## 四、已实现功能清单

- [x] 注册 / 登录 / JWT 鉴权 / 用户信息修改（注册含短信验证码：Redis 存储 + 后端控制台打印，开发模式）
- [x] 信息流：推荐（时间衰减热度）+ 关注（时间排序）+ 话题频道，分页；Android 支持下拉刷新、上拉自动加载、骨架屏
- [x] 笔记发布 / 删除 / 详情（多图）+ 详情页相关推荐 + 作者信息卡（关注）+ 点赞/收藏动效
- [x] 点赞 / 收藏 / 评论（含楼中楼回复）；个人主页「笔记 / 收藏 / 赞过」三 tab
- [x] 关注 / 粉丝 / 个人主页（App 含关注/粉丝列表页）
- [x] 话题列表、按话题聚合（App 详情页话题标签直达话题流）
- [x] 搜索（笔记 + 用户）+ 热搜榜 + 本地搜索历史（App）
- [x] 通知中心（赞 / 收藏 / 评论 / 关注 分类 tab + 未读角标 + 全部已读）
- [x] 私信（会话 + 消息；REST + WebSocket 实时推送，断线由 REST 兜底）
- [x] 深色模式（Android 跟随系统/手动三档，Web 手动切换，持久化记忆）
- [x] 发布草稿箱（Android/Web 本地自动保存、恢复、一键清空）
- [x] 设置页（主题切换 / 修改密码 / 清缓存 / 关于版本，Android + Web）
- [x] Android MVVM 分层（Repository 统一数据源 + 全页面 ViewModel，StateFlow 状态管理，UI 层零直连）
- [x] 推荐个性化 v1（兴趣标签加权：点赞/收藏/评论/发布学习兴趣，推荐流 Top5 标签加成）
- [x] 图片上传（本地磁盘存储）
- [x] 视频笔记（发布选视频 + 可选封面；信息流卡片 ▶ 角标；详情页 App 用 Media3 ExoPlayer / Web 用 HTML5 播放）
- [x] 笔记编辑（作者编辑标题/内容/话题，图片视频保留原样；App/Web 编辑入口）
- [x] 删除确认（Android 删除笔记弹确认框；Web 已有 confirm）
- [x] 收藏专辑（创建/删除专辑，把收藏归入专辑，按专辑筛选；App 个人主页收藏 tab + Web 专辑页）
- [x] 分享（Android 系统分享面板，Web 复制链接）
- [x] 浏览记录（我的浏览记录页；推荐流自动排斥已读笔记）
- [x] 推荐个性化 v2（协同过滤：共同互动用户的相似笔记加权；冷启动回落热度+兴趣）
- [x] 笔记 Redis 热点缓存（未登录详情短 TTL 缓存 + 写操作失效）+ 计数对账任务（定时重算 + 管理接口）
- [x] 私信已读回执 / 正在输入（WebSocket typing/read 帧 + 会话未读角标 + 消息已读标记）
- [x] Web 深色模式细节（输入框/编辑器适配、内容图压暗、滚动条、登录页）
- [x] 图片处理管线（上传自动生成缩略图 + 半透明水印；卡片用缩略图、详情用原图）
- [x] 搜索拼音纠错（标题/昵称支持全拼与首字母检索，中文搜索不受影响）
- [x] 内容安全审核（文本机审敏感词拦截发布/编辑/评论 + 审核日志管理接口）
- [x] 存储抽象层（StorageService 接口 + 本地实现，上传统一走接口，生产可替换对象存储）
- [x] 推荐离线物料池（预计算候选落表 + 定时/手动重建 + feed 在线直读、兜底实时）
- [x] 多端实时通知推送（点赞/收藏/评论/关注经 WS 推 notify 帧 → App 通知栏 + Web 角标）
- [x] 技术债修复：feed Redis 热点榜预热 + 推荐链路 SQL/Redis 分页（消除请求链路全量排序）
- [x] 技术债修复：计数对账增强（对账后驱逐变更笔记热点缓存，降低 stale 计数）
- [x] 技术债修复：Android 跨页状态共享（AppState 全局 StateFlow，消息角标跨页即时同步）
- [x] 安全加固：JWT 密钥 env 化 + 启动校验、管理接口 X-Admin-Key 保护、验证码防爆破（5 次失败锁 5 分钟）、限流 env 化
- [x] 合规：Web 隐私政策 / 用户协议页（登录页链接）+ Android 登录同意提示
- [x] 分享深链：分享文案带 `xls://post/{id}`，App 接收即直达笔记详情（未登录先登录再跳）
- [x] 通知点击路由：通知列表/系统通知点击按类型跳转（赞/藏/评→笔记详情，关注→用户主页，Android + Web）
- [x] 发布体验升级：多图封面选择（第一张为封面可切换）+ Android 图片编辑（1:1/3:4/自由裁剪 + 6 种滤镜）+ Web Canvas 滤镜 + 9 张上限
- [x] 举报入口：笔记详情页举报（垃圾广告/色情低俗/违法违规/其他 + 补充说明），Android + Web，同一用户同一笔记去重
- [x] 管理后台 Web 页（/admin，X-Admin-Key 登录）：概览统计 + 举报审核（成立删笔记/忽略）+ 内容管理（笔记列表/强制删除）
- [x] 忘记密码：登录页入口 + 短信验证码重置密码（POST /api/auth/reset-password），Android + Web

## 五、文档索引

| 文档 | 内容 |
|---|---|
| [01-总体架构](docs/01-总体架构.md) | 三端前后端整体架构、分层、关键链路 |
| [02-技术选型](docs/02-技术选型.md) | 各端选型理由与对比 |
| [03-数据库设计](docs/03-数据库设计.md) | ER 概览、表结构、索引、一致性策略 |
| [04-API设计](docs/04-API设计.md) | 接口约定、完整接口清单、示例 |
| [05-部署架构](docs/05-部署架构.md) | 开发/生产拓扑、Docker、扩展演进 |
| [06-消息队列异步化设计](docs/06-消息队列异步化设计.md) | MQ 演进方案：何时引入、落点、Outbox、幂等重试 |
| [07-云端部署指南](docs/07-云端部署指南.md) | 云服务器部署完整手册：构建、MySQL、Nginx/systemd、Docker、HTTPS、运维备份 |
| [08-对标小红书差距分析](docs/08-对标小红书差距分析与改进方案.md) | 差距清单（架构/功能/UI）、已落地改进、演进路线与技术债 |

## 六、安全与合规说明

- 本项目为原创教学/工程演示代码，未使用任何小红书品牌素材与接口。
- 用户密码使用 BCrypt 哈希存储；接口具备统一错误处理与基础限流。
- 生产环境需补充：HTTPS、更严格限流、内容审核（图片/文本）、隐私政策与用户协议、未成年人保护等（见 docs/05）。
