package com.custacm.platform.auth.infra.jdbc;

import com.custacm.platform.auth.domain.model.UserAccount;
import com.custacm.platform.auth.domain.model.UserRole;
import com.custacm.platform.auth.domain.repo.UserAccountRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcUserAccountRepository implements UserAccountRepository {
    private static final RowMapper<UserAccount> ROW_MAPPER = (rs, rowNum) -> new UserAccount(
                rs.getString("student_identity"),
                rs.getString("password_hash"),
                UserRole.fromValue(rs.getString("role")),
                rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcUserAccountRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccount> findByStudentIdentity(String studentIdentity) {
        List<UserAccount> users = jdbcTemplate.query("""
                        select student_identity, password_hash, role, created_at, updated_at
                        from auth_user_account
                        where student_identity = :studentIdentity
                        """,
                new MapSqlParameterSource("studentIdentity", studentIdentity),
                ROW_MAPPER);
        return users.stream().findFirst();
    }

    @Override
    public List<UserAccount> findAll() {
        return jdbcTemplate.query("""
                        select student_identity, password_hash, role, created_at, updated_at
                        from auth_user_account
                        order by created_at asc, student_identity asc
                        """,
                ROW_MAPPER);
    }

    @Override
    public UserAccount save(UserAccount account) {
        jdbcTemplate.update("""
                        insert into auth_user_account (
                            student_identity, password_hash, role, created_at, updated_at
                        ) values (
                            :studentIdentity, :passwordHash, :role, :createdAt, :updatedAt
                        )
                        """,
                parameters(account));
        return account;
    }

    @Override
    public UserAccount update(UserAccount account) {
        int updated = jdbcTemplate.update("""
                        update auth_user_account
                        set password_hash = :passwordHash,
                            role = :role,
                            updated_at = :updatedAt
                        where student_identity = :studentIdentity
                        """,
                parameters(account));
        if (updated != 1) {
            throw new IllegalStateException("expected to update one auth user account, updated=" + updated);
        }
        return account;
    }

    @Override
    public void deleteByStudentIdentity(String studentIdentity) {
        jdbcTemplate.update("""
                        delete from auth_user_account
                        where student_identity = :studentIdentity
                        """,
                new MapSqlParameterSource("studentIdentity", studentIdentity));
    }

    @Override
    public long countByRole(UserRole role) {
        Long count = jdbcTemplate.queryForObject("""
                        select count(*)
                        from auth_user_account
                        where role = :role
                        """,
                new MapSqlParameterSource("role", role.value()),
                Long.class);
        return count == null ? 0 : count;
    }

    private static MapSqlParameterSource parameters(UserAccount account) {
        return new MapSqlParameterSource()
                .addValue("studentIdentity", account.studentIdentity())
                .addValue("passwordHash", account.passwordHash())
                .addValue("role", account.role().value())
                .addValue("createdAt", timestamp(account.createdAt()))
                .addValue("updatedAt", timestamp(account.updatedAt()));
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
