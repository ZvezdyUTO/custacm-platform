package com.custacm.platform.trainingdata.web;

import com.custacm.platform.trainingdata.TrainingDataWebApplication;
import com.custacm.platform.trainingdata.atcoder.infra.RestClientAtcoderSourceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TrainingDataWebApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:atcoder_collection_http;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "platform.training-data.codeforces.collector.request-interval=0s",
                "platform.training-data.atcoder.collector.request-interval=0s",
                "platform.training-data.atcoder.problem-list-collector.bootstrap-on-startup=false"
        }
)
@AutoConfigureMockMvc
class AtcoderCollectionHttpIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestClientAtcoderSourceClient atcoderSourceClient;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminCollectsRecentAtcoderSubmissionsForStudentIdentity() throws Exception {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                insert into oj_handle_account (student_identity, handles_json)
                values ('112487张三', '{"ATCODER":"tourist"}')
                """);
        when(atcoderSourceClient.fetchUserSubmissions(eq("tourist"), anyLong()))
                .thenReturn(page(submission(5870139L, "tourist", now.minus(Duration.ofHours(1)))));

        mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "studentIdentity", "112487张三",
                                "lookbackHours", 120,
                                "ojName", "ATCODER"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ojName").value("ATCODER"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.matchedSubmissionCount").value(1))
                .andExpect(jsonPath("$.writtenRows").value(1))
                .andExpect(jsonPath("$.handles[0].handle").value("tourist"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ods_atcoder__submission",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select user_id from ods_atcoder__submission where atcoder_submission_id = 5870139",
                String.class
        )).isEqualTo("tourist");
        JsonNode collectionStates = objectMapper.readTree(jdbcTemplate.queryForObject("""
                select collection_states_json
                from oj_handle_account
                where student_identity = '112487张三'
                """, String.class));
        assertThat(collectionStates.path("ATCODER").path("historyStartReached").asBoolean()).isFalse();
        assertThat(collectionStates.path("ATCODER").path("lastCollectedAt").asText()).isNotBlank();
    }

    @Test
    void adminStartsAtcoderCollectionJobAndRefreshesWarehouse() throws Exception {
        Instant now = Instant.now();
        String studentIdentity = "112489AtCoder刷新目标";
        String handle = "refresh_user";
        long submissionId = 5870140L;
        jdbcTemplate.update("""
                insert into oj_handle_account (student_identity, handles_json)
                values (?, ?)
                """, studentIdentity, "{\"ATCODER\":\"" + handle + "\"}");
        insertProblem("abc122_c", "abc122", "C", "GeT AC", "ABC122 C - GeT AC");
        when(atcoderSourceClient.fetchUserSubmissions(eq(handle), anyLong()))
                .thenReturn(page(submission(
                        submissionId,
                        handle,
                        now.minus(Duration.ofHours(1)),
                        "abc122_c",
                        "abc122"
                )));

        MvcResult startResult = mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect-batch-jobs")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "studentIdentities", java.util.List.of(studentIdentity),
                                "lookbackHours", 120,
                                "refreshWarehouse", true,
                                "ojName", "ATCODER"
                        ))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.ojName").value("ATCODER"))
                .andReturn();
        String jobId = objectMapper.readTree(startResult.getResponse().getContentAsString()).path("jobId").asText();

        com.fasterxml.jackson.databind.JsonNode job = waitForFinishedJob(jobId);

        assertThat(job.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(job.path("refreshedCount").asInt()).isEqualTo(1);
        assertThat(job.path("items").get(0).path("refreshStatus").asText()).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ods_atcoder__submission where atcoder_submission_id = ?",
                Integer.class,
                submissionId
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from dwd_atcoder__submission where submission_id = ?",
                Integer.class,
                String.valueOf(submissionId)
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from dwm_atcoder__handle_problem_first_accepted where first_accepted_submission_id = ?",
                Integer.class,
                String.valueOf(submissionId)
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select accepted_problem_count
                from dws_atcoder__handle_daily_rating_accepted_summary
                where handle = ?
                  and difficulty = 'UNRATED'
                """, Integer.class, handle)).isEqualTo(1);
    }

    private ArrayNode page(Object... submissions) {
        ArrayNode page = objectMapper.createArrayNode();
        for (Object submission : submissions) {
            page.add((com.fasterxml.jackson.databind.JsonNode) submission);
        }
        return page;
    }

    private com.fasterxml.jackson.databind.JsonNode submission(long id, String userId, Instant submittedAt) {
        return submission(id, userId, submittedAt, "abc121_c", "abc121");
    }

    private com.fasterxml.jackson.databind.JsonNode submission(
            long id,
            String userId,
            Instant submittedAt,
            String problemId,
            String contestId
    ) {
        return objectMapper.createObjectNode()
                .put("id", id)
                .put("epoch_second", submittedAt.getEpochSecond())
                .put("problem_id", problemId)
                .put("contest_id", contestId)
                .put("user_id", userId)
                .put("language", "C++ 20")
                .put("point", 300.0)
                .put("length", 797)
                .put("result", "AC")
                .put("execution_time", 404);
    }

    private void insertProblem(
            String problemId,
            String contestId,
            String problemIndex,
            String problemName,
            String title
    ) {
        jdbcTemplate.update("""
                insert into ods_atcoder__problem (
                    problem_id,
                    contest_id,
                    problem_index,
                    problem_name,
                    title,
                    batch_id,
                    fetched_at,
                    raw_payload,
                    payload_hash
                ) values (?, ?, ?, ?, ?, 'batch-problems-http', timestamp '2026-07-08 00:00:00', '{}', ?)
                """, problemId, contestId, problemIndex, problemName, title,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    private com.fasterxml.jackson.databind.JsonNode waitForFinishedJob(String jobId) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            MvcResult listResult = mockMvc.perform(get("/api/training-data/admin/codeforces/submissions/collect-batch-jobs")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                    .andExpect(status().isOk())
                    .andReturn();
            com.fasterxml.jackson.databind.JsonNode jobs =
                    objectMapper.readTree(listResult.getResponse().getContentAsString());
            for (com.fasterxml.jackson.databind.JsonNode job : jobs) {
                if (jobId.equals(job.path("jobId").asText())
                        && !"RUNNING".equals(job.path("status").asText())) {
                    return job;
                }
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("AtCoder collection job did not finish: " + jobId);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
