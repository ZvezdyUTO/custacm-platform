package com.custacm.platform.trainingdata.web;

import com.custacm.platform.trainingdata.TrainingDataWebApplication;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesSubmissionSourceClient;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TrainingDataWebApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:cf_submission_collection_http;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "platform.training-data.codeforces.collector.request-interval=0s",
                "platform.training-data.atcoder.problem-list-collector.bootstrap-on-startup=false"
        }
)
@AutoConfigureMockMvc
class CodeforcesSubmissionCollectionHttpIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CodeforcesSubmissionSourceClient sourceClient;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminCollectsRecentCodeforcesSubmissionsForStudentIdentity() throws Exception {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                insert into oj_handle_account (student_identity, handles_json)
                values ('112487张三', '{"CODEFORCES":"alice"}'), ('112488李四', '{"CODEFORCES":"broken"}')
                """);
        when(sourceClient.fetchUserStatus("alice", 1, 1000))
                .thenReturn(page(
                        submission(1001, "alice", now.minus(Duration.ofHours(1))),
                        submission(1002, "alice", now.minus(Duration.ofDays(10)))
                ));

        mockMvc.perform(post("/api/training-data/admin/codeforces/submissions:collect")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "studentIdentity", "112487张三",
                                "lookbackHours", 120
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.requestedHandleCount").value(1))
                .andExpect(jsonPath("$.succeededHandleCount").value(1))
                .andExpect(jsonPath("$.failedHandleCount").value(0))
                .andExpect(jsonPath("$.matchedSubmissionCount").value(1))
                .andExpect(jsonPath("$.writtenRows").value(1))
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.handles[0].handle").value("alice"))
                .andExpect(jsonPath("$.handles[0].status").value("SUCCESS"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ods_codeforces__submission",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select author_handle from ods_codeforces__submission where codeforces_submission_id = 1001",
                String.class
        )).isEqualTo("alice");
        JsonNode collectionStates = objectMapper.readTree(jdbcTemplate.queryForObject("""
                select collection_states_json
                from oj_handle_account
                where student_identity = '112487张三'
                """, String.class));
        assertThat(collectionStates.path("CODEFORCES").path("historyStartReached").asBoolean()).isTrue();
        assertThat(collectionStates.path("CODEFORCES").path("lastCollectedAt").asText()).isNotBlank();
    }

    private ArrayNode page(Object... submissions) {
        ArrayNode page = objectMapper.createArrayNode();
        for (Object submission : submissions) {
            page.add((com.fasterxml.jackson.databind.JsonNode) submission);
        }
        return page;
    }

    private com.fasterxml.jackson.databind.JsonNode submission(long id, String handle, Instant submittedAt) {
        return objectMapper.createObjectNode()
                .put("id", id)
                .put("creationTimeSeconds", submittedAt.getEpochSecond())
                .set("author", objectMapper.createObjectNode()
                        .set("members", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode().put("handle", handle))));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
