package com.custacm.platform.trainingdata.atcoder.config;

import com.custacm.platform.trainingdata.atcoder.app.AtcoderProblemListCollectionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "platform.training-data.atcoder.problem-list-collector",
        name = "bootstrap-on-startup",
        havingValue = "true",
        matchIfMissing = true
)
public class AtcoderProblemListBootstrapRunner implements ApplicationRunner {
    private static final String PROBLEM_LIST_BOOTSTRAP_FAILED_ERROR_CODE =
            "ATCODER_PROBLEM_LIST_BOOTSTRAP_FAILED";
    private static final String PROBLEM_COUNT_SQL = "select count(*) from ods_atcoder__problem";
    private static final Logger log = LoggerFactory.getLogger(AtcoderProblemListBootstrapRunner.class);

    private final AtcoderProblemListCollectorProperties properties;
    private final AtcoderProblemListCollectionService collectionService;
    private final NamedParameterJdbcOperations jdbcOperations;

    public AtcoderProblemListBootstrapRunner(
            AtcoderProblemListCollectorProperties properties,
            AtcoderProblemListCollectionService collectionService,
            NamedParameterJdbcOperations jdbcOperations
    ) {
        this.properties = properties;
        this.collectionService = collectionService;
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!properties.bootstrapOnStartup()) {
                log.info("AtCoder problem-list startup bootstrap is disabled");
                return;
            }
            if (properties.bootstrapOnlyWhenEmpty() && existingProblemCount() > 0) {
                log.info("AtCoder problem-list startup bootstrap skipped because ODS problem table is not empty");
                return;
            }
            var result = collectionService.collectProblems();
            log.info(
                    "AtCoder problem-list startup bootstrap finished, batchId={}, writtenRows={}",
                    result.batchId(),
                    result.writtenRows()
            );
        } catch (JsonProcessingException ex) {
            log.error("AtCoder problem-list startup bootstrap failed, errorCode={}",
                    PROBLEM_LIST_BOOTSTRAP_FAILED_ERROR_CODE, ex);
        } catch (RuntimeException ex) {
            log.error("AtCoder problem-list startup bootstrap failed, errorCode={}",
                    PROBLEM_LIST_BOOTSTRAP_FAILED_ERROR_CODE, ex);
        }
    }

    private int existingProblemCount() {
        Integer count = jdbcOperations.queryForObject(PROBLEM_COUNT_SQL, Map.of(), Integer.class);
        return count == null ? 0 : count;
    }
}
