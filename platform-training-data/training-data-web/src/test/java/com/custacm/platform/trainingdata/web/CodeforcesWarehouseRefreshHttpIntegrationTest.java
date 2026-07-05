package com.custacm.platform.trainingdata.web;

import com.custacm.platform.trainingdata.TrainingDataWebApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TrainingDataWebApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:cf_warehouse_refresh_http;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        }
)
@AutoConfigureMockMvc
class CodeforcesWarehouseRefreshHttpIntegrationTest {
    private static final String FIXTURE = "fixtures/codeforces/submissions_multi_user_1000.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminRefreshesWarehouseFromCodeforcesSqlTaskDagAndCanResumeFromNode() throws Exception {
        String batchId = ingestFixture();

        mockMvc.perform(post("/api/training-data/admin/codeforces/warehouse:refresh")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("batchId", batchId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.tasks.length()").value(3))
                .andExpect(jsonPath("$.tasks[0].taskId").value("codeforces.dwd.submission"))
                .andExpect(jsonPath("$.tasks[1].taskId").value("codeforces.dwm.handle_problem_first_accepted"))
                .andExpect(jsonPath("$.tasks[2].taskId").value("codeforces.dws.handle_daily_rating_accepted_summary"))
                .andExpect(jsonPath("$.tasks[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.tasks[1].status").value("SUCCESS"))
                .andExpect(jsonPath("$.tasks[2].status").value("SUCCESS"));

        assertThat(count("ods_codeforces__submission")).isEqualTo(1000);
        assertThat(count("dwd_codeforces__submission")).isEqualTo(1000);
        assertThat(count("dwm_codeforces__handle_problem_first_accepted")).isPositive();
        assertThat(count("dws_codeforces__handle_daily_rating_accepted_summary")).isPositive();

        mockMvc.perform(post("/api/training-data/admin/codeforces/warehouse:refresh")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "batchId", batchId,
                                "startFromTaskId", "codeforces.dwm.handle_problem_first_accepted"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.startFromTaskId").value("codeforces.dwm.handle_problem_first_accepted"))
                .andExpect(jsonPath("$.tasks.length()").value(2))
                .andExpect(jsonPath("$.tasks[0].taskId").value("codeforces.dwm.handle_problem_first_accepted"))
                .andExpect(jsonPath("$.tasks[1].taskId").value("codeforces.dws.handle_daily_rating_accepted_summary"));
    }

    @Test
    void refreshRejectsUnknownResumeTaskId() throws Exception {
        String batchId = ingestFixture();

        mockMvc.perform(post("/api/training-data/admin/codeforces/warehouse:refresh")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "batchId", batchId,
                                "startFromTaskId", "missing.task"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SQL_TASK_START_NODE_INVALID"));
    }

    @Test
    void refreshRejectsBatchWithoutRefreshInterval() throws Exception {
        mockMvc.perform(post("/api/training-data/admin/codeforces/warehouse:refresh")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("batchId", "missing-batch"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODEFORCES_WAREHOUSE_REFRESH_INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("batchId has no Codeforces submissions with creationTimeSeconds"));
    }

    private String ingestFixture() throws Exception {
        String payload = new ClassPathResource(FIXTURE).getContentAsString(StandardCharsets.UTF_8);
        MvcResult result = mockMvc.perform(post("/api/training-data/admin/ods/codeforces/submissions:batch-upsert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("batchId").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
    }
}
