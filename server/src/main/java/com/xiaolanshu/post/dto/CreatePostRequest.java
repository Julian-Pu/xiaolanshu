package com.xiaolanshu.post.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/** 发布笔记 */
public record CreatePostRequest(
        @Size(max = 200, message = "标题最长 200 字") String title,
        @Size(max = 10000, message = "正文最长 10000 字") String content,
        List<@Size(max = 255) String> images,
        @Size(max = 512, message = "视频地址过长") String videoUrl,
        @Size(max = 255, message = "视频封面地址过长") String videoCover,
        Long topicId
) {
}
