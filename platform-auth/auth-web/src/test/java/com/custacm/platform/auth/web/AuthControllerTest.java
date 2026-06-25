package com.custacm.platform.auth.web;

import com.custacm.platform.auth.interfaceapi.CurrentUserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {
    @Test
    void returnsCurrentStudentIdentityAndRole() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "student_identity", "112487张三",
                        "realm_access", Map.of("roles", List.of("student"))
                )
        );

        ResponseEntity<CurrentUserResponse> response = new AuthController().currentUser(jwt);

        assertThat(response.getBody()).isEqualTo(new CurrentUserResponse("112487张三", "student"));
    }
}
