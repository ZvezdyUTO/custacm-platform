package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesHandleAccountRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcCodeforcesHandleAccountRepository implements CodeforcesHandleAccountRepository {
    private static final RowMapper<CodeforcesHandleAccount> ROW_MAPPER = (rs, rowNum) -> new CodeforcesHandleAccount(
            rs.getString("student_identity"),
            rs.getString("codeforces_handle"),
            rs.getBoolean("need_collect"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCodeforcesHandleAccountRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CodeforcesHandleAccount> findAll() {
        return jdbcTemplate.query("""
                        select student_identity, codeforces_handle, need_collect, created_at, updated_at
                        from codeforces_handle_account
                        order by student_identity
                        """,
                ROW_MAPPER);
    }

    @Override
    public Optional<CodeforcesHandleAccount> findByStudentIdentity(String studentIdentity) {
        List<CodeforcesHandleAccount> accounts = jdbcTemplate.query("""
                        select student_identity, codeforces_handle, need_collect, created_at, updated_at
                        from codeforces_handle_account
                        where student_identity = :studentIdentity
                        """,
                new MapSqlParameterSource("studentIdentity", studentIdentity),
                ROW_MAPPER);
        return accounts.stream().findFirst();
    }

    @Override
    public Optional<CodeforcesHandleAccount> findByHandle(String handle) {
        List<CodeforcesHandleAccount> accounts = jdbcTemplate.query("""
                        select student_identity, codeforces_handle, need_collect, created_at, updated_at
                        from codeforces_handle_account
                        where codeforces_handle = :handle
                        """,
                new MapSqlParameterSource("handle", handle),
                ROW_MAPPER);
        return accounts.stream().findFirst();
    }

    @Override
    public CodeforcesHandleAccount save(CodeforcesHandleAccount account) {
        jdbcTemplate.update("""
                        insert into codeforces_handle_account (
                            student_identity, codeforces_handle, need_collect, created_at, updated_at
                        ) values (
                            :studentIdentity, :handle, :needCollect, :createdAt, :updatedAt
                        )
                        """,
                parameters(account));
        return account;
    }

    @Override
    public CodeforcesHandleAccount updateStudentIdentityAndNeedCollect(
            String oldStudentIdentity,
            String newStudentIdentity,
            boolean needCollect,
            Instant updatedAt
    ) {
        int updated = jdbcTemplate.update("""
                        update codeforces_handle_account
                        set student_identity = :newStudentIdentity,
                            need_collect = :needCollect,
                            updated_at = :updatedAt
                        where student_identity = :oldStudentIdentity
                        """,
                new MapSqlParameterSource()
                        .addValue("oldStudentIdentity", oldStudentIdentity)
                        .addValue("newStudentIdentity", newStudentIdentity)
                        .addValue("needCollect", needCollect)
                        .addValue("updatedAt", timestamp(updatedAt)));
        if (updated != 1) {
            throw new IllegalStateException("expected to update one Codeforces handle account, updated=" + updated);
        }
        return findByStudentIdentity(newStudentIdentity)
                .orElseThrow(() -> new IllegalStateException("updated Codeforces handle account not found"));
    }

    private static MapSqlParameterSource parameters(CodeforcesHandleAccount account) {
        return new MapSqlParameterSource()
                .addValue("studentIdentity", account.studentIdentity())
                .addValue("handle", account.handle())
                .addValue("needCollect", account.needCollect())
                .addValue("createdAt", timestamp(account.createdAt()))
                .addValue("updatedAt", timestamp(account.updatedAt()));
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
