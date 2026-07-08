package com.custacm.platform.trainingdata.atcoder.config;

import com.custacm.platform.trainingdata.atcoder.app.AtcoderOdsBatchUpsertResult;
import com.custacm.platform.trainingdata.atcoder.app.AtcoderProblemListCollectionService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AtcoderProblemListBootstrapRunnerTest {
    @Test
    void collectsProblemListOnStartupWhenProblemTableIsEmpty() throws Exception {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        AtcoderProblemListCollectionService collectionService = mock(AtcoderProblemListCollectionService.class);
        when(jdbcOperations.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(0);
        when(collectionService.collectProblems()).thenReturn(result());

        runner(collectionService, jdbcOperations, true).run(null);

        verify(collectionService).collectProblems();
    }

    @Test
    void skipsStartupBootstrapWhenProblemTableAlreadyHasRows() throws Exception {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        AtcoderProblemListCollectionService collectionService = mock(AtcoderProblemListCollectionService.class);
        when(jdbcOperations.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(10);

        runner(collectionService, jdbcOperations, true).run(null);

        verify(collectionService, never()).collectProblems();
    }

    @Test
    void collectsProblemListWithoutCountingRowsWhenConfiguredToAlwaysBootstrap() throws Exception {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        AtcoderProblemListCollectionService collectionService = mock(AtcoderProblemListCollectionService.class);
        when(collectionService.collectProblems()).thenReturn(result());

        runner(collectionService, jdbcOperations, false).run(null);

        verifyNoInteractions(jdbcOperations);
        verify(collectionService).collectProblems();
    }

    @Test
    void skipsStartupBootstrapWhenDisabled() {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        AtcoderProblemListCollectionService collectionService = mock(AtcoderProblemListCollectionService.class);

        new AtcoderProblemListBootstrapRunner(
                new AtcoderProblemListCollectorProperties(
                        true,
                        false,
                        true,
                        null,
                        null
                ),
                collectionService,
                jdbcOperations
        ).run(null);

        verifyNoInteractions(jdbcOperations, collectionService);
    }

    private AtcoderProblemListBootstrapRunner runner(
            AtcoderProblemListCollectionService collectionService,
            NamedParameterJdbcOperations jdbcOperations,
            boolean bootstrapOnlyWhenEmpty
    ) {
        return new AtcoderProblemListBootstrapRunner(
                new AtcoderProblemListCollectorProperties(
                        true,
                        true,
                        bootstrapOnlyWhenEmpty,
                        null,
                        null
                ),
                collectionService,
                jdbcOperations
        );
    }

    private AtcoderOdsBatchUpsertResult result() {
        return new AtcoderOdsBatchUpsertResult(
                "collector-atcoder-problems-1",
                "ods_atcoder__problem",
                1,
                Instant.parse("2026-07-07T01:00:00Z")
        );
    }
}
