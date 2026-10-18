package com.xiaolanshu.config;

import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 显式注册 ObjectMapper（Jackson 3 / tools.jackson），供 JSON 读写与接口序列化使用 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
