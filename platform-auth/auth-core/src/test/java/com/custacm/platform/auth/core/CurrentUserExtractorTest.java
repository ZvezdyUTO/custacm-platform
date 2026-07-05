package com.custacm.platform.auth.core;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserExtractorTest {
    @Test
    void extractsSubjectAndPlatformRole() {
        Jwt jwt = jwt(Map.of(
                "sub", "230511213黄炳睿",
                "role", "player"
        ));

        CurrentUser currentUser = CurrentUserExtractor.from(jwt);

        assertThat(currentUser.studentIdentity()).isEqualTo("230511213黄炳睿");
        assertThat(currentUser.role()).isEqualTo("player");
    }

    @Test
    void requiresSubject() {
        Jwt jwt = jwt(Map.of("role", "player"));

        assertThatThrownBy(() -> CurrentUserExtractor.from(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sub");
    }

    @Test
    void requiresPlatformRole() {
        Jwt jwt = jwt(Map.of("sub", "230511213黄炳睿"));

        assertThatThrownBy(() -> CurrentUserExtractor.from(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role");
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
