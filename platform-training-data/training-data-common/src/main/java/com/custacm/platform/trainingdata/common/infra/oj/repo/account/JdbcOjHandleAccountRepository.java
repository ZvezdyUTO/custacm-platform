package com.custacm.platform.trainingdata.common.infra.oj.repo.account;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcOjHandleAccountRepository implements OjHandleAccountRepository {
    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, CollectionStateJson>> COLLECTION_STATE_MAP_TYPE =
            new TypeReference<>() {
            };

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcOjHandleAccountRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<OjHandleAccount> findAll() {
        return jdbcTemplate.query("""
                        select student_identity, handles_json, need_collect, collection_states_json, created_at, updated_at
                        from oj_handle_account
                        order by student_identity
                        """,
                (rs, rowNum) -> toAccount(rs));
    }

    @Override
    public Optional<OjHandleAccount> findByStudentIdentity(String studentIdentity) {
        return jdbcTemplate.query("""
                        select student_identity, handles_json, need_collect, collection_states_json, created_at, updated_at
                        from oj_handle_account
                        where student_identity = :studentIdentity
                        """,
                new MapSqlParameterSource("studentIdentity", studentIdentity),
                (rs, rowNum) -> toAccount(rs)
        ).stream().findFirst();
    }

    @Override
    public Optional<OjHandleAccount> findByHandle(String ojName, String handle) {
        String normalizedOjName = OjNames.normalize(ojName);
        return jdbcTemplate.query("""
                        select student_identity, handles_json, need_collect, collection_states_json, created_at, updated_at
                        from oj_handle_account
                        where locate(:ojNameJson, handles_json) > 0
                          and locate(:handleJson, handles_json) > 0
                        order by student_identity
                        """,
                new MapSqlParameterSource()
                        .addValue("ojNameJson", jsonString(normalizedOjName))
                        .addValue("handleJson", jsonString(handle)),
                (rs, rowNum) -> toAccount(rs)
        ).stream()
                .filter(account -> handle.equals(account.handles().get(normalizedOjName)))
                .findFirst();
    }

    @Override
    public OjHandleAccount save(OjHandleAccount account) {
        jdbcTemplate.update("""
                        insert into oj_handle_account (
                            student_identity, handles_json, need_collect, collection_states_json, created_at, updated_at
                        ) values (
                            :studentIdentity, :handlesJson, :needCollect, :collectionStatesJson, :createdAt, :updatedAt
                        )
                        """,
                parameters(account));
        return account;
    }

    @Override
    public OjHandleAccount updateStudentIdentityAndNeedCollect(
            String oldStudentIdentity,
            String newStudentIdentity,
            Map<String, String> handles,
            boolean needCollect,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant updatedAt
    ) {
        int updated = jdbcTemplate.update("""
                        update oj_handle_account
                        set student_identity = :newStudentIdentity,
                            handles_json = :handlesJson,
                            need_collect = :needCollect,
                            collection_states_json = :collectionStatesJson,
                            updated_at = :updatedAt
                        where student_identity = :oldStudentIdentity
                        """,
                new MapSqlParameterSource()
                        .addValue("oldStudentIdentity", oldStudentIdentity)
                        .addValue("newStudentIdentity", newStudentIdentity)
                        .addValue("handlesJson", handlesJson(handles))
                        .addValue("needCollect", needCollect)
                        .addValue("collectionStatesJson", collectionStatesJson(collectionStates))
                        .addValue("updatedAt", timestamp(updatedAt)));
        if (updated != 1) {
            throw new IllegalStateException("expected to update one OJ handle account, updated=" + updated);
        }
        return findByStudentIdentity(newStudentIdentity)
                .orElseThrow(() -> new IllegalStateException("updated OJ handle account not found"));
    }

    @Override
    public OjHandleAccount updateCollectionStates(
            String studentIdentity,
            Map<String, OjHandleCollectionState> collectionStates,
            Instant updatedAt
    ) {
        int updated = jdbcTemplate.update("""
                        update oj_handle_account
                        set collection_states_json = :collectionStatesJson,
                            updated_at = :updatedAt
                        where student_identity = :studentIdentity
                        """,
                new MapSqlParameterSource()
                        .addValue("studentIdentity", studentIdentity)
                        .addValue("collectionStatesJson", collectionStatesJson(collectionStates))
                        .addValue("updatedAt", timestamp(updatedAt)));
        if (updated != 1) {
            throw new IllegalStateException("expected to update one OJ handle account, updated=" + updated);
        }
        return findByStudentIdentity(studentIdentity)
                .orElseThrow(() -> new IllegalStateException("updated OJ handle account not found"));
    }

    private MapSqlParameterSource parameters(OjHandleAccount account) {
        return new MapSqlParameterSource()
                .addValue("studentIdentity", account.studentIdentity())
                .addValue("handlesJson", handlesJson(account.handles()))
                .addValue("needCollect", account.needCollect())
                .addValue("collectionStatesJson", collectionStatesJson(account.collectionStates()))
                .addValue("createdAt", timestamp(account.createdAt()))
                .addValue("updatedAt", timestamp(account.updatedAt()));
    }

    private OjHandleAccount toAccount(ResultSet rs) throws SQLException {
        Map<String, String> handles = handles(rs.getString("handles_json"));
        return new OjHandleAccount(
                rs.getString("student_identity"),
                handles,
                rs.getBoolean("need_collect"),
                collectionStates(handles, rs.getString("collection_states_json")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    private Map<String, String> handles(String handlesJson) {
        try {
            return objectMapper.readValue(handlesJson, STRING_MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid OJ handles json", ex);
        }
    }

    private String handlesJson(Map<String, String> handles) {
        try {
            return objectMapper.writeValueAsString(handles);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize OJ handles", ex);
        }
    }

    private String jsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize OJ handle lookup value", ex);
        }
    }

    private Map<String, OjHandleCollectionState> collectionStates(
            Map<String, String> handles,
            String collectionStatesJson
    ) {
        if (collectionStatesJson == null || collectionStatesJson.isBlank()) {
            return OjHandleAccount.normalizeCollectionStates(handles, Map.of());
        }
        try {
            Map<String, CollectionStateJson> rawStates =
                    objectMapper.readValue(collectionStatesJson, COLLECTION_STATE_MAP_TYPE);
            Map<String, OjHandleCollectionState> states = new LinkedHashMap<>();
            rawStates.forEach((ojName, state) -> {
                if (state != null) {
                    states.put(ojName, new OjHandleCollectionState(
                            state.historyStartReached(),
                            instantOrNull(state.lastCollectedAt())
                    ));
                }
            });
            return OjHandleAccount.normalizeCollectionStates(handles, states);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid OJ collection states json", ex);
        }
    }

    private String collectionStatesJson(Map<String, OjHandleCollectionState> collectionStates) {
        try {
            Map<String, CollectionStateJson> rawStates = new LinkedHashMap<>();
            collectionStates.forEach((ojName, state) -> rawStates.put(
                    ojName,
                    new CollectionStateJson(
                            state.historyStartReached(),
                            state.lastCollectedAt() == null ? null : state.lastCollectedAt().toString()
                    )
            ));
            return objectMapper.writeValueAsString(rawStates);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize OJ collection states", ex);
        }
    }

    private static Instant instantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private record CollectionStateJson(
            boolean historyStartReached,
            String lastCollectedAt
    ) {
    }
}
