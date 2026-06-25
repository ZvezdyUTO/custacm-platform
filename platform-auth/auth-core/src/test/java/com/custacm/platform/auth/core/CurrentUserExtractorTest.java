package com.custacm.platform.auth.core;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserExtractorTest {
    @Test
    void extractsStudentIdentityAndPlatformRole() {
        Jwt jwt = jwt(Map.of(
                "student_identity", "112487张三",
                "realm_access", Map.of("roles", java.util.List.of("student"))
        ));

        CurrentUser currentUser = CurrentUserExtractor.from(jwt);

        assertThat(currentUser.studentIdentity()).isEqualTo("112487张三");
        assertThat(currentUser.role()).isEqualTo("student");
    }

    @Test
    void requiresStudentIdentityClaim() {
        Jwt jwt = jwt(Map.of(
                "realm_access", Map.of("roles", java.util.List.of("student"))
        ));

        assertThatThrownBy(() -> CurrentUserExtractor.from(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("student_identity");
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                claims
        );
    }
}
