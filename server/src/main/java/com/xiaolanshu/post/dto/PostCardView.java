package com.xiaolanshu.post.dto;

import com.xiaolanshu.topic.dto.TopicBrief;
import com.xiaolanshu.user.dto.UserBrief;

import java.util.List;

/** 信息流/列表中的笔记卡片 */
public record PostCardView(
        Long id,
        String title,
        String cover,
        String coverThumb,
        List<String> images,
        String videoUrl,
        int likeCount,
        int collectCount,
        int commentCount,
        int viewCount,
        String createdAt,
        UserBrief author,
        TopicBrief topic,
        boolean isLiked,
        boolean isCollected
) {
}
