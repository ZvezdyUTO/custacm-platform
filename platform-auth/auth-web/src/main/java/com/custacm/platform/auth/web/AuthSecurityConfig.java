package com.custacm.platform.auth.web;

import com.custacm.platform.auth.core.PlatformSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class AuthSecurityConfig {
    @Bean
    @Order(PlatformSecurityConfig.PROTECTED_CHAIN_ORDER)
    public SecurityFilterChain authProtectedSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        return PlatformSecurityConfig.statelessJwt(http, jwtDecoder)
                .admin("/api/auth/admin/**")
                .player("/api/auth/player/**")
                .build();
    }

    @Bean
    @Order(PlatformSecurityConfig.GUEST_CHAIN_ORDER)
    public SecurityFilterChain authGuestSecurityFilterChain(HttpSecurity http) throws Exception {
        return PlatformSecurityConfig.guest(http);
    }
}
