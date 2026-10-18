package com.xiaolanshu.config;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 演示数据播种：仅当 users 表为空时执行（生成 SVG 图、5 个用户、12 篇笔记及互动） */
@Component
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final File seedDir;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public SeedDataRunner(JdbcTemplate jdbc, ObjectMapper objectMapper,
                          @Value("${xiaolanshu.upload-dir}") String uploadDir) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.seedDir = new File(new File(uploadDir).getAbsoluteFile(), "seed");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        if (count != null && count > 0) {
            log.info("[seed] 已有数据，跳过演示数据播种");
            return;
        }
        log.info("[seed] 开始播种演示数据…");
        seedDir.mkdirs();
        writeSvg("a1.svg", "#5B8FF9", "#61DDAA", "蓝");
        writeSvg("a2.svg", "#F6BD16", "#F58B2F", "青");
        writeSvg("a3.svg", "#7262FD", "#9D7BFF", "哲");
        writeSvg("a4.svg", "#E8684A", "#F6C15B", "沐");
        writeSvg("a5.svg", "#269A99", "#78D3F8", "舟");

        String[] postLabels = {
                "宜宾三江口夜景", "露营攻略", "宝藏咖啡店", "番茄意面",
                "桌面好物", "耳机横评", "春日穿搭", "大衣挑选",
                "老城漫步", "徒步路线", "手冲咖啡", "极简书桌"
        };
        for (int i = 1; i <= postLabels.length; i++) {
            writeSvg("p" + i + ".svg", gradient(i, true), gradient(i, false), postLabels[i - 1]);
            if (i % 2 == 0) {
                writeSvg("p" + i + "-2.svg", gradient(i, false), gradient(i + 3, true), postLabels[i - 1] + "·细节");
            }
        }

        String now = Instant.now().toString();
        String hash = encoder.encode("123456");

        // topics
        String[][] topics = {
                {"旅行日记", "/uploads/seed/p1.svg"},
                {"美食探店", "/uploads/seed/p3.svg"},
                {"数码好物", "/uploads/seed/p5.svg"},
                {"穿搭分享", "/uploads/seed/p7.svg"},
                {"周末去哪儿", "/uploads/seed/p9.svg"},
        };
        for (String[] t : topics) {
            jdbc.update("INSERT INTO topics (name, cover, post_count) VALUES (?, ?, 0)", t[0], t[1]);
        }

        // users（演示账号：1380000000X / 123456）
        String[][] users = {
                {"13800000001", "小蓝", "female", "蓝色星球记录者", "/uploads/seed/a1.svg"},
                {"13800000002", "青青", "female", "城市漫游与美食", "/uploads/seed/a2.svg"},
                {"13800000003", "阿哲", "male", "数码爱好者", "/uploads/seed/a3.svg"},
                {"13800000004", "沐沐", "female", "穿搭灵感集", "/uploads/seed/a4.svg"},
                {"13800000005", "一舟", "male", "周末永远在路上", "/uploads/seed/a5.svg"},
        };
        for (String[] u : users) {
            jdbc.update("""
                    INSERT INTO users (phone, password_hash, nickname, avatar, bio, gender,
                                       following_count, follower_count, post_count, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, ?, ?)
                    """, u[0], hash, u[1], u[4], u[3], u[2], now, now);
        }

        // follows：1 关注 2/3/4/5，2 关注 1，3 关注 1/4，4 关注 5，5 关注 2/3
        int[][] follows = {{1, 2}, {1, 3}, {1, 4}, {1, 5}, {2, 1}, {3, 1}, {3, 4}, {4, 5}, {5, 2}, {5, 3}};
        for (int[] f : follows) {
            jdbc.update("INSERT INTO follows (user_id, follow_user_id, created_at) VALUES (?, ?, ?)", f[0], f[1], now);
        }

        // posts: [authorId, topicId, title, content]
        Object[][] posts = {
                {1, 1, "宜宾三江口夜景", "晚饭后沿着江边散步，华灯初上的三江口真的很好看。手机直出，无需滤镜。"},
                {2, 5, "周末露营攻略：新手装备清单", "第一次露营别乱买！帐篷、防潮垫、头灯、保温杯四件套先备齐，周末就出发。"},
                {3, 2, "老街里的宝藏咖啡店", "藏在巷子尽头的手冲小店，老板会耐心介绍豆子风味，人均 25 就能坐一下午。"},
                {4, 2, "一人食的快乐：自制番茄意面", "15 分钟搞定的一人份意面，番茄熬出沙，加一点黄油，比外卖好吃太多。"},
                {5, 3, "千元内桌面好物分享", "台灯、机械键盘、显示器支架……预算一千以内，把桌面布置得舒服又好看。"},
                {1, 3, "通勤耳机横评：三款真无线对比", "从佩戴舒适度、降噪、续航三个维度横评了三款主流真无线耳机。"},
                {2, 4, "小个子春日穿搭公式", "上短下长 + 同色系，视觉显高 5cm，附三套春日通勤搭配。"},
                {3, 4, "秋冬大衣挑选指南", "版型、面料、长度怎么选？一件大衣穿三年的挑选思路都在这里。"},
                {4, 5, "城市漫步：两小时打卡老城", "不用刻意做攻略，沿着老街慢慢走，两小时也能收获一座城的味道。"},
                {5, 5, "城郊徒步路线推荐", "距离市区 40 分钟车程的轻徒步路线，风景好、难度低，适合新手。"},
                {1, 2, "在家做手冲咖啡的快乐", "入手了一套入门手冲装备，每天早上一杯，仪式感拉满。"},
                {2, 3, "我的极简书桌布置", "只保留必须的物品，桌面清爽了，工作心情也好了。"},
        };
        long[] postIds = new long[posts.length];
        for (int i = 0; i < posts.length; i++) {
            Object[] p = posts[i];
            String images = p[1] instanceof Number && ((Number) p[1]).intValue() % 2 == 0
                    ? "[\"/uploads/seed/p" + (i + 1) + ".svg\",\"/uploads/seed/p" + (i + 1) + "-2.svg\"]"
                    : "[\"/uploads/seed/p" + (i + 1) + ".svg\"]";
            jdbc.update("""
                    INSERT INTO posts (user_id, title, content, images, topic_id, status,
                                       like_count, collect_count, comment_count, view_count, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, 'published', 0, 0, 0, ?, ?, ?)
                    """, ((Number) p[0]).longValue(), p[2], p[3], images, ((Number) p[1]).longValue(),
                    60 + i * 37, now, now);
            postIds[i] = jdbc.queryForObject(
                    "SELECT id FROM posts WHERE user_id = ? AND created_at = ? AND title = ?",
                    Long.class, ((Number) p[0]).longValue(), now, p[2]);
        }

        // likes：每篇 2-5 个（作者本人除外）
        int li = 1;
        for (long pid : postIds) {
            int n = 2 + (int) (pid % 4);
            for (int k = 0; k < n; k++) {
                long u = 1 + ((li + k) % 5);
                jdbc.update("INSERT INTO likes (user_id, post_id, created_at) VALUES (?, ?, ?)", u, pid, now);
            }
            li++;
        }
        // collects：每篇 1-3 个
        int ci = 2;
        for (long pid : postIds) {
            int n = 1 + (int) (pid % 3);
            for (int k = 0; k < n; k++) {
                long u = 1 + ((ci + k) % 5);
                jdbc.update("INSERT INTO collects (user_id, post_id, created_at) VALUES (?, ?, ?)", u, pid, now);
            }
            ci++;
        }
        // comments：部分笔记有评论，其中一条含回复
        String[][] comments = {
                {"1", "3", "这个角度拍得真好看！", null},
                {"1", "4", "收藏了，下次去吃", null},
                {"2", "1", "露营清单很实用", null},
                {"3", "1", "求店名！", null},
                {"3", "5", "同求，周末就想去", "3"},
                {"5", "2", "键盘是哪一款？", null},
                {"7", "4", "学到了，小个子福音", null},
                {"9", "5", "路线能发个具体位置吗", null},
                {"10", "1", "上周刚去，风景确实好", null},
        };
        for (String[] c : comments) {
            jdbc.update("""
                    INSERT INTO comments (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at)
                    VALUES (?, ?, ?, ?, ?, 0, ?)
                    """, Long.parseLong(c[0]), Long.parseLong(c[1]),
                    c[3] == null ? null : 4L, c[3] == null ? null : 1L, c[2], now);
        }

        // 私信会话：用户 1 与 2、3 各一个会话（含消息）
        Object[][] convs = {
                {1, 2, "这周末去露营吗？", 2},
                {1, 3, "那家咖啡店叫什么名字呀", 3},
        };
        for (Object[] c : convs) {
            jdbc.update("""
                    INSERT INTO conversations (user_a, user_b, last_message, updated_at)
                    VALUES (?, ?, ?, ?)
                    """, c[0], c[1], c[2], now);
            long convId = jdbc.queryForObject(
                    "SELECT id FROM conversations WHERE user_a = ? AND user_b = ?", Long.class, c[0], c[1]);
            long sender = ((Number) c[3]).longValue();
            jdbc.update("""
                    INSERT INTO messages (conversation_id, sender_id, content, type, is_read, created_at)
                    VALUES (?, ?, ?, 'text', 1, ?)
                    """, convId, sender, c[2], now);
            jdbc.update("""
                    INSERT INTO messages (conversation_id, sender_id, content, type, is_read, created_at)
                    VALUES (?, ?, ?, 'text', 0, ?)
                    """, convId, 1, "好的，回头细聊～", now);
            jdbc.update("UPDATE conversations SET last_message = '好的，回头细聊～' WHERE id = ?", convId);
        }

        // demo 通知
        Object[][] notifs = {
                {1, 3, "like", 1, "赞了你的笔记"},
                {1, 4, "collect", 1, "收藏了你的笔记"},
                {1, 2, "comment", 1, "评论了你的笔记"},
                {1, 5, "follow", 2, "关注了你"},
        };
        for (Object[] n : notifs) {
            jdbc.update("""
                    INSERT INTO notifications (user_id, actor_id, type, ref_id, content, is_read, created_at)
                    VALUES (?, ?, ?, ?, ?, 0, ?)
                    """, ((Number) n[0]).longValue(), ((Number) n[1]).longValue(), n[2], n[3], n[4], now);
        }

        // 重算冗余计数，保证与明细一致
        jdbc.update("""
                UPDATE posts SET
                  like_count    = (SELECT COUNT(*) FROM likes    WHERE post_id = posts.id),
                  collect_count = (SELECT COUNT(*) FROM collects WHERE post_id = posts.id),
                  comment_count = (SELECT COUNT(*) FROM comments WHERE post_id = posts.id)
                """);
        jdbc.update("""
                UPDATE users SET
                  following_count = (SELECT COUNT(*) FROM follows WHERE user_id = users.id),
                  follower_count  = (SELECT COUNT(*) FROM follows WHERE follow_user_id = users.id),
                  post_count      = (SELECT COUNT(*) FROM posts WHERE user_id = users.id AND status = 'published')
                """);
        jdbc.update("""
                UPDATE topics SET post_count =
                  (SELECT COUNT(*) FROM posts WHERE topic_id = topics.id AND status = 'published')
                """);
        log.info("[seed] 播种完成：5 用户 / 12 笔记 / 话题 5。演示账号 13800000001~5，密码 123456");
    }

    private void writeSvg(String name, String from, String to, String label) throws Exception {
        boolean avatar = name.startsWith("a");
        String svg;
        if (avatar) {
            svg = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="200" height="200">
                      <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/>
                      </linearGradient></defs>
                      <rect width="200" height="200" rx="100" fill="url(#g)"/>
                      <text x="100" y="128" font-size="80" fill="#ffffff" text-anchor="middle"
                            font-family="'PingFang SC','Microsoft YaHei',sans-serif">%s</text>
                    </svg>
                    """.formatted(from, to, label);
        } else {
            svg = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="600" height="800">
                      <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0" stop-color="%s"/><stop offset="1" stop-color="%s"/>
                      </linearGradient></defs>
                      <rect width="600" height="800" fill="url(#g)"/>
                      <circle cx="300" cy="330" r="130" fill="rgba(255,255,255,0.18)"/>
                      <text x="300" y="530" font-size="44" fill="#ffffff" text-anchor="middle"
                            font-family="'PingFang SC','Microsoft YaHei',sans-serif">%s</text>
                    </svg>
                    """.formatted(from, to, label);
        }
        Files.writeString(new File(seedDir, name).toPath(), svg, StandardCharsets.UTF_8);
    }

    private String gradient(int seed, boolean start) {
        String[][] palettes = {
                {"#5B8FF9", "#61DDAA"}, {"#F6BD16", "#F58B2F"}, {"#7262FD", "#9D7BFF"},
                {"#E8684A", "#F6C15B"}, {"#269A99", "#78D3F8"}, {"#5AD8A6", "#5B8FF9"},
        };
        String[] c = palettes[seed % palettes.length];
        return start ? c[0] : c[1];
    }
}
