package com.xiaolanshu.comment.dto;

import com.xiaolanshu.user.dto.UserBrief;

import java.util.List;

/** 评论视图：顶层评论含子回复列表 */
public record CommentView(
        Long id,
        String content,
        int likeCount,
        String createdAt,
        UserBrief user,
        UserBrief replyTo,
        List<CommentView> replies
) {
}
