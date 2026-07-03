package com.example.aiagent.config;

import com.example.aiagent.filter.AuthFilter;
import com.example.aiagent.service.AuthService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册鉴权过滤器，对 /api/ai 下接口进行登录校验
 */
@Configuration
public class AuthFilterConfig {

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthService authService) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthFilter(authService));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
