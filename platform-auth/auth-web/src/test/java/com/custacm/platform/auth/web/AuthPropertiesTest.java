package com.custacm.platform.auth.web;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPropertiesTest {
    @Test
    void jwtTtlDefaultsToTwoHours() {
        assertThat(new AuthProperties.Jwt(null, null, null, null, null, null).resolvedAccessTokenTtl())
                .isEqualTo(Duration.ofHours(2));
        assertThat(new AuthProperties.Jwt(null, null, null, null, Duration.ofMinutes(30), null).resolvedAccessTokenTtl())
                .isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void rememberMeJwtTtlDefaultsToThirtyDays() {
        assertThat(new AuthProperties.Jwt(null, null, null, null, null, null).resolvedRememberMeAccessTokenTtl())
                .isEqualTo(Duration.ofDays(30));
        assertThat(new AuthProperties.Jwt(null, null, null, null, null, Duration.ofDays(7)).resolvedRememberMeAccessTokenTtl())
                .isEqualTo(Duration.ofDays(7));
    }

    @Test
    void bootstrapAdminReportsConfiguredAndCompleteState() {
        assertThat(new AuthProperties.BootstrapAdmin(null, null).configured()).isFalse();
        assertThat(new AuthProperties.BootstrapAdmin("root", null).configured()).isTrue();
        assertThat(new AuthProperties.BootstrapAdmin("root", null).complete()).isFalse();
        assertThat(new AuthProperties.BootstrapAdmin("root", "rootPass123").complete()).isTrue();
    }
}
