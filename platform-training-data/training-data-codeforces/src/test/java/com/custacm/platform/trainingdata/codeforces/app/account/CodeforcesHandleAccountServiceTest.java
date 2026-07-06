package com.custacm.platform.trainingdata.codeforces.app.account;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesHandleAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeforcesHandleAccountServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-05T00:00:00Z");

    private final InMemoryCodeforcesHandleAccountRepository repository =
            new InMemoryCodeforcesHandleAccountRepository();
    private final CodeforcesHandleAccountService service = new CodeforcesHandleAccountService(
            repository,
            Clock.fixed(NOW, ZoneOffset.ofHours(8))
    );

    @Test
    void createsHandleAccount() {
        CodeforcesHandleAccount account = service.create("112487张三", "tourist");

        assertThat(account.studentIdentity()).isEqualTo("112487张三");
        assertThat(account.handle()).isEqualTo("tourist");
        assertThat(account.needCollect()).isTrue();
        assertThat(account.createdAt()).isEqualTo(NOW);
        assertThat(account.updatedAt()).isEqualTo(NOW);
        assertThat(service.getByStudentIdentity("112487张三")).isEqualTo(account);
        assertThat(service.getByHandle("tourist")).isEqualTo(account);
    }

    @Test
    void rejectsDuplicateIdentityOrHandle() {
        service.create("112487张三", "tourist");

        assertThatThrownBy(() -> service.create("112487张三", "Benq"))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS
                        ));
        assertThatThrownBy(() -> service.create("112488李四", "tourist"))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_HANDLE_EXISTS
                        ));
    }

    @Test
    void changesStudentIdentityWithoutChangingHandle() {
        service.create("112487张三", "tourist");

        CodeforcesHandleAccount changed = service.changeStudentIdentity("112487张三", "112488张三", null);

        assertThat(changed.studentIdentity()).isEqualTo("112488张三");
        assertThat(changed.handle()).isEqualTo("tourist");
        assertThat(changed.needCollect()).isTrue();
        assertThat(service.getByStudentIdentity("112488张三").handle()).isEqualTo("tourist");
        assertThatThrownBy(() -> service.getByStudentIdentity("112487张三"))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND
                        ));
        assertThatThrownBy(() -> service.getByHandle("missing"))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND
                        ));
    }

    @Test
    void changesNeedCollectWithoutChangingIdentity() {
        service.create("112487张三", "tourist");

        CodeforcesHandleAccount changed = service.changeStudentIdentity("112487张三", "112487张三", false);

        assertThat(changed.studentIdentity()).isEqualTo("112487张三");
        assertThat(changed.handle()).isEqualTo("tourist");
        assertThat(changed.needCollect()).isFalse();
        assertThat(changed.updatedAt()).isEqualTo(NOW);
        assertThat(service.getByStudentIdentity("112487张三").needCollect()).isFalse();
    }

    @Test
    void rejectsMissingOldIdentityOrConflictingNewIdentity() {
        service.create("112487张三", "tourist");
        service.create("112488李四", "Benq");

        assertThatThrownBy(() -> service.changeStudentIdentity("missing", "112489王五", null))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND
                        ));
        assertThatThrownBy(() -> service.changeStudentIdentity("112487张三", "112488李四", null))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS
                        ));
    }

    private static final class InMemoryCodeforcesHandleAccountRepository implements CodeforcesHandleAccountRepository {
        private final Map<String, CodeforcesHandleAccount> accountsByIdentity = new LinkedHashMap<>();

        @Override
        public List<CodeforcesHandleAccount> findAll() {
            return List.copyOf(accountsByIdentity.values());
        }

        @Override
        public Optional<CodeforcesHandleAccount> findByStudentIdentity(String studentIdentity) {
            return Optional.ofNullable(accountsByIdentity.get(studentIdentity));
        }

        @Override
        public Optional<CodeforcesHandleAccount> findByHandle(String handle) {
            return accountsByIdentity.values().stream()
                    .filter(account -> account.handle().equals(handle))
                    .findFirst();
        }

        @Override
        public CodeforcesHandleAccount save(CodeforcesHandleAccount account) {
            accountsByIdentity.put(account.studentIdentity(), account);
            return account;
        }

        @Override
        public CodeforcesHandleAccount updateStudentIdentityAndNeedCollect(
                String oldStudentIdentity,
                String newStudentIdentity,
                boolean needCollect,
                Instant updatedAt
        ) {
            CodeforcesHandleAccount existing = accountsByIdentity.remove(oldStudentIdentity);
            CodeforcesHandleAccount updated = new CodeforcesHandleAccount(
                    newStudentIdentity,
                    existing.handle(),
                    needCollect,
                    existing.createdAt(),
                    updatedAt
            );
            accountsByIdentity.put(newStudentIdentity, updated);
            return updated;
        }
    }
}
