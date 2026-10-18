-- 小蓝书数据库 Schema
-- 方言：MySQL 兼容（H2 以 MODE=MySQL 运行开发库；生产直接用于 MySQL 8）。
-- 时间统一存 VARCHAR(32) ISO-8601 UTC 字符串（跨库稳定，字典序即时间序）。

CREATE TABLE IF NOT EXISTS users (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone           VARCHAR(20)  NOT NULL UNIQUE,
  password_hash   VARCHAR(100) NOT NULL,
  nickname        VARCHAR(50)  NOT NULL,
  avatar          VARCHAR(255),
  bio             VARCHAR(255),
  gender          VARCHAR(10)  NOT NULL DEFAULT 'secret', -- male / female / secret
  following_count INT          NOT NULL DEFAULT 0,
  follower_count  INT          NOT NULL DEFAULT 0,
  post_count      INT          NOT NULL DEFAULT 0,
  created_at      VARCHAR(32)  NOT NULL,
  updated_at      VARCHAR(32)  NOT NULL
);

CREATE TABLE IF NOT EXISTS follows (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  follow_user_id BIGINT NOT NULL,
  created_at     VARCHAR(32) NOT NULL,
  UNIQUE (user_id, follow_user_id)
);
CREATE INDEX IF NOT EXISTS idx_follows_follow ON follows (follow_user_id);

CREATE TABLE IF NOT EXISTS topics (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(50) NOT NULL UNIQUE,
  cover      VARCHAR(255),
  post_count INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS posts (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  title         VARCHAR(200),
  content       TEXT,
  images        TEXT,                          -- JSON 数组字符串
  video_url     VARCHAR(512),                  -- 视频笔记：视频地址（可空）
  video_cover   VARCHAR(255),                  -- 视频笔记：封面图地址（可空）
  topic_id      BIGINT,
  status        VARCHAR(20) NOT NULL DEFAULT 'published', -- draft / published / deleted
  like_count    INT NOT NULL DEFAULT 0,
  collect_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  view_count    INT NOT NULL DEFAULT 0,
  created_at    VARCHAR(32) NOT NULL,
  updated_at    VARCHAR(32) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_posts_created ON posts (created_at);
CREATE INDEX IF NOT EXISTS idx_posts_user   ON posts (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_posts_topic  ON posts (topic_id);

CREATE TABLE IF NOT EXISTS comments (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id          BIGINT NOT NULL,
  user_id          BIGINT NOT NULL,
  parent_id        BIGINT,
  reply_to_user_id BIGINT,
  content          TEXT    NOT NULL,
  like_count       INT NOT NULL DEFAULT 0,
  created_at       VARCHAR(32) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_comments_post ON comments (post_id, created_at);

CREATE TABLE IF NOT EXISTS likes (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  post_id    BIGINT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  UNIQUE (user_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_likes_post ON likes (post_id);

CREATE TABLE IF NOT EXISTS collects (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  post_id    BIGINT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  UNIQUE (user_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_collects_post ON collects (post_id);

CREATE TABLE IF NOT EXISTS notifications (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,                 -- 接收者
  actor_id   BIGINT,                          -- 触发者（可空：系统通知）
  type       VARCHAR(20) NOT NULL,            -- like / collect / comment / follow / system
  ref_id     BIGINT,
  content    VARCHAR(255) NOT NULL,
  is_read    INT NOT NULL DEFAULT 0,
  created_at VARCHAR(32) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications (user_id, is_read);

CREATE TABLE IF NOT EXISTS hot_words (
  word       VARCHAR(50) PRIMARY KEY,
  cnt        INT NOT NULL DEFAULT 1,
  updated_at VARCHAR(32) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_hot_words_cnt ON hot_words (cnt DESC);

CREATE TABLE IF NOT EXISTS conversations (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_a       BIGINT NOT NULL,
  user_b       BIGINT NOT NULL,
  last_message VARCHAR(500),
  updated_at   VARCHAR(32) NOT NULL,
  UNIQUE (user_a, user_b)
);

CREATE TABLE IF NOT EXISTS messages (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_id       BIGINT NOT NULL,
  content         TEXT NOT NULL,
  type            VARCHAR(20) NOT NULL DEFAULT 'text',
  is_read         INT NOT NULL DEFAULT 0,
  created_at      VARCHAR(32) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_msg_conv ON messages (conversation_id, created_at);

CREATE TABLE IF NOT EXISTS interests (
  user_id BIGINT NOT NULL,
  tag     VARCHAR(50) NOT NULL,
  score   INT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id, tag)
);
CREATE INDEX IF NOT EXISTS idx_interests_user ON interests (user_id, score DESC);

-- 收藏专辑：用户自建收藏夹（收藏笔记可按专辑归类）
CREATE TABLE IF NOT EXISTS albums (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  name       VARCHAR(50) NOT NULL,
  created_at VARCHAR(32) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_albums_user ON albums (user_id);

CREATE TABLE IF NOT EXISTS album_posts (
  album_id   BIGINT NOT NULL,
  post_id    BIGINT NOT NULL,
  created_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (album_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_album_posts_post ON album_posts (post_id);

-- 浏览历史：用户看过哪些笔记（用于"我的浏览记录" + 推荐流排斥已读）
CREATE TABLE IF NOT EXISTS read_posts (
  user_id   BIGINT NOT NULL,
  post_id   BIGINT NOT NULL,
  viewed_at VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_read_posts_user ON read_posts (user_id, viewed_at DESC);

-- 内容安全：文本机审拦截日志（命中敏感词时记录，供人工复核）
CREATE TABLE IF NOT EXISTS audit_logs (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT NOT NULL,
  action        VARCHAR(16) NOT NULL,          -- publish | edit | comment
  hit_word      VARCHAR(64) NOT NULL,
  content_snippet VARCHAR(200),
  created_at    VARCHAR(32) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs (created_at DESC);

-- 推荐离线物料池：预计算的每个用户 TopN 候选（召回/排序产物落表，feed 在线直读；
-- 生产由 Spark/Flink 离线任务生成，演示用 Spring @Scheduled 定时重建）
CREATE TABLE IF NOT EXISTS recommend_candidates (
  user_id   BIGINT NOT NULL,
  post_id   BIGINT NOT NULL,
  score     DOUBLE NOT NULL,
  reason    VARCHAR(16) NOT NULL,          -- hot | interest | cf
  built_at  VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_rc_user_score ON recommend_candidates (user_id, score DESC);
