package com.custacm.platform.trainingdata.common.infra.oj.repo.warehouse;

import com.custacm.platform.trainingdata.common.domain.oj.repo.OjWarehouseDataPurgeRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class JdbcOjWarehouseDataPurgeRepository implements OjWarehouseDataPurgeRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcOjWarehouseDataPurgeRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public OjWarehouseDataPurgeCounts purgeAllByHandle(String ojName, String handle) {
        String normalizedOjName = requireText(ojName, "ojName");
        String normalizedHandle = requireText(handle, "handle");
        return transactionTemplate.execute(status -> purgeAllByHandleInTransaction(normalizedOjName, normalizedHandle));
    }

    private OjWarehouseDataPurgeCounts purgeAllByHandleInTransaction(String ojName, String handle) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("handle", handle);
        int dwsRows = deleteByHandleColumn(OjWarehouseTableNames.dwsHandleDailyRatingAcceptedSummary(ojName), parameters);
        int dwmRows = deleteByHandleColumn(OjWarehouseTableNames.dwmHandleProblemFirstAccepted(ojName), parameters);
        int dwdRows = deleteByHandleColumn(OjWarehouseTableNames.dwdSubmission(ojName), parameters);
        return new OjWarehouseDataPurgeCounts(dwdRows, dwmRows, dwsRows);
    }

    private int deleteByHandleColumn(String tableName, MapSqlParameterSource parameters) {
        return jdbcTemplate.update("delete from " + tableName + " where handle = :handle", parameters);
    }

}
