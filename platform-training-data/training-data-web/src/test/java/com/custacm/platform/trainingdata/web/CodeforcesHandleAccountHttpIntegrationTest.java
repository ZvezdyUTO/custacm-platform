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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                .andExpect(jsonPath("$.handle").value("tourist"))
                .andExpect(jsonPath("$.needCollect").value(true));

        mockMvc.perform(get("/api/training-data/codeforces/handles")
                        .param("studentIdentity", "112487张三")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentity").value("112487张三"))
                .andExpect(jsonPath("$.handle").value("tourist"))
                .andExpect(jsonPath("$.needCollect").value(true));

        mockMvc.perform(patch("/api/training-data/admin/codeforces/handles:change-identity")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldStudentIdentity": "112487张三",
                                  "newStudentIdentity": "112488张三",
                                  "needCollect": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentity").value("112488张三"))
                .andExpect(jsonPath("$.handle").value("tourist"))
                .andExpect(jsonPath("$.needCollect").value(false));

        mockMvc.perform(get("/api/training-data/codeforces/handles")
                        .param("studentIdentity", "112487张三"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND"));
        mockMvc.perform(get("/api/training-data/codeforces/handles")
                        .param("studentIdentity", "112488张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handle").value("tourist"))
                .andExpect(jsonPath("$.needCollect").value(false));

        String storedHandle = jdbcTemplate.queryForObject("""
                select codeforces_handle
                from codeforces_handle_account
                where student_identity = '112488张三'
                """, String.class);
        assertThat(storedHandle).isEqualTo("tourist");
        Boolean needCollect = jdbcTemplate.queryForObject("""
                select need_collect
                from codeforces_handle_account
                where student_identity = '112488张三'
                """, Boolean.class);
        assertThat(needCollect).isFalse();
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

    @Test
    void adminPurgesCodeforcesDataForOneStudentIdentity() throws Exception {
        String targetIdentity = "112491清理目标";
        String targetHandle = "purgeTarget";
        String otherIdentity = "112492保留目标";
        String otherHandle = "purgeKeep";
        create(targetIdentity, targetHandle);
        create(otherIdentity, otherHandle);
        insertCodeforcesRows(targetHandle, 390000001L);
        insertCodeforcesRows(otherHandle, 390000002L);

        mockMvc.perform(delete("/api/training-data/admin/codeforces/users/{studentIdentity}/data", targetIdentity)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentity").value(targetIdentity))
                .andExpect(jsonPath("$.handle").value(targetHandle))
                .andExpect(jsonPath("$.handleAccountRows").value(1))
                .andExpect(jsonPath("$.odsSubmissionRows").value(1))
                .andExpect(jsonPath("$.dwdSubmissionRows").value(1))
                .andExpect(jsonPath("$.dwmFirstAcceptedRows").value(1))
                .andExpect(jsonPath("$.dwsAcceptedSummaryRows").value(1))
                .andExpect(jsonPath("$.totalDeletedRows").value(5));

        assertThat(count("codeforces_handle_account", "student_identity = '" + targetIdentity + "'")).isZero();
        assertThat(count("codeforces_handle_account", "student_identity = '" + otherIdentity + "'")).isEqualTo(1);
        assertThat(count("ods_codeforces__submission", "author_handle = '" + targetHandle + "'")).isZero();
        assertThat(count("dwd_codeforces__submission", "author_handle = '" + targetHandle + "'")).isZero();
        assertThat(count("dwm_codeforces__handle_problem_first_accepted", "author_handle = '" + targetHandle + "'")).isZero();
        assertThat(count("dws_codeforces__handle_daily_rating_accepted_summary", "author_handle = '" + targetHandle + "'")).isZero();
        assertThat(count("ods_codeforces__submission", "author_handle = '" + otherHandle + "'")).isEqualTo(1);
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

    private void insertCodeforcesRows(String handle, long submissionId) {
        jdbcTemplate.update("""
                insert into ods_codeforces__submission (
                    codeforces_submission_id,
                    author_handle,
                    batch_id,
                    fetched_at,
                    raw_payload,
                    payload_hash
                ) values (?, ?, 'batch-purge-http', timestamp '2026-07-06 00:00:00', '{}', ?)
                """, submissionId, handle, hash(submissionId));
        jdbcTemplate.update("""
                insert into dwd_codeforces__submission (
                    ods_submission_id,
                    codeforces_submission_id,
                    author_handle,
                    submitted_at_utc_plus8,
                    submitted_date_utc_plus8,
                    problem_key,
                    problem_contest_id,
                    problem_index,
                    is_accepted,
                    ods_batch_id,
                    ods_fetched_at,
                    ods_payload_hash
                ) values (?, ?, ?, timestamp '2026-07-06 08:00:00', date '2026-07-06',
                    '1000:A', 1000, 'A', 1, 'batch-purge-http', timestamp '2026-07-06 00:00:00', ?)
                """, submissionId, submissionId, handle, hash(submissionId));
        jdbcTemplate.update("""
                insert into dwm_codeforces__handle_problem_first_accepted (
                    author_handle,
                    problem_key,
                    problem_contest_id,
                    problem_index,
                    first_accepted_submission_id,
                    first_accepted_at_utc_plus8,
                    first_accepted_date_utc_plus8
                ) values (?, '1000:A', 1000, 'A', ?, timestamp '2026-07-06 08:00:00', date '2026-07-06')
                """, handle, submissionId);
        jdbcTemplate.update("""
                insert into dws_codeforces__handle_daily_rating_accepted_summary (
                    author_handle,
                    accepted_date_utc_plus8,
                    rating_800_accepted_problem_count
                ) values (?, date '2026-07-06', 1)
                """, handle);
    }

    private int count(String tableName, String predicate) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName + " where " + predicate, Integer.class);
    }

    private static String hash(long submissionId) {
        return String.format("%064d", submissionId);
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
