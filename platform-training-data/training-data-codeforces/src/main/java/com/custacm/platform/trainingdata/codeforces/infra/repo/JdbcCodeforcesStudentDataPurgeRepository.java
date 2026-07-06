package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesStudentDataPurgeResult;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesStudentDataPurgeRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

public class JdbcCodeforcesStudentDataPurgeRepository implements CodeforcesStudentDataPurgeRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcCodeforcesStudentDataPurgeRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public CodeforcesStudentDataPurgeResult purgeByStudentIdentity(String studentIdentity) {
        return transactionTemplate.execute(status -> purgeInTransaction(studentIdentity));
    }

    private CodeforcesStudentDataPurgeResult purgeInTransaction(String studentIdentity) {
        String handle = findHandle(studentIdentity);
        if (handle == null) {
            return new CodeforcesStudentDataPurgeResult(studentIdentity, null, 0, 0, 0, 0, 0);
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("studentIdentity", studentIdentity)
                .addValue("handle", handle);

        int dwsRows = deleteByHandle("dws_codeforces__handle_daily_rating_accepted_summary", parameters);
        int dwmRows = deleteByHandle("dwm_codeforces__handle_problem_first_accepted", parameters);
        int dwdRows = deleteByHandle("dwd_codeforces__submission", parameters);
        int odsRows = deleteByHandle("ods_codeforces__submission", parameters);
        int accountRows = jdbcTemplate.update("""
                        delete from codeforces_handle_account
                        where student_identity = :studentIdentity
                          and codeforces_handle = :handle
                        """,
                parameters);

        return new CodeforcesStudentDataPurgeResult(
                studentIdentity,
                handle,
                accountRows,
                odsRows,
                dwdRows,
                dwmRows,
                dwsRows
        );
    }

    private String findHandle(String studentIdentity) {
        List<String> handles = jdbcTemplate.queryForList("""
                        select codeforces_handle
                        from codeforces_handle_account
                        where student_identity = :studentIdentity
                        """,
                new MapSqlParameterSource("studentIdentity", studentIdentity),
                String.class);
        return handles.stream().findFirst().orElse(null);
    }

    private int deleteByHandle(String tableName, MapSqlParameterSource parameters) {
        return jdbcTemplate.update("delete from " + tableName + " where author_handle = :handle", parameters);
    }
}
