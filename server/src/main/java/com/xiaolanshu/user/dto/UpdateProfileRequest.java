package com.xiaolanshu.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 修改资料：字段为 null 表示不修改 */
public record UpdateProfileRequest(
        @Size(max = 20, message = "昵称最长 20 字") String nickname,
        @Size(max = 200, message = "简介最长 200 字") String bio,
        @Size(max = 255) String avatar,
        @Pattern(regexp = "male|female|secret", message = "性别取值不合法") String gender
) {
}
