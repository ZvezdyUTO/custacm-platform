package com.custacm.platform.trainingdata.web;

import com.custacm.platform.trainingdata.TrainingDataWebApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TrainingDataWebApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:cf_handle_account_http;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        }
)
@AutoConfigureMockMvc
class CodeforcesHandleAccountHttpIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminCreatesAndChangesIdentityWhileGuestQueriesByIdentity() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "studentIdentity", "112487张三",
                                "handle", "tourist"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentIdentity").value("112487张三"))
                .andExpect(jsonPath("$.handle").value("tourist"));

        mockMvc.perform(get("/api/training-data/codeforces/handles")
                        .param("studentIdentity", "112487张三")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentity").value("112487张三"))
                .andExpect(jsonPath("$.handle").value("tourist"));

        mockMvc.perform(patch("/api/training-data/admin/codeforces/handles:change-identity")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "oldStudentIdentity", "112487张三",
                                "newStudentIdentity", "112488张三"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentity").value("112488张三"))
                .andExpect(jsonPath("$.handle").value("tourist"));

        mockMvc.perform(get("/api/training-data/codeforces/handles")
                        .param("studentIdentity", "112487张三"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND"));
        mockMvc.perform(get("/api/training-data/codeforces/handles")
                        .param("studentIdentity", "112488张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handle").value("tourist"));

        String storedHandle = jdbcTemplate.queryForObject("""
                select codeforces_handle
                from codeforces_handle_account
                where student_identity = '112488张三'
                """, String.class);
        assertThat(storedHandle).isEqualTo("tourist");
    }

    @Test
    void rejectsDuplicateIdentityAndHandleConflicts() throws Exception {
        create("112489王五", "Benq");

        mockMvc.perform(post("/api/training-data/admin/codeforces/handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "studentIdentity", "112489王五",
                                "handle", "ecnerwala"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS"));
        mockMvc.perform(post("/api/training-data/admin/codeforces/handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "studentIdentity", "112490赵六",
                                "handle", "Benq"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODEFORCES_HANDLE_ACCOUNT_HANDLE_EXISTS"));
    }

    @Test
    void rejectsPlayerWritesButAllowsPublicReadThroughSecurity() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "studentIdentity", "112487张三",
                                "handle", "tourist"
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/training-data/codeforces/handles")
                        .param("studentIdentity", "112487张三"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND"));
    }

    private void create(String studentIdentity, String handle) throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "studentIdentity", studentIdentity,
                                "handle", handle
                        ))))
                .andExpect(status().isCreated());
    }

    private static String json(Map<String, String> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(entry.getKey()).append("\":\"")
                    .append(entry.getValue()).append('"');
            first = false;
        }
        return builder.append('}').toString();
    }
}
