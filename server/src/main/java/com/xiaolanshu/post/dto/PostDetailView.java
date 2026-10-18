package com.xiaolanshu.post.dto;

import com.xiaolanshu.topic.dto.TopicBrief;
import com.xiaolanshu.user.dto.UserBrief;

import java.util.List;

/** 笔记详情（含正文） */
public record PostDetailView(
        Long id,
        String title,
        String content,
        String cover,
        List<String> images,
        String videoUrl,
        String videoCover,
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
