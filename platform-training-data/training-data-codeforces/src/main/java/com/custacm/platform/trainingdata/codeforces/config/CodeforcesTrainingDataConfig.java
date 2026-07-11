package com.custacm.platform.trainingdata.codeforces.config;

import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.common.app.account.TrainingUserDirectory;
import com.custacm.platform.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import com.custacm.platform.trainingdata.common.collector.job.OjWarehouseRefreshHandler;
import com.custacm.platform.trainingdata.common.collector.job.SqlTaskOjWarehouseRefreshHandler;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseRefreshIntervalRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.codeforces.app.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.codeforces.app.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesSubmissionSourceClient;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.codeforces.domain.SubmissionPayloadParser;
import com.custacm.platform.trainingdata.codeforces.infra.RestClientCodeforcesSubmissionSourceClient;
import com.custacm.platform.trainingdata.codeforces.infra.JacksonSubmissionPayloadParser;
import com.custacm.platform.trainingdata.codeforces.infra.JdbcCodeforcesOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.codeforces.infra.JdbcCodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.infra.JdbcCodeforcesWarehouseRefreshIntervalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CodeforcesCollectorProperties.class)
public class CodeforcesTrainingDataConfig {
    @Bean
    SubmissionPayloadParser submissionPayloadParser(ObjectMapper objectMapper) {
        return new JacksonSubmissionPayloadParser(objectMapper);
    }

    @Bean
    CodeforcesOdsSubmissionWriter codeforcesOdsSubmissionWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        return new JdbcCodeforcesOdsSubmissionWriter(jdbcTemplate);
    }

    @Bean
    OjWarehouseRefreshIntervalRepository codeforcesWarehouseRefreshIntervalRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcCodeforcesWarehouseRefreshIntervalRepository(jdbcTemplate);
    }

    @Bean
    CodeforcesOdsDataPurgeRepository codeforcesOdsDataPurgeRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcCodeforcesOdsDataPurgeRepository(jdbcTemplate);
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
    CodeforcesOdsSubmissionIngestService codeforcesOdsSubmissionIngestService(
            SubmissionPayloadParser parser,
            CodeforcesOdsSubmissionWriter writer,
            ObjectMapper objectMapper
    ) {
        return new CodeforcesOdsSubmissionIngestService(parser, writer, objectMapper);
    }

    @Bean
    CodeforcesSubmissionCollectionService codeforcesSubmissionCollectionService(
            TrainingUserDirectory handleAccountService,
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
    OjWarehouseRefreshService codeforcesWarehouseRefreshService(
            SqlTaskRunner sqlTaskRunner,
            @Qualifier("codeforcesWarehouseRefreshIntervalRepository")
            OjWarehouseRefreshIntervalRepository intervalRepository
    ) {
        return new OjWarehouseRefreshService(
                sqlTaskRunner,
                intervalRepository,
                "classpath:sql/tasks/codeforces-warehouse-refresh.yml",
                "batchId has no Codeforces submissions with creationTimeSeconds"
        );
    }

    @Bean
    OjWarehouseRefreshHandler codeforcesWarehouseRefreshHandler(
            @Qualifier("codeforcesWarehouseRefreshService")
            OjWarehouseRefreshService warehouseRefreshService
    ) {
        return new SqlTaskOjWarehouseRefreshHandler(OjNames.CODEFORCES, warehouseRefreshService);
    }
}
