package com.xiaolanshu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 忘记密码请求：{ phone, smsCode, newPassword }，验证码复用短信验证码通道 */
public record ResetPasswordRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
        String smsCode,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, message = "密码至少 6 位")
        String newPassword
) {
}
