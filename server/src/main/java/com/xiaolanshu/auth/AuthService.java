package com.xiaolanshu.auth;

import com.xiaolanshu.auth.dto.AuthView;
import com.xiaolanshu.auth.dto.LoginRequest;
import com.xiaolanshu.auth.dto.RegisterRequest;
import com.xiaolanshu.auth.dto.ResetPasswordRequest;
import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.security.JwtUtil;
import com.xiaolanshu.user.dto.UserView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final JdbcTemplate jdbc;
    private final JwtUtil jwtUtil;
    private final SmsCodeService smsCodeService;
    private final boolean smsEnabled;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(JdbcTemplate jdbc, JwtUtil jwtUtil, SmsCodeService smsCodeService,
                       @Value("${xiaolanshu.sms.enabled:true}") boolean smsEnabled) {
        this.jdbc = jdbc;
        this.jwtUtil = jwtUtil;
        this.smsCodeService = smsCodeService;
        this.smsEnabled = smsEnabled;
    }

    public AuthView register(RegisterRequest req) {
        if (smsEnabled) {
            if (req.smsCode() == null || req.smsCode().isBlank()) {
                throw ApiException.badRequest("请先获取短信验证码");
            }
            if (!smsCodeService.verify(req.phone(), req.smsCode())) {
                throw ApiException.badRequest("验证码错误或已过期");
            }
        }
        Long exists = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE phone = ?", Long.class, req.phone());
        if (exists != null && exists > 0) {
            throw ApiException.conflict("该手机号已注册");
        }
        String now = Instant.now().toString();
        jdbc.update("""
                INSERT INTO users (phone, password_hash, nickname, avatar, bio, gender,
                                   following_count, follower_count, post_count, created_at, updated_at)
                VALUES (?, ?, ?, NULL, NULL, 'secret', 0, 0, 0, ?, ?)
                """, req.phone(), encoder.encode(req.password()), req.nickname(), now, now);
        long id = jdbc.queryForObject("SELECT id FROM users WHERE phone = ?", Long.class, req.phone());
        return new AuthView(jwtUtil.sign(id, req.phone()), findUser(id));
    }

    public AuthView login(LoginRequest req) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users WHERE phone = ?", req.phone());
        if (rows.isEmpty() || !encoder.matches(req.password(), (String) rows.get(0).get("password_hash"))) {
            throw ApiException.badRequest("手机号或密码错误");
        }
        Map<String, Object> row = rows.get(0);
        long id = ((Number) row.get("id")).longValue();
        return new AuthView(jwtUtil.sign(id, req.phone()), toView(row, false));
    }

    /** 忘记密码：短信验证码校验通过后重置密码（验证码防爆破由 SmsCodeService 内建） */
    public void resetPassword(ResetPasswordRequest req) {
        if (!smsCodeService.verify(req.phone(), req.smsCode())) {
            throw ApiException.badRequest("验证码错误或已过期");
        }
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id FROM users WHERE phone = ?", req.phone());
        if (rows.isEmpty()) {
            throw ApiException.notFound("该手机号未注册");
        }
        long id = ((Number) rows.get(0).get("id")).longValue();
        jdbc.update("UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?",
                encoder.encode(req.newPassword()), Instant.now().toString(), id);
    }

    public UserView findUser(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("用户不存在");
        }
        return toView(rows.get(0), false);
    }

    UserView toView(Map<String, Object> row, boolean isFollowing) {
        return new UserView(
                ((Number) row.get("id")).longValue(),
                (String) row.get("nickname"),
                (String) row.get("avatar"),
                (String) row.get("bio"),
                (String) row.get("gender"),
                ((Number) row.get("following_count")).intValue(),
                ((Number) row.get("follower_count")).intValue(),
                ((Number) row.get("post_count")).intValue(),
                (String) row.get("created_at"),
                isFollowing
        );
    }
}
