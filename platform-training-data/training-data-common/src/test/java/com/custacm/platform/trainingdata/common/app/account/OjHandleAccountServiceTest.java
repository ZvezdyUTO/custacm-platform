package com.custacm.platform.trainingdata.common.app.account;

import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjHandleAccountServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-05T00:00:00Z");

    private final InMemoryOjHandleAccountRepository repository =
            new InMemoryOjHandleAccountRepository();
    private final OjHandleAccountService service = new OjHandleAccountService(
            repository,
            Clock.fixed(NOW, ZoneOffset.ofHours(8))
    );

    @Test
    void createsHandleAccount() {
        OjHandleAccount account = service.create(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, " tourist_atcoder ")
        );

        assertThat(account.username()).isEqualTo("112487张三");
        assertThat(account.handles()).containsEntry(OjNames.CODEFORCES, "tourist");
        assertThat(account.handles()).containsEntry(OjNames.ATCODER, "tourist_atcoder");
        assertThat(account.needCollect()).isTrue();
        assertThat(account.createdAt()).isEqualTo(NOW);
        assertThat(account.updatedAt()).isEqualTo(NOW);
        assertThat(service.listAll()).containsExactly(account);
    }

    @Test
    void rejectsDuplicateIdentityOrHandle() {
        service.create("112487张三", Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"));

        assertThatThrownBy(() -> service.create("112487张三", Map.of(OjNames.CODEFORCES, "Benq")))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS
                        ));
        assertThatThrownBy(() -> service.create("112488李四", Map.of(OjNames.CODEFORCES, "tourist")))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS
                        ));
        assertThatThrownBy(() -> service.create("112489王五", Map.of(
                OjNames.CODEFORCES, "ecnerwala",
                OjNames.ATCODER, "tourist_atcoder"
        )))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS
                        ));
    }

    @Test
    void marksLatestSuccessfulCollectionWindowEndByOjHandle() {
        service.create("112487张三", Map.of(OjNames.CODEFORCES, "tourist"));

        service.markCollectedByHandle(
                OjNames.CODEFORCES,
                "tourist",
                Instant.parse("2026-07-04T00:00:00Z")
        );
        OjHandleAccount first = service.getByUsername("112487张三");
        service.markCollectedByHandle(
                OjNames.CODEFORCES,
                "tourist",
                Instant.parse("2026-07-05T00:00:00Z")
        );
        OjHandleAccount second = service.getByUsername("112487张三");

        assertThat(first.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt())
                .isEqualTo(Instant.parse("2026-07-04T00:00:00Z"));
        assertThat(second.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt())
                .isEqualTo(Instant.parse("2026-07-05T00:00:00Z"));
        assertThat(second.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void replacesCompleteHandleSetAfterPurgeAndResetsChangedState() {
        OjHandleAccount created = service.create("112487张三", Map.of(OjNames.CODEFORCES, "tourist"));
        repository.accountsByIdentity.put("112487张三", new OjHandleAccount(
                created.username(), created.handles(), created.needCollect(),
                Map.of(OjNames.CODEFORCES,
                        new OjHandleCollectionState(Instant.parse("2026-07-04T00:00:00Z"))),
                created.createdAt(), created.updatedAt()
        ));

        OjHandleAccount changed = service.replaceHandlesAfterPurge(
                "112487张三", Map.of(OjNames.ATCODER, "new-atcoder"), false);

        assertThat(changed.handles()).containsExactlyEntriesOf(Map.of(OjNames.ATCODER, "new-atcoder"));
        assertThat(changed.collectionStates().get(OjNames.ATCODER).lastCollectedAt()).isNull();
        assertThat(changed.needCollect()).isFalse();
    }

    @Test
    void rejectsConflictingUpdatedHandle() {
        service.create("112487张三", Map.of(OjNames.CODEFORCES, "tourist"));
        service.create("112488李四", Map.of(OjNames.ATCODER, "tourist_atcoder"));

        assertThatThrownBy(() -> service.replaceHandlesAfterPurge(
                "112487张三",
                Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                true))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_HANDLE_EXISTS
                        ));
    }

    private static final class InMemoryOjHandleAccountRepository implements OjHandleAccountRepository {
        private final Map<String, OjHandleAccount> accountsByIdentity = new LinkedHashMap<>();

        @Override
        public List<OjHandleAccount> findAll() {
            return List.copyOf(accountsByIdentity.values());
        }

        @Override
        public java.util.Optional<OjHandleAccount> findByUsername(String username) {
            return java.util.Optional.ofNullable(accountsByIdentity.get(username));
        }

        @Override
        public java.util.Optional<OjHandleAccount> findByHandle(String ojName, String handle) {
            return accountsByIdentity.values().stream()
                    .filter(account -> handle.equals(account.handles().get(OjNames.normalize(ojName))))
                    .findFirst();
        }

        @Override
        public OjHandleAccount save(OjHandleAccount account) {
            accountsByIdentity.put(account.username(), account);
            return account;
        }

        @Override
        public OjHandleAccount replace(
                String username,
                Map<String, String> handles,
                boolean needCollect,
                Map<String, OjHandleCollectionState> collectionStates,
                Instant updatedAt
        ) {
            OjHandleAccount existing = accountsByIdentity.get(username);
            OjHandleAccount updated = new OjHandleAccount(
                    username,
                    handles,
                    needCollect,
                    collectionStates,
                    existing.createdAt(),
                    updatedAt
            );
            accountsByIdentity.put(username, updated);
            return updated;
        }

        @Override
        public boolean updateLastCollectedAtByHandle(
                String ojName,
                String handle,
                Instant lastCollectedAt,
                Instant updatedAt
        ) {
            OjHandleAccount existing = findByHandle(ojName, handle).orElse(null);
            if (existing == null) {
                return false;
            }
            Map<String, OjHandleCollectionState> collectionStates =
                    new LinkedHashMap<>(existing.collectionStates());
            collectionStates.put(OjNames.normalize(ojName), new OjHandleCollectionState(lastCollectedAt));
            OjHandleAccount updated = new OjHandleAccount(
                    existing.username(),
                    existing.handles(),
                    existing.needCollect(),
                    collectionStates,
                    existing.createdAt(),
                    updatedAt
            );
            accountsByIdentity.put(existing.username(), updated);
            return true;
        }
    }
}
