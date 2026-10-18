package com.xiaolanshu.user.dto;

/** 公开用户视图 */
public record UserView(
        Long id,
        String nickname,
        String avatar,
        String bio,
        String gender,
        int followingCount,
        int followerCount,
        int postCount,
        String createdAt,
        boolean isFollowing
) {
}
