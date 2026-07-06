package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcCodeforcesHandleAccountRepositoryTest {
    private static final Instant CREATED_AT = Instant.parse("2026-07-05T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-05T01:00:00Z");

    private JdbcTemplate jdbcTemplate;
    private JdbcCodeforcesHandleAccountRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:cf_handle_account_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcCodeforcesHandleAccountRepository(new NamedParameterJdbcTemplate(dataSource));
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V014__create_codeforces_handle_account.sql")
            );
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V016__add_codeforces_handle_account_need_collect.sql")
            );
        }
    }

    @Test
    void savesFindsAndChangesStudentIdentityWithoutChangingHandle() {
        repository.save(new CodeforcesHandleAccount("112487张三", "tourist", false, CREATED_AT, CREATED_AT));

        CodeforcesHandleAccount byIdentity = repository.findByStudentIdentity("112487张三").orElseThrow();
        CodeforcesHandleAccount byHandle = repository.findByHandle("tourist").orElseThrow();

        assertThat(byIdentity.handle()).isEqualTo("tourist");
        assertThat(byIdentity.needCollect()).isFalse();
        assertThat(byHandle.studentIdentity()).isEqualTo("112487张三");
        assertThat(byHandle.needCollect()).isFalse();

        CodeforcesHandleAccount changed = repository.updateStudentIdentityAndNeedCollect(
                "112487张三",
                "112488张三",
                true,
                UPDATED_AT
        );

        assertThat(changed.studentIdentity()).isEqualTo("112488张三");
        assertThat(changed.handle()).isEqualTo("tourist");
        assertThat(changed.needCollect()).isTrue();
        assertThat(changed.createdAt()).isEqualTo(CREATED_AT);
        assertThat(changed.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(repository.findByStudentIdentity("112487张三")).isEmpty();
        assertThat(repository.findByHandle("tourist").orElseThrow().studentIdentity()).isEqualTo("112488张三");
    }

    @Test
    void findsAllHandleAccountsOrderedByStudentIdentity() {
        repository.save(new CodeforcesHandleAccount("112488李四", "Benq", CREATED_AT, CREATED_AT));
        repository.save(new CodeforcesHandleAccount("112487张三", "tourist", CREATED_AT, CREATED_AT));

        assertThat(repository.findAll())
                .extracting(CodeforcesHandleAccount::studentIdentity)
                .containsExactly("112487张三", "112488李四");
        assertThat(repository.findAll())
                .extracting(CodeforcesHandleAccount::handle)
                .containsExactly("tourist", "Benq");
    }

    @Test
    void databaseDefaultsNeedCollectToTrueForExistingInsertShape() {
        jdbcTemplate.update("""
                insert into codeforces_handle_account (student_identity, codeforces_handle, created_at, updated_at)
                values ('112487张三', 'tourist', current_timestamp, current_timestamp)
                """);

        assertThat(repository.findByStudentIdentity("112487张三").orElseThrow().needCollect()).isTrue();
    }

    @Test
    void databaseRejectsDuplicateIdentityAndHandle() {
        repository.save(new CodeforcesHandleAccount("112487张三", "tourist", CREATED_AT, CREATED_AT));

        assertThatThrownBy(() -> repository.save(new CodeforcesHandleAccount(
                "112487张三",
                "Benq",
                CREATED_AT,
                CREATED_AT
        ))).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> repository.save(new CodeforcesHandleAccount(
                "112488李四",
                "tourist",
                CREATED_AT,
                CREATED_AT
        ))).isInstanceOf(DuplicateKeyException.class);

        Integer count = jdbcTemplate.queryForObject("select count(*) from codeforces_handle_account", Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
