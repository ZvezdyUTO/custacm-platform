package com.custacm.platform.trainingdata.web;

import com.custacm.platform.trainingdata.TrainingDataWebApplication;
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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TrainingDataWebApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:cf_ods_http_ingest;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        }
)
@AutoConfigureMockMvc
class CodeforcesOdsSubmissionHttpIngestIntegrationTest {
    private static final String FIXTURE = "fixtures/codeforces/submissions_multi_user_1000.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void postsLocalFixtureThroughAdminHttpEndpointAndUpsertsIdempotently() throws Exception {
        String payload = new ClassPathResource(FIXTURE).getContentAsString(StandardCharsets.UTF_8);

        postFixture(payload);
        postFixture(payload);

        Integer rowCount = jdbcTemplate.queryForObject("select count(*) from ods_codeforces__submission", Integer.class);
        Integer uniqueSubmissionCount = jdbcTemplate.queryForObject("""
                select count(distinct codeforces_submission_id)
                from ods_codeforces__submission
                """, Integer.class);
        Integer touristCount = jdbcTemplate.queryForObject("""
                select count(*)
                from ods_codeforces__submission
                where author_handle = 'tourist'
                """, Integer.class);

        assertThat(rowCount).isEqualTo(1000);
        assertThat(uniqueSubmissionCount).isEqualTo(1000);
        assertThat(touristCount).isPositive();
    }

    private void postFixture(String payload) throws Exception {
        mockMvc.perform(post("/api/training-data/admin/ods/codeforces/submissions:batch-upsert")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableName").value("ods_codeforces__submission"))
                .andExpect(jsonPath("$.writtenRows").value(1000));
    }
}
