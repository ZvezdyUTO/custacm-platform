package com.custacm.platform.trainingdata.common.infra.oj.repo.account;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcOjHandleAccountRepositoryTest {
    private static final Instant CREATED_AT = Instant.parse("2026-07-05T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-05T01:00:00Z");

    private JdbcTemplate jdbcTemplate;
    private JdbcOjHandleAccountRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:cf_handle_account_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcOjHandleAccountRepository(new NamedParameterJdbcTemplate(dataSource), new ObjectMapper());
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V014__create_codeforces_handle_account.sql")
            );
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V016__add_codeforces_handle_account_need_collect.sql")
            );
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V018__rename_oj_handle_account_and_store_handles_map.sql")
            );
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V021__add_oj_handle_account_collection_states.sql")
            );
        }
    }

    @Test
    void savesListsAndChangesUsernameWithoutChangingHandle() {
        repository.save(new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                false,
                Map.of(
                        OjNames.CODEFORCES,
                        new OjHandleCollectionState(true, Instant.parse("2026-07-04T00:00:00Z"))
                ),
                CREATED_AT,
                CREATED_AT
        ));

        OjHandleAccount listed = repository.findAll().get(0);

        assertThat(listed.username()).isEqualTo("112487张三");
        assertThat(listed.handles()).containsEntry(OjNames.CODEFORCES, "tourist");
        assertThat(listed.handles()).containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(listed.needCollect()).isFalse();
        assertThat(listed.collectionStates().get(OjNames.CODEFORCES).historyStartReached()).isTrue();
        assertThat(listed.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt())
                .isEqualTo(Instant.parse("2026-07-04T00:00:00Z"));
        assertThat(listed.collectionStates().get(OjNames.ATCODER).historyStartReached()).isFalse();

        OjHandleAccount changed = repository.updateUsernameAndNeedCollect(
                "112487张三",
                "112488张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                true,
                Map.of(
                        OjNames.CODEFORCES,
                        new OjHandleCollectionState(true, Instant.parse("2026-07-04T00:00:00Z")),
                        OjNames.ATCODER,
                        new OjHandleCollectionState(false, UPDATED_AT)
                ),
                UPDATED_AT
        );

        assertThat(changed.username()).isEqualTo("112488张三");
        assertThat(changed.handles()).containsEntry(OjNames.CODEFORCES, "tourist");
        assertThat(changed.handles()).containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(changed.needCollect()).isTrue();
        assertThat(changed.collectionStates().get(OjNames.CODEFORCES).historyStartReached()).isTrue();
        assertThat(changed.collectionStates().get(OjNames.ATCODER).lastCollectedAt()).isEqualTo(UPDATED_AT);
        assertThat(changed.createdAt()).isEqualTo(CREATED_AT);
        assertThat(changed.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(repository.findAll())
                .extracting(OjHandleAccount::username)
                .containsExactly("112488张三");
        assertThat(repository.findAll().get(0).handles())
                .containsEntry(OjNames.CODEFORCES, "tourist")
                .containsEntry(OjNames.ATCODER, "tourist_atcoder");
    }

    @Test
    void findsAllHandleAccountsOrderedByUsername() {
        repository.save(new OjHandleAccount(
                "112488李四",
                Map.of(OjNames.CODEFORCES, "Benq"),
                true,
                CREATED_AT,
                CREATED_AT
        ));
        repository.save(new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                true,
                CREATED_AT,
                CREATED_AT
        ));

        assertThat(repository.findAll())
                .extracting(OjHandleAccount::username)
                .containsExactly("112487张三", "112488李四");
        assertThat(repository.findAll())
                .extracting(account -> account.handles().get(OjNames.CODEFORCES))
                .containsExactly("tourist", "Benq");
    }

    @Test
    void findsHandleAccountByRequestedOjHandle() {
        repository.save(new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                true,
                CREATED_AT,
                CREATED_AT
        ));
        repository.save(new OjHandleAccount(
                "112488李四",
                Map.of(OjNames.ATCODER, "tourist"),
                true,
                CREATED_AT,
                CREATED_AT
        ));

        assertThat(repository.findByHandle("codeforces", "tourist"))
                .get()
                .extracting(OjHandleAccount::username)
                .isEqualTo("112487张三");
        assertThat(repository.findByHandle("atcoder", "tourist"))
                .get()
                .extracting(OjHandleAccount::username)
                .isEqualTo("112488李四");
        assertThat(repository.findByHandle(OjNames.ATCODER, "missing")).isEmpty();
    }

    @Test
    void updatesHandlesJsonWhenChangingAccount() {
        repository.save(new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist"),
                true,
                CREATED_AT,
                CREATED_AT
        ));

        OjHandleAccount changed = repository.updateUsernameAndNeedCollect(
                "112487张三",
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                true,
                Map.of(),
                UPDATED_AT
        );

        assertThat(changed.handles())
                .containsEntry(OjNames.CODEFORCES, "tourist")
                .containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(repository.findAll().get(0).handles())
                .containsEntry(OjNames.CODEFORCES, "tourist")
                .containsEntry(OjNames.ATCODER, "tourist_atcoder");
    }

    @Test
    void updatesCollectionStatesWithoutChangingIdentityHandlesOrNeedCollect() {
        repository.save(new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                false,
                CREATED_AT,
                CREATED_AT
        ));

        OjHandleAccount changed = repository.updateCollectionStates(
                "112487张三",
                Map.of(
                        OjNames.CODEFORCES,
                        new OjHandleCollectionState(true, Instant.parse("2026-07-04T00:00:00Z")),
                        OjNames.ATCODER,
                        new OjHandleCollectionState(false, UPDATED_AT)
                ),
                UPDATED_AT
        );

        assertThat(changed.username()).isEqualTo("112487张三");
        assertThat(changed.handles())
                .containsEntry(OjNames.CODEFORCES, "tourist")
                .containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(changed.needCollect()).isFalse();
        assertThat(changed.collectionStates().get(OjNames.CODEFORCES).historyStartReached()).isTrue();
        assertThat(changed.collectionStates().get(OjNames.ATCODER).lastCollectedAt()).isEqualTo(UPDATED_AT);
        assertThat(changed.createdAt()).isEqualTo(CREATED_AT);
        assertThat(changed.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void databaseDefaultsNeedCollectToTrueForExistingInsertShape() {
        jdbcTemplate.update("""
                insert into oj_handle_account (username, handles_json, created_at, updated_at)
                values ('112487张三', '{"CODEFORCES":"tourist"}', current_timestamp, current_timestamp)
                """);

        assertThat(repository.findAll().get(0).needCollect()).isTrue();
        assertThat(repository.findAll().get(0).collectionStates().get(OjNames.CODEFORCES).historyStartReached())
                .isFalse();
        assertThat(repository.findAll().get(0).collectionStates().get(OjNames.CODEFORCES).lastCollectedAt())
                .isNull();
    }

    @Test
    void databaseRejectsDuplicateIdentity() {
        repository.save(new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist"),
                true,
                CREATED_AT,
                CREATED_AT
        ));

        assertThatThrownBy(() -> repository.save(new OjHandleAccount(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "Benq"),
                true,
                CREATED_AT,
                CREATED_AT
        ))).isInstanceOf(DuplicateKeyException.class);

        Integer count = jdbcTemplate.queryForObject("select count(*) from oj_handle_account", Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
