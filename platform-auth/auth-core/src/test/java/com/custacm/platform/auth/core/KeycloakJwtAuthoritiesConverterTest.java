package com.custacm.platform.auth.core;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthoritiesConverterTest {
    @Test
    void convertsPlatformRoleToSpringAuthority() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("realm_access", Map.of("roles", List.of("student")))
        );

        assertThat(new KeycloakJwtAuthoritiesConverter().convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_student");
    }
}
