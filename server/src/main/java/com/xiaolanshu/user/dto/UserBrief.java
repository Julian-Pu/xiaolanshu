package com.xiaolanshu.user.dto;

/** 用户简要信息（评论/卡片作者等） */
public record UserBrief(
        Long id,
        String nickname,
        String avatar,
        boolean isFollowing
) {
}
