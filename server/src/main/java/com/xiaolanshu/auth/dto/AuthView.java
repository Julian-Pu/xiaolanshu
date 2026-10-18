package com.xiaolanshu.auth.dto;

import com.xiaolanshu.user.dto.UserView;

public record AuthView(String token, UserView user) {
}
