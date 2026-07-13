package com.custacm.platform.trainingdata.common.infra.oj.repo.account;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Relational persistence for training members and their OJ bindings.
 *
 * @author huangbingrui.awa
 */
public class JdbcOjHandleAccountRepository implements OjHandleAccountRepository {
    private static final String ACCOUNT_SELECT = """
            select tm.username,
                   tm.need_collect,
                   tm.created_at as member_created_at,
                   tm.updated_at as member_updated_at,
                   ohb.oj_name,
                   ohb.handle,
                   ohb.last_collected_at,
                   ohb.updated_at as binding_updated_at
            from training_member tm
            left join oj_handle_binding ohb on ohb.username = tm.username
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcOjHandleAccountRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<OjHandleAccount> findAll() {
        return queryAccounts(
                ACCOUNT_SELECT + " order by tm.username, ohb.oj_name",
                new MapSqlParameterSource()
        );
    }

    @Override
    public Optional<OjHandleAccount> findByUsername(String username) {
        return queryAccounts(
                ACCOUNT_SELECT + " where tm.username = :username order by ohb.oj_name",
                new MapSqlParameterSource("username", username)
        ).stream().findFirst();
    }

    @Override
    public Optional<OjHandleAccount> findByHandle(String ojName, String handle) {
        return queryAccounts(
                ACCOUNT_SELECT + """
                        where tm.username = (
                            select requested.username
                            from oj_handle_binding requested
                            where requested.oj_name = :ojName
                              and requested.handle = :handle
                        )
                        order by ohb.oj_name
                        """,
                new MapSqlParameterSource()
                        .addValue("ojName", OjNames.normalize(ojName))
                        .addValue("handle", handle)
        ).stream().findFirst();
    }

    @Override
    public OjHandleAccount save(OjHandleAccount account) {
        return transactionTemplate.execute(status -> {
            jdbcTemplate.update("""
                            insert into training_member (
                                username, need_collect, created_at, updated_at
                            ) values (
                                :username, :needCollect, :createdAt, :updatedAt
                            )
                            """,
                    new MapSqlParameterSource()
                            .addValue("username", account.username())
                            .addValue("needCollect", account.needCollect())
                            .addValue("createdAt", timestamp(account.createdAt()))
                            .addValue("updatedAt", timestamp(account.updatedAt())));
            insertBindings(
                    account.username(),
                    account.handles(),
                    account.collectionStates(),
                    account.createdAt(),
                    account.updatedAt()
            );
            return account;
        });
    }

    @Override
    public OjHandleAccount replace(
            String username,
            Map<String, String> handles,
            boolean needCollect,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant updatedAt
    ) {
        return transactionTemplate.execute(status -> {
            int updated = jdbcTemplate.update("""
                            update training_member
                            set need_collect = :needCollect,
                                updated_at = :updatedAt
                            where username = :username
                            """,
                    new MapSqlParameterSource()
                            .addValue("username", username)
                            .addValue("needCollect", needCollect)
                            .addValue("updatedAt", timestamp(updatedAt)));
            requireSingleUpdate("OJ handle account", updated);
            synchronizeBindings(username, handles, collectionStates, updatedAt);
            return findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("updated OJ handle account not found"));
        });
    }

    @Override
    public boolean updateLastCollectedAtByHandle(
            String ojName,
            String handle,
            Instant lastCollectedAt,
            Instant updatedAt
    ) {
        int updated = jdbcTemplate.update("""
                        update oj_handle_binding
                        set last_collected_at = :lastCollectedAt,
                            updated_at = :updatedAt
                        where oj_name = :ojName
                          and handle = :handle
                        """,
                new MapSqlParameterSource()
                        .addValue("ojName", OjNames.normalize(ojName))
                        .addValue("handle", handle)
                        .addValue("lastCollectedAt", timestamp(lastCollectedAt))
                        .addValue("updatedAt", timestamp(updatedAt)));
        if (updated > 1) {
            throw new IllegalStateException("expected to update at most one OJ handle binding, updated=" + updated);
        }
        return updated == 1;
    }

    private List<OjHandleAccount> queryAccounts(String sql, MapSqlParameterSource parameters) {
        return jdbcTemplate.query(sql, parameters, resultSet -> {
            Map<String, MutableAccount> accounts = new LinkedHashMap<>();
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                MutableAccount account = accounts.computeIfAbsent(
                        username,
                        ignored -> mutableAccount(resultSet)
                );
                String ojName = resultSet.getString("oj_name");
                if (ojName != null) {
                    account.addBinding(
                            ojName,
                            resultSet.getString("handle"),
                            instantOrNull(resultSet, "last_collected_at"),
                            instantOrNull(resultSet, "binding_updated_at")
                    );
                }
            }
            return accounts.values().stream().map(MutableAccount::toAccount).toList();
        });
    }

    private static MutableAccount mutableAccount(ResultSet resultSet) {
        try {
            return new MutableAccount(
                    resultSet.getString("username"),
                    resultSet.getBoolean("need_collect"),
                    resultSet.getTimestamp("member_created_at").toInstant(),
                    resultSet.getTimestamp("member_updated_at").toInstant()
            );
        } catch (SQLException ex) {
            throw new IllegalStateException("failed to map OJ handle account", ex);
        }
    }

    private void synchronizeBindings(
            String username,
            Map<String, String> handles,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant updatedAt
    ) {
        List<String> existingOjNames = findOjNames(username);
        for (String existingOjName : existingOjNames) {
            if (!handles.containsKey(existingOjName)) {
                jdbcTemplate.update("""
                                delete from oj_handle_binding
                                where username = :username and oj_name = :ojName
                                """,
                        new MapSqlParameterSource()
                                .addValue("username", username)
                                .addValue("ojName", existingOjName));
            }
        }
        for (Map.Entry<String, String> handle : handles.entrySet()) {
            String ojName = OjNames.normalize(handle.getKey());
            MapSqlParameterSource parameters = bindingParameters(
                    username,
                    ojName,
                    handle.getValue(),
                    lastCollectedAt(collectionStates, ojName),
                    updatedAt
            );
            if (existingOjNames.contains(ojName)) {
                int updated = jdbcTemplate.update("""
                                update oj_handle_binding
                                set handle = :handle,
                                    last_collected_at = :lastCollectedAt,
                                    updated_at = :updatedAt
                                where username = :username and oj_name = :ojName
                                """,
                        parameters);
                requireSingleUpdate("OJ handle binding", updated);
            } else {
                jdbcTemplate.update("""
                                insert into oj_handle_binding (
                                    username, oj_name, handle, last_collected_at, created_at, updated_at
                                ) values (
                                    :username, :ojName, :handle, :lastCollectedAt, :updatedAt, :updatedAt
                                )
                                """,
                        parameters);
            }
        }
    }

    private void insertBindings(
            String username,
            Map<String, String> handles,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (handles.isEmpty()) {
            return;
        }
        List<MapSqlParameterSource> batch = new ArrayList<>();
        handles.forEach((ojName, handle) -> batch.add(
                bindingParameters(
                        username,
                        OjNames.normalize(ojName),
                        handle,
                        lastCollectedAt(collectionStates, ojName),
                        updatedAt
                ).addValue("createdAt", timestamp(createdAt))
        ));
        jdbcTemplate.batchUpdate("""
                        insert into oj_handle_binding (
                            username, oj_name, handle, last_collected_at, created_at, updated_at
                        ) values (
                            :username, :ojName, :handle, :lastCollectedAt, :createdAt, :updatedAt
                        )
                        """,
                batch.toArray(MapSqlParameterSource[]::new));
    }

    private List<String> findOjNames(String username) {
        return jdbcTemplate.queryForList("""
                        select oj_name
                        from oj_handle_binding
                        where username = :username
                        order by oj_name
                        """,
                new MapSqlParameterSource("username", username),
                String.class);
    }

    private static MapSqlParameterSource bindingParameters(
            String username,
            String ojName,
            String handle,
            Instant lastCollectedAt,
            Instant updatedAt
    ) {
        return new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("ojName", ojName)
                .addValue("handle", handle)
                .addValue("lastCollectedAt", timestampOrNull(lastCollectedAt))
                .addValue("updatedAt", timestamp(updatedAt));
    }

    private static Instant lastCollectedAt(
            Map<String, OjHandleCollectionState> collectionStates,
            String ojName
    ) {
        if (collectionStates == null) {
            return null;
        }
        OjHandleCollectionState state = collectionStates.get(OjNames.normalize(ojName));
        return state == null ? null : state.lastCollectedAt();
    }

    private static Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private static Timestamp timestampOrNull(Instant instant) {
        return instant == null ? null : timestamp(instant);
    }

    private static void requireSingleUpdate(String entityName, int updated) {
        if (updated != 1) {
            throw new IllegalStateException("expected to update one " + entityName + ", updated=" + updated);
        }
    }

    private static final class MutableAccount {
        private final String username;
        private final boolean needCollect;
        private final Instant createdAt;
        private final Map<String, String> handles = new LinkedHashMap<>();
        private final Map<String, OjHandleCollectionState> collectionStates = new LinkedHashMap<>();
        private Instant updatedAt;

        private MutableAccount(String username, boolean needCollect, Instant createdAt, Instant updatedAt) {
            this.username = username;
            this.needCollect = needCollect;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        private void addBinding(
                String ojName,
                String handle,
                Instant lastCollectedAt,
                Instant bindingUpdatedAt
        ) {
            handles.put(ojName, handle);
            collectionStates.put(ojName, new OjHandleCollectionState(lastCollectedAt));
            if (bindingUpdatedAt != null && bindingUpdatedAt.isAfter(updatedAt)) {
                updatedAt = bindingUpdatedAt;
            }
        }

        private OjHandleAccount toAccount() {
            return new OjHandleAccount(
                    username,
                    handles,
                    needCollect,
                    collectionStates,
                    createdAt,
                    updatedAt
            );
        }
    }
}
