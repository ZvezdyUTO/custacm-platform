package com.custacm.platform.trainingdata.atcoder.config;

import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.atcoder.app.AtcoderOdsIngestService;
import com.custacm.platform.trainingdata.atcoder.app.AtcoderProblemListCollectionService;
import com.custacm.platform.trainingdata.atcoder.app.AtcoderSubmissionCollectionService;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModelWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmissionWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemModelPayloadParser;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemPayloadParser;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemSourceClient;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionPayloadParser;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import com.custacm.platform.trainingdata.atcoder.infra.JdbcAtcoderOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.atcoder.infra.JacksonAtcoderPayloadParser;
import com.custacm.platform.trainingdata.atcoder.infra.JdbcAtcoderOdsProblemModelWriter;
import com.custacm.platform.trainingdata.atcoder.infra.JdbcAtcoderOdsProblemWriter;
import com.custacm.platform.trainingdata.atcoder.infra.JdbcAtcoderOdsSubmissionWriter;
import com.custacm.platform.trainingdata.atcoder.infra.JdbcAtcoderWarehouseRefreshIntervalRepository;
import com.custacm.platform.trainingdata.atcoder.infra.RestClientAtcoderSourceClient;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import com.custacm.platform.trainingdata.common.collector.job.OjWarehouseRefreshHandler;
import com.custacm.platform.trainingdata.common.collector.job.SqlTaskOjWarehouseRefreshHandler;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseRefreshIntervalRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
        AtcoderCollectorProperties.class,
        AtcoderProblemListCollectorProperties.class
})
public class AtcoderTrainingDataConfig {
    @Bean
    JacksonAtcoderPayloadParser atcoderPayloadParser(ObjectMapper objectMapper) {
        return new JacksonAtcoderPayloadParser(objectMapper);
    }

    @Bean
    AtcoderOdsSubmissionWriter atcoderOdsSubmissionWriter(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderOdsSubmissionWriter(jdbcTemplate);
    }

    @Bean
    AtcoderOdsProblemWriter atcoderOdsProblemWriter(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderOdsProblemWriter(jdbcTemplate);
    }

    @Bean
    AtcoderOdsProblemModelWriter atcoderOdsProblemModelWriter(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderOdsProblemModelWriter(jdbcTemplate);
    }

    @Bean
    RestClientAtcoderSourceClient atcoderSourceClient(
            AtcoderCollectorProperties properties
    ) {
        return new RestClientAtcoderSourceClient(RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(atcoderRequestFactory(properties))
                .build());
    }

    private static SimpleClientHttpRequestFactory atcoderRequestFactory(AtcoderCollectorProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }

    @Bean
    AtcoderOdsIngestService atcoderOdsIngestService(
            AtcoderSubmissionPayloadParser submissionParser,
            AtcoderProblemPayloadParser problemParser,
            AtcoderProblemModelPayloadParser problemModelParser,
            AtcoderOdsSubmissionWriter submissionWriter,
            AtcoderOdsProblemWriter problemWriter,
            AtcoderOdsProblemModelWriter problemModelWriter,
            ObjectMapper objectMapper
    ) {
        return new AtcoderOdsIngestService(
                submissionParser,
                problemParser,
                problemModelParser,
                submissionWriter,
                problemWriter,
                problemModelWriter,
                objectMapper
        );
    }

    @Bean
    AtcoderSubmissionCollectionService atcoderSubmissionCollectionService(
            OjHandleAccountService handleAccountService,
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            AtcoderCollectorProperties properties
    ) {
        return new AtcoderSubmissionCollectionService(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                properties
        );
    }

    @Bean
    AtcoderProblemListCollectionService atcoderProblemListCollectionService(
            AtcoderProblemSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            AtcoderCollectorProperties properties
    ) {
        return new AtcoderProblemListCollectionService(sourceClient, ingestService, properties);
    }

    @Bean
    OjOdsDataPurgeRepository atcoderOdsDataPurgeRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderOdsDataPurgeRepository(jdbcTemplate);
    }

    @Bean
    OjWarehouseRefreshIntervalRepository atcoderWarehouseRefreshIntervalRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderWarehouseRefreshIntervalRepository(jdbcTemplate);
    }

    @Bean
    OjWarehouseRefreshService atcoderWarehouseRefreshService(
            SqlTaskRunner sqlTaskRunner,
            @Qualifier("atcoderWarehouseRefreshIntervalRepository")
            OjWarehouseRefreshIntervalRepository intervalRepository
    ) {
        return new OjWarehouseRefreshService(
                sqlTaskRunner,
                intervalRepository,
                "classpath:sql/tasks/atcoder-warehouse-refresh.yml",
                "batchId has no AtCoder submissions"
        );
    }

    @Bean
    OjWarehouseRefreshHandler atcoderWarehouseRefreshHandler(
            @Qualifier("atcoderWarehouseRefreshService")
            OjWarehouseRefreshService refreshService
    ) {
        return new SqlTaskOjWarehouseRefreshHandler(OjNames.ATCODER, refreshService);
    }
}
