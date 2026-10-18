package com.xiaolanshu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码至少 6 位")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 20, message = "昵称最长 20 字")
        String nickname,

        // 短信验证码：null 时由 AuthService 按 xiaolanshu.sms.enabled 决定是否校验
        @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
        String smsCode
) {
}
