package com.custacm.platform.trainingdata.common.infra.oj.repo.account;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author huangbingrui.awa
 */
class JdbcOjHandleAccountRepositoryTest {
    private static final Instant CREATED_AT = Instant.parse("2026-07-05T00:00:00Z");
    private static final Instant FIRST_COLLECTED_AT = Instant.parse("2026-07-04T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-05T01:00:00Z");

    private JdbcTemplate jdbcTemplate;
    private JdbcOjHandleAccountRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:oj_handle_account_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        createRelationalSchema();
        repository = new JdbcOjHandleAccountRepository(
                new NamedParameterJdbcTemplate(dataSource),
                new DataSourceTransactionManager(dataSource)
        );
    }

    @Test
    void savesListsAndReplacesCollectionSettings() {
        repository.save(account(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                false,
                Map.of(OjNames.CODEFORCES, new OjHandleCollectionState(FIRST_COLLECTED_AT))
        ));

        OjHandleAccount listed = repository.findAll().getFirst();

        assertThat(listed.username()).isEqualTo("112487张三");
        assertThat(listed.handles())
                .containsEntry(OjNames.CODEFORCES, "tourist")
                .containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(listed.needCollect()).isFalse();
        assertThat(listed.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt())
                .isEqualTo(FIRST_COLLECTED_AT);
        assertThat(listed.collectionStates().get(OjNames.ATCODER).lastCollectedAt()).isNull();

        OjHandleAccount changed = repository.replace(
                "112487张三",
                listed.handles(),
                true,
                Map.of(
                        OjNames.CODEFORCES, new OjHandleCollectionState(FIRST_COLLECTED_AT),
                        OjNames.ATCODER, new OjHandleCollectionState(UPDATED_AT)
                ),
                UPDATED_AT
        );

        assertThat(changed.username()).isEqualTo("112487张三");
        assertThat(changed.handles()).isEqualTo(listed.handles());
        assertThat(changed.needCollect()).isTrue();
        assertThat(changed.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt())
                .isEqualTo(FIRST_COLLECTED_AT);
        assertThat(changed.collectionStates().get(OjNames.ATCODER).lastCollectedAt())
                .isEqualTo(UPDATED_AT);
        assertThat(changed.createdAt()).isEqualTo(CREATED_AT);
        assertThat(changed.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(repository.findByUsername("112487张三")).contains(changed);
    }

    @Test
    void findsAllHandleAccountsOrderedByUsername() {
        repository.save(account("112488李四", Map.of(OjNames.CODEFORCES, "Benq"), true, Map.of()));
        repository.save(account(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                true,
                Map.of()
        ));

        assertThat(repository.findAll())
                .extracting(OjHandleAccount::username)
                .containsExactly("112487张三", "112488李四");
        assertThat(repository.findAll())
                .extracting(account -> account.handles().get(OjNames.CODEFORCES))
                .containsExactly("tourist", "Benq");
    }

    @Test
    void findsHandleAccountThroughTheIndexedOjBinding() {
        repository.save(account(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                true,
                Map.of()
        ));
        repository.save(account("112488李四", Map.of(OjNames.ATCODER, "tourist"), true, Map.of()));

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
    void synchronizesAddedUpdatedAndRemovedBindings() {
        repository.save(account(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "old_atcoder"),
                true,
                Map.of(
                        OjNames.CODEFORCES, new OjHandleCollectionState(FIRST_COLLECTED_AT),
                        OjNames.ATCODER, new OjHandleCollectionState(FIRST_COLLECTED_AT)
                )
        ));

        OjHandleAccount changed = repository.replace(
                "112487张三",
                Map.of(OjNames.ATCODER, "new_atcoder"),
                false,
                Map.of(OjNames.ATCODER, OjHandleCollectionState.empty()),
                UPDATED_AT
        );

        assertThat(changed.handles()).containsExactly(Map.entry(OjNames.ATCODER, "new_atcoder"));
        assertThat(changed.collectionStates().get(OjNames.ATCODER).lastCollectedAt()).isNull();
        assertThat(repository.findByHandle(OjNames.CODEFORCES, "tourist")).isEmpty();
        assertThat(repository.findByHandle(OjNames.ATCODER, "old_atcoder")).isEmpty();
    }

    @Test
    void atomicallyUpdatesOnlyTheRequestedHandleCursor() {
        repository.save(account(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                true,
                Map.of(
                        OjNames.CODEFORCES, new OjHandleCollectionState(FIRST_COLLECTED_AT),
                        OjNames.ATCODER, new OjHandleCollectionState(FIRST_COLLECTED_AT)
                )
        ));

        assertThat(repository.updateLastCollectedAtByHandle(
                "codeforces", "tourist", UPDATED_AT, UPDATED_AT)).isTrue();
        assertThat(repository.updateLastCollectedAtByHandle(
                OjNames.CODEFORCES, "missing", UPDATED_AT, UPDATED_AT)).isFalse();

        OjHandleAccount changed = repository.findByUsername("112487张三").orElseThrow();
        assertThat(changed.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt())
                .isEqualTo(UPDATED_AT);
        assertThat(changed.collectionStates().get(OjNames.ATCODER).lastCollectedAt())
                .isEqualTo(FIRST_COLLECTED_AT);
        assertThat(changed.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void databaseDefaultsNeedCollectForAMemberWithoutBindings() {
        jdbcTemplate.update("""
                        insert into training_member (username, created_at, updated_at)
                        values (?, ?, ?)
                        """,
                "112487张三", Timestamp.from(CREATED_AT), Timestamp.from(CREATED_AT));

        OjHandleAccount account = repository.findByUsername("112487张三").orElseThrow();
        assertThat(account.needCollect()).isTrue();
        assertThat(account.handles()).isEmpty();
        assertThat(account.collectionStates()).isEmpty();
    }

    @Test
    void databaseRejectsDuplicateIdentity() {
        repository.save(account("112487张三", Map.of(OjNames.CODEFORCES, "tourist"), true, Map.of()));

        assertThatThrownBy(() -> repository.save(account(
                "112487张三", Map.of(OjNames.CODEFORCES, "Benq"), true, Map.of()
        ))).isInstanceOf(DataIntegrityViolationException.class);

        Integer count = jdbcTemplate.queryForObject("select count(*) from training_member", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void databaseRejectsDuplicateHandleWithinTheSameOjAndRollsBackTheMember() {
        repository.save(account("112487张三", Map.of(OjNames.CODEFORCES, "tourist"), true, Map.of()));

        assertThatThrownBy(() -> repository.save(account(
                "112488李四", Map.of(OjNames.CODEFORCES, "tourist"), true, Map.of()
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(repository.findByUsername("112488李四")).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from training_member", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void sameHandleTextMayBelongToDifferentOjs() {
        repository.save(account("112487张三", Map.of(OjNames.CODEFORCES, "tourist"), true, Map.of()));
        repository.save(account("112488李四", Map.of(OjNames.ATCODER, "tourist"), true, Map.of()));

        assertThat(repository.findByHandle(OjNames.CODEFORCES, "tourist"))
                .get().extracting(OjHandleAccount::username).isEqualTo("112487张三");
        assertThat(repository.findByHandle(OjNames.ATCODER, "tourist"))
                .get().extracting(OjHandleAccount::username).isEqualTo("112488李四");
    }

    private OjHandleAccount account(
            String username,
            Map<String, String> handles,
            boolean needCollect,
            Map<String, OjHandleCollectionState> collectionStates
    ) {
        return new OjHandleAccount(
                username,
                handles,
                needCollect,
                collectionStates,
                CREATED_AT,
                CREATED_AT
        );
    }

    private void createRelationalSchema() {
        jdbcTemplate.execute("""
                create table training_member (
                    username varchar(128) not null,
                    need_collect boolean not null default true,
                    created_at timestamp(6) not null,
                    updated_at timestamp(6) not null,
                    primary key (username)
                )
                """);
        jdbcTemplate.execute("""
                create table oj_handle_binding (
                    username varchar(128) not null,
                    oj_name varchar(32) not null,
                    handle varchar(128) not null,
                    last_collected_at timestamp(6),
                    created_at timestamp(6) not null,
                    updated_at timestamp(6) not null,
                    primary key (username, oj_name),
                    unique (oj_name, handle),
                    constraint fk_binding_member foreign key (username)
                        references training_member (username)
                        on update cascade on delete cascade
                )
                """);
    }
}
