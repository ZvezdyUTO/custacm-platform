package com.custacm.platform.auth.core;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformJwtAuthenticationConvertersTest {
    @Test
    void usesSubjectAsPrincipalAndPlatformRoleAsAuthority() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "230511213黄炳睿", "role", "admin")
        );

        var authentication = PlatformJwtAuthenticationConverters.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getName()).isEqualTo("230511213黄炳睿");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_admin", "ROLE_player");
    }
}
