package com.custacm.platform.auth.app.service;

import com.custacm.platform.auth.app.exception.AuthErrorCode;
import com.custacm.platform.auth.app.exception.AuthServiceException;
import com.custacm.platform.auth.app.port.PasswordHasher;
import com.custacm.platform.auth.app.result.IssuedToken;
import com.custacm.platform.auth.domain.model.UserAccount;
import com.custacm.platform.auth.domain.model.UserRole;
import com.custacm.platform.auth.domain.repo.UserAccountRepository;
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

class AuthAccountServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-04T12:00:00Z"), ZoneOffset.UTC);

    private final MemoryUserAccountRepository users = new MemoryUserAccountRepository();
    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final AuthAccountService service = service(CLOCK);

    @Test
    void loginIssuesTokenForActiveUser() {
        users.save(account("230511213黄炳睿", "secret123", UserRole.PLAYER));

        var result = service.login("230511213黄炳睿", "secret123");

        assertThat(result.token().tokenValue()).isEqualTo("token-230511213黄炳睿-player");
        assertThat(result.user().role()).isEqualTo(UserRole.PLAYER);
    }

    @Test
    void loginRejectsInvalidPasswordAndStartsCooldown() {
        users.save(account("230511213黄炳睿", "secret123", UserRole.PLAYER));

        assertThatThrownBy(() -> service.login("230511213黄炳睿", "bad-password"))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_CREDENTIALS));
        assertThatThrownBy(() -> service.login("230511213黄炳睿", "secret123"))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_LOGIN_RATE_LIMITED));
    }

    @Test
    void loginRejectsDisabledRole() {
        users.save(account("230511213黄炳睿", "secret123", UserRole.DISABLE));

        assertThatThrownBy(() -> service.login("230511213黄炳睿", "secret123"))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_USER_DISABLED));
    }

    @Test
    void loginAllowsRetryAfterCooldownExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-04T12:00:00Z"));
        AuthAccountService service = service(clock);
        users.save(account("230511213黄炳睿", "secret123", UserRole.PLAYER));

        assertThatThrownBy(() -> service.login("230511213黄炳睿", "bad-password"))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_CREDENTIALS));

        clock.now = clock.now.plusSeconds(4);
        assertThatThrownBy(() -> service.login("230511213黄炳睿", "secret123"))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_LOGIN_RATE_LIMITED));

        clock.now = clock.now.plusSeconds(1);
        assertThat(service.login("230511213黄炳睿", "secret123").token().tokenValue())
                .isEqualTo("token-230511213黄炳睿-player");
    }

    @Test
    void changeOwnPasswordRequiresOldPasswordAndStoresNewHash() {
        users.save(account("230511213黄炳睿", "secret123", UserRole.PLAYER));

        service.changeOwnPassword("230511213黄炳睿", "secret123", "1", "1");

        assertThat(users.findByStudentIdentity("230511213黄炳睿"))
                .get()
                .extracting(UserAccount::passwordHash)
                .isEqualTo("hash:1");
    }

    @Test
    void changeOwnPasswordRejectsWrongOldPassword() {
        users.save(account("230511213黄炳睿", "secret123", UserRole.PLAYER));

        assertThatThrownBy(() -> service.changeOwnPassword("230511213黄炳睿", "bad-password", "newSecret123", "newSecret123"))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_CREDENTIALS));
    }

    @Test
    void changeOwnPasswordRejectsMismatchedConfirmation() {
        users.save(account("230511213黄炳睿", "secret123", UserRole.PLAYER));

        assertThatThrownBy(() -> service.changeOwnPassword("230511213黄炳睿", "secret123", "newSecret123", "differentSecret123"))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_REQUEST));
        assertThat(users.findByStudentIdentity("230511213黄炳睿"))
                .get()
                .extracting(UserAccount::passwordHash)
                .isEqualTo("hash:secret123");
    }

    private UserAccount account(String studentIdentity, String password, UserRole role) {
        return new UserAccount(studentIdentity, passwordHasher.hash(password), role, CLOCK.instant(), CLOCK.instant());
    }

    private AuthAccountService service(Clock clock) {
        return new AuthAccountService(
                users,
                passwordHasher,
                (studentIdentity, role) -> new IssuedToken("token-" + studentIdentity + "-" + role.value(), 7200),
                clock
        );
    }

    private static class FakePasswordHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hash:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return ("hash:" + rawPassword).equals(passwordHash);
        }
    }

    private static class MemoryUserAccountRepository implements UserAccountRepository {
        private final Map<String, UserAccount> accounts = new LinkedHashMap<>();

        @Override
        public Optional<UserAccount> findByStudentIdentity(String studentIdentity) {
            return Optional.ofNullable(accounts.get(studentIdentity));
        }

        @Override
        public List<UserAccount> findAll() {
            return List.copyOf(accounts.values());
        }

        @Override
        public UserAccount save(UserAccount account) {
            accounts.put(account.studentIdentity(), account);
            return account;
        }

        @Override
        public UserAccount update(UserAccount account) {
            accounts.put(account.studentIdentity(), account);
            return account;
        }

        @Override
        public void deleteByStudentIdentity(String studentIdentity) {
            accounts.remove(studentIdentity);
        }

        @Override
        public long countByRole(UserRole role) {
            return accounts.values().stream().filter(account -> account.role() == role).count();
        }
    }

    private static class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
