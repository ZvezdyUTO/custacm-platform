package com.custacm.platform.trainingdata.common.config;

import com.custacm.platform.common.sqltask.SqlTaskRunner;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeService;
import com.custacm.platform.trainingdata.common.app.query.OjAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.common.app.query.OjFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.common.app.query.OjSubmissionQueryService;
import com.custacm.platform.trainingdata.common.collector.config.OjCollectorSchedulingProperties;
import com.custacm.platform.trainingdata.common.collector.dispatch.OjRecentSubmissionCollector;
import com.custacm.platform.trainingdata.common.collector.dispatch.OjSubmissionCollectionDispatcher;
import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.custacm.platform.trainingdata.common.collector.job.OjWarehouseRefreshDispatcher;
import com.custacm.platform.trainingdata.common.collector.job.OjWarehouseRefreshHandler;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjSubmissionRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseDataPurgeRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.common.infra.oj.repo.account.JdbcOjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.infra.oj.repo.query.JdbcOjAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.common.infra.oj.repo.query.JdbcOjFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.common.infra.oj.repo.query.JdbcOjSubmissionRepository;
import com.custacm.platform.trainingdata.common.infra.oj.repo.warehouse.JdbcOjWarehouseDataPurgeRepository;
import com.custacm.platform.trainingdata.common.scheduler.OjScheduledSubmissionCollectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(OjCollectorSchedulingProperties.class)
public class CommonTrainingDataConfig {
    @Bean
    SqlTaskRunner sqlTaskRunner(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            ResourceLoader resourceLoader
    ) {
        return new SqlTaskRunner(jdbcTemplate, transactionManager, resourceLoader);
    }

    @Bean
    OjAcceptedSummaryRepository ojAcceptedSummaryRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcOjAcceptedSummaryRepository(jdbcTemplate);
    }

    @Bean
    OjSubmissionRepository ojSubmissionRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcOjSubmissionRepository(jdbcTemplate);
    }

    @Bean
    OjFirstAcceptedProblemRepository ojFirstAcceptedProblemRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcOjFirstAcceptedProblemRepository(jdbcTemplate);
    }

    @Bean
    OjHandleAccountRepository ojHandleAccountRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        return new JdbcOjHandleAccountRepository(jdbcTemplate, objectMapper);
    }

    @Bean
    OjWarehouseDataPurgeRepository ojWarehouseDataPurgeRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        return new JdbcOjWarehouseDataPurgeRepository(jdbcTemplate, transactionManager);
    }

    @Bean
    OjHandleAccountService ojHandleAccountService(
            OjHandleAccountRepository repository
    ) {
        return new OjHandleAccountService(repository, Clock.system(ZoneOffset.ofHours(8)));
    }

    @Bean
    OjAcceptedSummaryQueryService ojAcceptedSummaryQueryService(
            OjAcceptedSummaryRepository repository,
            OjHandleAccountService handleAccountService
    ) {
        return new OjAcceptedSummaryQueryService(repository, handleAccountService);
    }

    @Bean
    OjSubmissionQueryService ojSubmissionQueryService(
            OjSubmissionRepository repository,
            OjHandleAccountService handleAccountService
    ) {
        return new OjSubmissionQueryService(repository, handleAccountService);
    }

    @Bean
    OjFirstAcceptedProblemQueryService ojFirstAcceptedProblemQueryService(
            OjFirstAcceptedProblemRepository repository,
            OjHandleAccountService handleAccountService
    ) {
        return new OjFirstAcceptedProblemQueryService(repository, handleAccountService);
    }

    @Bean
    OjScheduledSubmissionCollectionService ojScheduledSubmissionCollectionService(
            List<OjRecentSubmissionCollector> collectors
    ) {
        return new OjSubmissionCollectionDispatcher(OjNames.CODEFORCES, collectors);
    }

    @Bean
    OjWarehouseRefreshDispatcher ojWarehouseRefreshDispatcher(
            List<OjWarehouseRefreshHandler> refreshHandlers
    ) {
        return new OjWarehouseRefreshDispatcher(refreshHandlers);
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService ojSubmissionCollectionJobExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "oj-submission-collection-job");
            thread.setDaemon(false);
            return thread;
        });
    }

    @Bean
    OjSubmissionCollectionJobService ojSubmissionCollectionJobService(
            OjScheduledSubmissionCollectionService collectionService,
            OjWarehouseRefreshDispatcher warehouseRefreshDispatcher,
            ExecutorService ojSubmissionCollectionJobExecutor,
            OjCollectorSchedulingProperties properties
    ) {
        return new OjSubmissionCollectionJobService(
                collectionService::collectRecentWindowForStudentIdentity,
                warehouseRefreshDispatcher,
                ojSubmissionCollectionJobExecutor,
                properties.jobItemInterval()
        );
    }

    @Bean
    OjStudentDataPurgeService ojStudentDataPurgeService(
            List<OjOdsDataPurgeRepository> odsDataPurgeRepositories,
            OjWarehouseDataPurgeRepository warehouseDataPurgeRepository,
            OjHandleAccountService handleAccountService,
            PlatformTransactionManager transactionManager
    ) {
        return new OjStudentDataPurgeService(
                odsDataPurgeRepositories,
                warehouseDataPurgeRepository,
                handleAccountService,
                new TransactionTemplate(transactionManager)
        );
    }
}
