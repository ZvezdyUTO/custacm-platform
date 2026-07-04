package com.custacm.platform.trainingdata.codeforces.config;

import com.custacm.platform.trainingdata.codeforces.app.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.infra.CodeforcesSubmissionParser;
import com.custacm.platform.trainingdata.codeforces.infra.JdbcCodeforcesOdsSubmissionWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class CodeforcesTrainingDataConfig {
    @Bean
    CodeforcesSubmissionParser codeforcesSubmissionParser(ObjectMapper objectMapper) {
        return new CodeforcesSubmissionParser(objectMapper);
    }

    @Bean
    CodeforcesOdsSubmissionWriter codeforcesOdsSubmissionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        return new JdbcCodeforcesOdsSubmissionWriter(jdbcTemplate);
    }

    @Bean
    CodeforcesOdsSubmissionIngestService codeforcesOdsSubmissionIngestService(
            CodeforcesSubmissionParser parser,
            CodeforcesOdsSubmissionWriter writer,
            ObjectMapper objectMapper
    ) {
        return new CodeforcesOdsSubmissionIngestService(parser, writer, objectMapper);
    }
}
