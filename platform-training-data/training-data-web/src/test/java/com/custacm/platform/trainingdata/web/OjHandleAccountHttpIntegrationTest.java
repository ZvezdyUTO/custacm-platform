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
                "spring.datasource.driver-class-name=org.h2.Driver",
                "platform.training-data.atcoder.problem-list-collector.bootstrap-on-startup=false"
        }
)
@AutoConfigureMockMvc
class OjHandleAccountHttpIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminCreatesAndChangesIdentityWhileGuestListsAllBindings() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/oj-handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handlePayload("112487张三", "tourist", "tourist_atcoder")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentIdentity").value("112487张三"))
                .andExpect(jsonPath("$.handles.CODEFORCES").value("tourist"))
                .andExpect(jsonPath("$.handles.ATCODER").value("tourist_atcoder"))
                .andExpect(jsonPath("$.needCollect").value(true))
                .andExpect(jsonPath("$.collectionStates.CODEFORCES.historyStartReached").value(false))
                .andExpect(jsonPath("$.collectionStates.ATCODER.historyStartReached").value(false));

        mockMvc.perform(get("/api/training-data/oj-handles")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['112487张三'].studentIdentity").value("112487张三"))
                .andExpect(jsonPath("$['112487张三'].handles.CODEFORCES").value("tourist"))
                .andExpect(jsonPath("$['112487张三'].handles.ATCODER").value("tourist_atcoder"))
                .andExpect(jsonPath("$['112487张三'].needCollect").value(true))
                .andExpect(jsonPath("$['112487张三'].collectionStates.CODEFORCES.historyStartReached").value(false))
                .andExpect(jsonPath("$['112487张三'].collectionStates.ATCODER.historyStartReached").value(false));

        mockMvc.perform(patch("/api/training-data/admin/oj-handles:change-identity")
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
                .andExpect(jsonPath("$.handles.CODEFORCES").value("tourist"))
                .andExpect(jsonPath("$.handles.ATCODER").value("tourist_atcoder"))
                .andExpect(jsonPath("$.needCollect").value(false))
                .andExpect(jsonPath("$.collectionStates.CODEFORCES.historyStartReached").value(false))
                .andExpect(jsonPath("$.collectionStates.ATCODER.historyStartReached").value(false));

        mockMvc.perform(get("/api/training-data/oj-handles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['112487张三']").doesNotExist())
                .andExpect(jsonPath("$['112488张三'].handles.CODEFORCES").value("tourist"))
                .andExpect(jsonPath("$['112488张三'].handles.ATCODER").value("tourist_atcoder"))
                .andExpect(jsonPath("$['112488张三'].needCollect").value(false));

        mockMvc.perform(get("/api/training-data/oj-handles")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['112488张三'].studentIdentity").value("112488张三"))
                .andExpect(jsonPath("$['112488张三'].handles.CODEFORCES").value("tourist"))
                .andExpect(jsonPath("$['112488张三'].handles.ATCODER").value("tourist_atcoder"));

        String storedHandlesJson = jdbcTemplate.queryForObject("""
                select handles_json
                from oj_handle_account
                where student_identity = '112488张三'
                """, String.class);
        assertThat(storedHandlesJson).contains("\"CODEFORCES\":\"tourist\"");
        assertThat(storedHandlesJson).contains("\"ATCODER\":\"tourist_atcoder\"");
        Boolean needCollect = jdbcTemplate.queryForObject("""
                select need_collect
                from oj_handle_account
                where student_identity = '112488张三'
                """, Boolean.class);
        assertThat(needCollect).isFalse();
    }

    @Test
    void rejectsDuplicateIdentityAndHandleConflicts() throws Exception {
        create("112489王五", "Benq", "benq_atcoder");

        mockMvc.perform(post("/api/training-data/admin/oj-handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handlePayload("112489王五", "ecnerwala", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS"));
        mockMvc.perform(post("/api/training-data/admin/oj-handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handlePayload("112490赵六", "Benq", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OJ_HANDLE_ACCOUNT_HANDLE_EXISTS"));
        mockMvc.perform(post("/api/training-data/admin/oj-handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handlePayload("112491钱七", "ecnerwala", "benq_atcoder")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OJ_HANDLE_ACCOUNT_HANDLE_EXISTS"));
    }

    @Test
    void rejectsPlayerWritesButAllowsPublicReadThroughSecurity() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/oj-handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handlePayload("112487张三", "tourist", null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/training-data/oj-handles")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isOk());
    }

    @Test
    void adminPurgesCodeforcesDataForOneStudentIdentity() throws Exception {
        String targetIdentity = "112491清理目标";
        String targetHandle = "purgeTarget";
        String otherIdentity = "112492保留目标";
        String otherHandle = "purgeKeep";
        create(targetIdentity, targetHandle, null);
        create(otherIdentity, otherHandle, null);
        insertCodeforcesRows(targetHandle, 390000001L);
        insertCodeforcesRows(otherHandle, 390000002L);

        mockMvc.perform(delete("/api/training-data/admin/students/{studentIdentity}/oj-data", targetIdentity)
                        .queryParam("ojName", "CODEFORCES")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentity").value(targetIdentity))
                .andExpect(jsonPath("$.ojName").value("CODEFORCES"))
                .andExpect(jsonPath("$.handle").value(targetHandle))
                .andExpect(jsonPath("$.handleAccountRows").value(0))
                .andExpect(jsonPath("$.odsSubmissionRows").value(1))
                .andExpect(jsonPath("$.dwdSubmissionRows").value(1))
                .andExpect(jsonPath("$.dwmFirstAcceptedRows").value(1))
                .andExpect(jsonPath("$.dwsAcceptedSummaryRows").value(1))
                .andExpect(jsonPath("$.totalDeletedRows").value(4));

        assertThat(count("oj_handle_account", "student_identity = '" + targetIdentity + "'")).isEqualTo(1);
        assertThat(count("oj_handle_account", "student_identity = '" + otherIdentity + "'")).isEqualTo(1);
        assertThat(count("ods_codeforces__submission", "author_handle = '" + targetHandle + "'")).isZero();
        assertThat(count("dwd_codeforces__submission", "handle = '" + targetHandle + "'")).isZero();
        assertThat(count("dwm_codeforces__handle_problem_first_accepted", "handle = '" + targetHandle + "'")).isZero();
        assertThat(count("dws_codeforces__handle_daily_rating_accepted_summary", "handle = '" + targetHandle + "'")).isZero();
        assertThat(count("ods_codeforces__submission", "author_handle = '" + otherHandle + "'")).isEqualTo(1);
    }

    @Test
    void adminPurgeRequiresOjName() throws Exception {
        create("112494缺少OjName", "missingOjNameHandle", null);

        mockMvc.perform(delete("/api/training-data/admin/students/{studentIdentity}/oj-data", "112494缺少OjName")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OJ_STUDENT_DATA_PURGE_INVALID_REQUEST"));
    }

    @Test
    void adminPurgesAtcoderDataForOneStudentIdentity() throws Exception {
        String targetIdentity = "112493AtCoder清理目标";
        String codeforcesHandle = "atcoderPurgeCf";
        String atcoderHandle = "atcoderPurgeTarget";
        create(targetIdentity, codeforcesHandle, atcoderHandle);
        insertAtcoderRows(atcoderHandle, 510000001L);

        mockMvc.perform(delete("/api/training-data/admin/students/{studentIdentity}/oj-data", targetIdentity)
                        .queryParam("ojName", "ATCODER")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentIdentity").value(targetIdentity))
                .andExpect(jsonPath("$.ojName").value("ATCODER"))
                .andExpect(jsonPath("$.handle").value(atcoderHandle))
                .andExpect(jsonPath("$.handleAccountRows").value(0))
                .andExpect(jsonPath("$.odsSubmissionRows").value(1))
                .andExpect(jsonPath("$.dwdSubmissionRows").value(1))
                .andExpect(jsonPath("$.dwmFirstAcceptedRows").value(1))
                .andExpect(jsonPath("$.dwsAcceptedSummaryRows").value(1))
                .andExpect(jsonPath("$.totalDeletedRows").value(4));

        assertThat(count("oj_handle_account", "student_identity = '" + targetIdentity + "'")).isEqualTo(1);
        assertThat(count("ods_atcoder__submission", "user_id = '" + atcoderHandle + "'")).isZero();
        assertThat(count("dwd_atcoder__submission", "handle = '" + atcoderHandle + "'")).isZero();
        assertThat(count("dwm_atcoder__handle_problem_first_accepted", "handle = '" + atcoderHandle + "'")).isZero();
        assertThat(count("dws_atcoder__handle_daily_rating_accepted_summary", "handle = '" + atcoderHandle + "'")).isZero();
    }

    private void create(String studentIdentity, String handle, String atcoderHandle) throws Exception {
        mockMvc.perform(post("/api/training-data/admin/oj-handles")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handlePayload(studentIdentity, handle, atcoderHandle)))
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
                    submission_id,
                    handle,
                    submitted_at_utc_plus8,
                    submitted_date_utc_plus8,
                    problem_key,
                    problem_index,
                    problem_name,
                    difficulty,
                    language,
                    verdict,
                    is_accepted,
                    time_consumed_millis,
                    source_url,
                    ods_batch_id,
                    ods_fetched_at,
                    ods_payload_hash
                ) values (?, ?, ?, timestamp '2026-07-06 08:00:00', date '2026-07-06',
                    '1000:A', 'A', 'Problem A', '800', 'C++23', 'OK', 1, 46,
                    ?, 'batch-purge-http', timestamp '2026-07-06 00:00:00', ?)
                """, submissionId, String.valueOf(submissionId), handle,
                "https://codeforces.com/contest/1000/submission/" + submissionId,
                hash(submissionId));
        jdbcTemplate.update("""
                insert into dwm_codeforces__handle_problem_first_accepted (
                    handle,
                    problem_key,
                    problem_index,
                    problem_name,
                    difficulty,
                    first_accepted_submission_id,
                    first_accepted_at_utc_plus8,
                    first_accepted_date_utc_plus8,
                    first_accepted_language,
                    first_accepted_source_url
                ) values (?, '1000:A', 'A', 'Problem A', '800', ?, timestamp '2026-07-06 08:00:00',
                    date '2026-07-06', 'C++23', ?)
                """, handle, String.valueOf(submissionId),
                "https://codeforces.com/contest/1000/submission/" + submissionId);
        jdbcTemplate.update("""
                insert into dws_codeforces__handle_daily_rating_accepted_summary (
                    handle,
                    accepted_date_utc_plus8,
                    difficulty,
                    accepted_problem_count
                ) values (?, date '2026-07-06', '800', 1)
                """, handle);
    }

    private void insertAtcoderRows(String handle, long submissionId) {
        jdbcTemplate.update("""
                insert into ods_atcoder__submission (
                    atcoder_submission_id,
                    epoch_second,
                    problem_id,
                    contest_id,
                    user_id,
                    language,
                    point,
                    source_code_length,
                    result,
                    execution_time_millis,
                    batch_id,
                    fetched_at,
                    raw_payload,
                    payload_hash
                ) values (?, 1783305600, 'abc100_a', 'abc100', ?, 'C++23', 100.0, 1024,
                    'AC', 46, 'batch-atcoder-purge-http', timestamp '2026-07-06 00:00:00', '{}', ?)
                """, submissionId, handle, hash(submissionId));
        jdbcTemplate.update("""
                insert into dwd_atcoder__submission (
                    ods_submission_id,
                    submission_id,
                    handle,
                    submitted_at_utc_plus8,
                    submitted_date_utc_plus8,
                    problem_key,
                    problem_index,
                    problem_name,
                    difficulty,
                    language,
                    verdict,
                    is_accepted,
                    time_consumed_millis,
                    source_url,
                    ods_batch_id,
                    ods_fetched_at,
                    ods_payload_hash
                ) values (?, ?, ?, timestamp '2026-07-06 08:00:00', date '2026-07-06',
                    'abc100_a', 'A', 'Problem A', '800', 'C++23', 'AC', 1, 46,
                    ?, 'batch-atcoder-purge-http', timestamp '2026-07-06 00:00:00', ?)
                """, submissionId, String.valueOf(submissionId), handle,
                "https://atcoder.jp/contests/abc100/submissions/" + submissionId,
                hash(submissionId));
        jdbcTemplate.update("""
                insert into dwm_atcoder__handle_problem_first_accepted (
                    handle,
                    problem_key,
                    problem_index,
                    problem_name,
                    difficulty,
                    first_accepted_submission_id,
                    first_accepted_at_utc_plus8,
                    first_accepted_date_utc_plus8,
                    first_accepted_language,
                    first_accepted_source_url
                ) values (?, 'abc100_a', 'A', 'Problem A', '800', ?, timestamp '2026-07-06 08:00:00',
                    date '2026-07-06', 'C++23', ?)
                """, handle, String.valueOf(submissionId),
                "https://atcoder.jp/contests/abc100/submissions/" + submissionId);
        jdbcTemplate.update("""
                insert into dws_atcoder__handle_daily_rating_accepted_summary (
                    handle,
                    accepted_date_utc_plus8,
                    difficulty,
                    accepted_problem_count
                ) values (?, date '2026-07-06', '800', 1)
                """, handle);
    }

    private int count(String tableName, String predicate) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName + " where " + predicate, Integer.class);
    }

    private static String hash(long submissionId) {
        return String.format("%064d", submissionId);
    }

    private static String handlePayload(String studentIdentity, String codeforcesHandle, String atcoderHandle) {
        String atcoderEntry = atcoderHandle == null ? "" : ",\"ATCODER\":\"" + atcoderHandle + "\"";
        return """
                {
                  "studentIdentity": "%s",
                  "handles": {
                    "CODEFORCES": "%s"%s
                  }
                }
                """.formatted(studentIdentity, codeforcesHandle, atcoderEntry);
    }
}
