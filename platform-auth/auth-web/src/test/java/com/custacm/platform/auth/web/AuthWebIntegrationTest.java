package com.custacm.platform.auth.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthWebIntegrationTest {
    private static final KeyPair RSA_KEY_PAIR = rsaKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void authProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:auth_web_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("platform.auth.jwt.private-key", () -> pem("PRIVATE KEY", RSA_KEY_PAIR.getPrivate().getEncoded()));
        registry.add("platform.auth.jwt.public-key", () -> pem("PUBLIC KEY", RSA_KEY_PAIR.getPublic().getEncoded()));
        registry.add("platform.auth.jwt.access-token-ttl", () -> "2h");
        registry.add("platform.auth.bootstrap-admin.student-identity", () -> "root");
        registry.add("platform.auth.bootstrap-admin.password", () -> "rootPass123");
    }

    @Test
    void loginCurrentUserAdminManagementAndPlayerPasswordChangeWork() throws Exception {
        String adminToken = login("root", "rootPass123");

        mockMvc.perform(get("/api/auth/player/me").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentity").value("root"))
                .andExpect(jsonPath("$.role").value("admin"));

        MvcResult createPlayerResult = mockMvc.perform(post("/api/auth/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "studentIdentity", "230511213黄炳睿",
                                "password", "",
                                "role", "player"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.studentIdentity").value("230511213黄炳睿"))
                .andExpect(jsonPath("$.user.role").value("player"))
                .andExpect(jsonPath("$.plainPassword").exists())
                .andReturn();
        String playerPassword = objectMapper.readTree(createPlayerResult.getResponse().getContentAsString())
                .get("plainPassword").asText();

        String playerToken = login("230511213黄炳睿", playerPassword);
        mockMvc.perform(get("/api/auth/users").header("Authorization", bearer(playerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].studentIdentity", hasItem("230511213黄炳睿")));

        mockMvc.perform(patch("/api/auth/player/me/password")
                        .header("Authorization", bearer(playerToken))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "oldPassword", playerPassword,
                                "newPassword", "newPlayerPass123",
                                "confirmNewPassword", "differentPlayerPass123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REQUEST"));

        mockMvc.perform(patch("/api/auth/player/me/password")
                        .header("Authorization", bearer(playerToken))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "oldPassword", playerPassword,
                                "newPassword", "newPlayerPass123",
                                "confirmNewPassword", "newPlayerPass123"
                        ))))
                .andExpect(status().isNoContent());

        login("230511213黄炳睿", "newPlayerPass123");

        mockMvc.perform(delete("/api/auth/admin/users/230511213黄炳睿")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.studentIdentity").value("230511213黄炳睿"));
    }

    @Test
    void guestEndpointsIgnoreBearerToken() throws Exception {
        mockMvc.perform(get("/health").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/users").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].studentIdentity", hasItem("root")));

        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer not-a-jwt")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "studentIdentity", "root",
                                "password", "rootPass123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void playerEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/player/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotDowngradeSelf() throws Exception {
        String adminToken = login("root", "rootPass123");

        mockMvc.perform(patch("/api/auth/admin/users/root")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("role", "player"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void adminBatchListUpdateResetPasswordDisableAndDeleteWork() throws Exception {
        String adminToken = login("root", "rootPass123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String first = "batch-player-" + suffix;
        String second = "batch-admin-" + suffix;

        mockMvc.perform(post("/api/auth/admin/users:batch-create")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("users", java.util.List.of(
                                Map.of("studentIdentity", first, "password", "firstPass123", "role", "player"),
                                Map.of("studentIdentity", second, "password", "secondPass123", "role", "player")
                        )))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].success").value(true))
                .andExpect(jsonPath("$[0].user.studentIdentity").value(first))
                .andExpect(jsonPath("$[0].plainPassword").value("firstPass123"))
                .andExpect(jsonPath("$[1].success").value(true))
                .andExpect(jsonPath("$[1].user.studentIdentity").value(second))
                .andExpect(jsonPath("$[1].plainPassword").value("secondPass123"));

        String third = "batch-third-" + suffix;
        mockMvc.perform(post("/api/auth/admin/users:batch-create")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("users", java.util.List.of(
                                Map.of("studentIdentity", second, "password", "duplicatePass123", "role", "player"),
                                Map.of("studentIdentity", third, "password", "", "role", "player")
                        )))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].success").value(false))
                .andExpect(jsonPath("$[0].studentIdentity").value(second))
                .andExpect(jsonPath("$[0].errorCode").value("AUTH_USER_EXISTS"))
                .andExpect(jsonPath("$[1].success").value(true))
                .andExpect(jsonPath("$[1].user.studentIdentity").value(third))
                .andExpect(jsonPath("$[1].plainPassword").exists());

        mockMvc.perform(get("/api/auth/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].studentIdentity", hasItem(first)))
                .andExpect(jsonPath("$[*].studentIdentity", hasItem(second)));

        mockMvc.perform(patch("/api/auth/admin/users/{studentIdentity}", first)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("role", "disable"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.role").value("disable"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("studentIdentity", first, "password", "firstPass123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_USER_DISABLED"));

        MvcResult resetSecondResult = mockMvc.perform(patch("/api/auth/admin/users/{studentIdentity}", second)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("newPassword", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.studentIdentity").value(second))
                .andExpect(jsonPath("$.plainPassword").exists())
                .andReturn();
        String resetSecondPassword = objectMapper.readTree(resetSecondResult.getResponse().getContentAsString())
                .get("plainPassword").asText();

        login(second, resetSecondPassword);

        mockMvc.perform(patch("/api/auth/admin/users/{studentIdentity}", second)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("role", "admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("admin"));

        mockMvc.perform(patch("/api/auth/admin/users/{studentIdentity}", second)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("role", "player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("player"));

        mockMvc.perform(patch("/api/auth/admin/users/{studentIdentity}", second)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("role", "guest", "newPassword", "guestNullPass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("player"))
                .andExpect(jsonPath("$.plainPassword").value("guestNullPass123"));

        login(second, "guestNullPass123");

        mockMvc.perform(delete("/api/auth/admin/users/{studentIdentity}", first)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(delete("/api/auth/admin/users/{studentIdentity}", second)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(delete("/api/auth/admin/users/{studentIdentity}", third)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void authErrorsUseDocumentedResponseShape() throws Exception {
        String adminToken = login("root", "rootPass123");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String duplicate = "duplicate-" + suffix;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("studentIdentity", "invalid-" + suffix, "password", "wrongPass123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/auth/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "studentIdentity", "guest-" + suffix,
                                "password", "guestPass123",
                                "role", "guest"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REQUEST"));

        mockMvc.perform(post("/api/auth/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "studentIdentity", duplicate,
                                "password", "duplicatePass123",
                                "role", "player"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/admin/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "studentIdentity", duplicate,
                                "password", "duplicatePass123",
                                "role", "player"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH_USER_EXISTS"));

        mockMvc.perform(patch("/api/auth/admin/users/missing-" + suffix)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("newPassword", "resetPass123"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AUTH_USER_NOT_FOUND"));

        mockMvc.perform(patch("/api/auth/admin/users/{studentIdentity}", duplicate)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(Map.of("role", "guest"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_REQUEST"));

        mockMvc.perform(delete("/api/auth/admin/users/{studentIdentity}", duplicate)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void failedLoginStartsShortRetryCooldown() throws Exception {
        String missingIdentity = "missing-" + UUID.randomUUID();
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("studentIdentity", missingIdentity, "password", "wrongPass123"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("studentIdentity", missingIdentity, "password", "wrongPass123"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_LOGIN_RATE_LIMITED"));
    }

    private String login(String studentIdentity, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "studentIdentity", studentIdentity,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String pem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }
}
