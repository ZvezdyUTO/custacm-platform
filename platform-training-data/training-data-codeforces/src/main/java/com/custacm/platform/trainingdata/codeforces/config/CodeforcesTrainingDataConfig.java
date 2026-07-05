package com.custacm.platform.trainingdata.codeforces.config;

import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.codeforces.collector.config.CodeforcesCollectorProperties;
import com.custacm.platform.trainingdata.codeforces.app.query.CodeforcesAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.codeforces.app.query.CodeforcesFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountService;
import com.custacm.platform.trainingdata.codeforces.app.ingest.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.codeforces.app.collector.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.app.query.CodeforcesSubmissionQueryService;
import com.custacm.platform.trainingdata.codeforces.app.warehouse.CodeforcesWarehouseRefreshService;
import com.custacm.platform.trainingdata.codeforces.domain.collector.CodeforcesSubmissionSourceClient;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesHandleAccountRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesSubmissionRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesWarehouseRefreshIntervalRepository;
import com.custacm.platform.trainingdata.codeforces.domain.parser.CodeforcesSubmissionParser;
import com.custacm.platform.trainingdata.codeforces.infra.collector.RestClientCodeforcesSubmissionSourceClient;
import com.custacm.platform.trainingdata.codeforces.infra.parser.JacksonCodeforcesSubmissionParser;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesHandleAccountRepository;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesSubmissionRepository;
import com.custacm.platform.trainingdata.codeforces.infra.repo.JdbcCodeforcesWarehouseRefreshIntervalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CodeforcesCollectorProperties.class)
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
    CodeforcesHandleAccountRepository codeforcesHandleAccountRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcCodeforcesHandleAccountRepository(jdbcTemplate);
    }

    @Bean
    CodeforcesWarehouseRefreshIntervalRepository codeforcesWarehouseRefreshIntervalRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcCodeforcesWarehouseRefreshIntervalRepository(jdbcTemplate);
    }

    @Bean
    CodeforcesSubmissionSourceClient codeforcesSubmissionSourceClient(
            CodeforcesCollectorProperties properties
    ) {
        return new RestClientCodeforcesSubmissionSourceClient(RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(codeforcesRequestFactory(properties))
                .build());
    }

    private static SimpleClientHttpRequestFactory codeforcesRequestFactory(CodeforcesCollectorProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }

    @Bean
    SqlTaskRunner sqlTaskRunner(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            ResourceLoader resourceLoader
    ) {
        return new SqlTaskRunner(jdbcTemplate, transactionManager, resourceLoader);
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
    CodeforcesSubmissionCollectionService codeforcesSubmissionCollectionService(
            CodeforcesHandleAccountService handleAccountService,
            CodeforcesSubmissionSourceClient sourceClient,
            CodeforcesOdsSubmissionIngestService ingestService,
            ObjectMapper objectMapper,
            CodeforcesCollectorProperties properties
    ) {
        return new CodeforcesSubmissionCollectionService(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                properties
        );
    }

    @Bean
    CodeforcesAcceptedSummaryQueryService codeforcesAcceptedSummaryQueryService(
            CodeforcesAcceptedSummaryRepository repository,
            CodeforcesHandleAccountService handleAccountService
    ) {
        return new CodeforcesAcceptedSummaryQueryService(repository, handleAccountService);
    }

    @Bean
    CodeforcesSubmissionQueryService codeforcesSubmissionQueryService(
            CodeforcesSubmissionRepository repository,
            CodeforcesHandleAccountService handleAccountService
    ) {
        return new CodeforcesSubmissionQueryService(repository, handleAccountService);
    }

    @Bean
    CodeforcesFirstAcceptedProblemQueryService codeforcesFirstAcceptedProblemQueryService(
            CodeforcesFirstAcceptedProblemRepository repository,
            CodeforcesHandleAccountService handleAccountService
    ) {
        return new CodeforcesFirstAcceptedProblemQueryService(repository, handleAccountService);
    }

    @Bean
    CodeforcesHandleAccountService codeforcesHandleAccountService(
            CodeforcesHandleAccountRepository repository
    ) {
        return new CodeforcesHandleAccountService(repository);
    }

    @Bean
    CodeforcesWarehouseRefreshService codeforcesWarehouseRefreshService(
            SqlTaskRunner sqlTaskRunner,
            CodeforcesWarehouseRefreshIntervalRepository intervalRepository
    ) {
        return new CodeforcesWarehouseRefreshService(sqlTaskRunner, intervalRepository);
    }
}
