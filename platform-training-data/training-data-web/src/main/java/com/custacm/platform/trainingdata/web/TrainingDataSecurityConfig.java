package com.custacm.platform.trainingdata.web;

import com.custacm.platform.auth.core.PemRsaKeys;
import com.custacm.platform.auth.core.PlatformJwtDecoders;
import com.custacm.platform.auth.core.PlatformSecurityConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(TrainingDataJwtProperties.class)
public class TrainingDataSecurityConfig {
    @Bean
    public JwtDecoder trainingDataJwtDecoder(TrainingDataJwtProperties properties) {
        return PlatformJwtDecoders.rsa(PemRsaKeys.publicKey(properties.publicKey(), properties.publicKeyPath()));
    }

    @Bean
    @Order(PlatformSecurityConfig.PROTECTED_CHAIN_ORDER)
    public SecurityFilterChain trainingDataProtectedSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder)
            throws Exception {
        return PlatformSecurityConfig.statelessJwt(http, jwtDecoder)
                .admin("/api/training-data/admin/**")
                .build();
    }

    @Bean
    @Order(PlatformSecurityConfig.GUEST_CHAIN_ORDER)
    public SecurityFilterChain trainingDataGuestSecurityFilterChain(HttpSecurity http) throws Exception {
        return PlatformSecurityConfig.guest(http);
    }
}
