package com.custacm.platform.trainingdata.codeforces.config;

import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesSubmissionQueryService;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesSubmissionRepository;
import com.custacm.platform.trainingdata.codeforces.domain.parser.CodeforcesSubmissionParser;
import com.custacm.platform.trainingdata.codeforces.infra.parser.JacksonCodeforcesSubmissionParser;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesSubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class CodeforcesTrainingDataConfig {
    @Bean
    CodeforcesSubmissionParser codeforcesSubmissionParser(ObjectMapper objectMapper) {
        return new JacksonCodeforcesSubmissionParser(objectMapper);
    }

    @Bean
    CodeforcesOdsSubmissionWriter codeforcesOdsSubmissionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        return new JdbcCodeforcesOdsSubmissionWriter(jdbcTemplate);
    }

    @Bean
    CodeforcesAcceptedSummaryRepository codeforcesAcceptedSummaryRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcCodeforcesAcceptedSummaryRepository(jdbcTemplate);
    }

    @Bean
    CodeforcesSubmissionRepository codeforcesSubmissionRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcCodeforcesSubmissionRepository(jdbcTemplate);
    }

    @Bean
    CodeforcesFirstAcceptedProblemRepository codeforcesFirstAcceptedProblemRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcCodeforcesFirstAcceptedProblemRepository(jdbcTemplate);
    }

    @Bean
    CodeforcesOdsSubmissionIngestService codeforcesOdsSubmissionIngestService(
            CodeforcesSubmissionParser parser,
            CodeforcesOdsSubmissionWriter writer,
            ObjectMapper objectMapper
    ) {
        return new CodeforcesOdsSubmissionIngestService(parser, writer, objectMapper);
    }

    @Bean
    CodeforcesAcceptedSummaryQueryService codeforcesAcceptedSummaryQueryService(
            CodeforcesAcceptedSummaryRepository repository
    ) {
        return new CodeforcesAcceptedSummaryQueryService(repository);
    }

    @Bean
    CodeforcesSubmissionQueryService codeforcesSubmissionQueryService(
            CodeforcesSubmissionRepository repository
    ) {
        return new CodeforcesSubmissionQueryService(repository);
    }

    @Bean
    CodeforcesFirstAcceptedProblemQueryService codeforcesFirstAcceptedProblemQueryService(
            CodeforcesFirstAcceptedProblemRepository repository
    ) {
        return new CodeforcesFirstAcceptedProblemQueryService(repository);
    }
}
