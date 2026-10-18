package com.xiaolanshu.auth;

import com.xiaolanshu.auth.dto.AuthView;
import com.xiaolanshu.auth.dto.LoginRequest;
import com.xiaolanshu.auth.dto.RegisterRequest;
import com.xiaolanshu.auth.dto.SmsCodeRequest;
import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.user.dto.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SmsCodeService smsCodeService;

    public AuthController(AuthService authService, SmsCodeService smsCodeService) {
        this.authService = authService;
        this.smsCodeService = smsCodeService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthView> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    /** 发送短信验证码（开发模式：验证码打印到后端控制台，debug 时随响应返回） */
    @PostMapping("/sms-code")
    public ApiResponse<Map<String, String>> smsCode(@Valid @RequestBody SmsCodeRequest req) {
        String code = smsCodeService.send(req.phone());
        return ApiResponse.ok(Map.of("code", code == null ? "" : code));
    }

    @PostMapping("/login")
    public ApiResponse<AuthView> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me(HttpServletRequest request) {
        return ApiResponse.ok(authService.findUser(CurrentUser.require(request)));
    }
}
