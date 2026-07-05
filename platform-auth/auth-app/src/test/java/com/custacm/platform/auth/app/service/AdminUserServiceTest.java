package com.custacm.platform.auth.app.service;

import com.custacm.platform.auth.app.exception.AuthErrorCode;
import com.custacm.platform.auth.app.exception.AuthServiceException;
import com.custacm.platform.auth.app.port.PasswordHasher;
import com.custacm.platform.auth.app.result.UserOperationResult;
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

class AdminUserServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-04T12:00:00Z"), ZoneOffset.UTC);

    private final MemoryUserAccountRepository users = new MemoryUserAccountRepository();
    private final FakePasswordHasher passwordHasher = new FakePasswordHasher();
    private final AdminUserService service = new AdminUserService(users, passwordHasher, CLOCK);

    @Test
    void createUserStoresHashedPasswordAndPlayerRole() {
        UserOperationResult result = service.createUser("230511213黄炳睿", "1", UserRole.PLAYER);

        UserAccount account = result.account();
        assertThat(result.success()).isTrue();
        assertThat(result.plainPassword()).isEqualTo("1");
        assertThat(account.studentIdentity()).isEqualTo("230511213黄炳睿");
        assertThat(account.passwordHash()).isEqualTo("hash:1");
        assertThat(account.role()).isEqualTo(UserRole.PLAYER);
        assertThat(account.role().canAuthenticate()).isTrue();
    }

    @Test
    void createUserGeneratesPasswordWhenPasswordIsBlank() {
        UserOperationResult result = service.createUser("230511213黄炳睿", "", UserRole.PLAYER);

        assertThat(result.success()).isTrue();
        assertThat(result.plainPassword()).hasSize(16);
        assertThat(result.account().passwordHash()).isEqualTo("hash:" + result.plainPassword());
    }

    @Test
    void batchCreateReturnsPerUserResults() {
        users.save(account("existing", "oldPass", UserRole.PLAYER));

        List<UserOperationResult> results = service.createUsers(List.of(
                new AdminUserService.CreateUserCommand("230511213黄炳睿", "", "player"),
                new AdminUserService.CreateUserCommand("existing", "rootPass123", "admin"),
                new AdminUserService.CreateUserCommand("bad-role", "badPass", "guest")
        ));

        assertThat(results).hasSize(3);
        assertThat(results.get(0).success()).isTrue();
        assertThat(results.get(0).studentIdentity()).isEqualTo("230511213黄炳睿");
        assertThat(results.get(0).plainPassword()).hasSize(16);
        assertThat(results.get(1).success()).isFalse();
        assertThat(results.get(1).errorCode()).isEqualTo(AuthErrorCode.AUTH_USER_EXISTS.name());
        assertThat(results.get(2).success()).isFalse();
        assertThat(results.get(2).errorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_REQUEST.name());
    }

    @Test
    void adminCannotDowngradeSelf() {
        users.save(account("root", "rootPass123", UserRole.ADMIN));

        assertThatThrownBy(() -> service.updateUser(
                "root",
                "root",
                new AdminUserService.UpdateUserCommand(UserRole.PLAYER, null)
        ))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_FORBIDDEN));
        assertThatThrownBy(() -> service.updateUser(
                "root",
                "root",
                new AdminUserService.UpdateUserCommand(UserRole.DISABLE, null)
        ))
                .isInstanceOfSatisfying(AuthServiceException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(AuthErrorCode.AUTH_FORBIDDEN));
    }

    @Test
    void adminCanDisableAndDeleteOtherUser() {
        users.save(account("root", "rootPass123", UserRole.ADMIN));
        users.save(account("230511213黄炳睿", "secret123", UserRole.PLAYER));

        UserOperationResult disabled = service.updateUser(
                "root",
                "230511213黄炳睿",
                new AdminUserService.UpdateUserCommand(UserRole.DISABLE, null)
        );
        UserOperationResult deleted = service.deleteUser("root", "230511213黄炳睿");

        assertThat(disabled.account().role()).isEqualTo(UserRole.DISABLE);
        assertThat(disabled.account().role().canAuthenticate()).isFalse();
        assertThat(deleted.success()).isTrue();
        assertThat(deleted.studentIdentity()).isEqualTo("230511213黄炳睿");
        assertThat(users.findByStudentIdentity("230511213黄炳睿")).isEmpty();
    }

    @Test
    void updateUserCanResetPasswordThroughSameUseCase() {
        users.save(account("root", "rootPass123", UserRole.ADMIN));
        users.save(account("230511213黄炳睿", "secret123", UserRole.PLAYER));

        UserOperationResult updated = service.updateUser(
                "root",
                "230511213黄炳睿",
                new AdminUserService.UpdateUserCommand(UserRole.ADMIN, "")
        );

        assertThat(updated.success()).isTrue();
        assertThat(updated.account().role()).isEqualTo(UserRole.ADMIN);
        assertThat(updated.plainPassword()).hasSize(16);
        assertThat(updated.account().passwordHash()).isEqualTo("hash:" + updated.plainPassword());
    }

    private UserAccount account(String studentIdentity, String password, UserRole role) {
        return new UserAccount(studentIdentity, passwordHasher.hash(password), role, CLOCK.instant(), CLOCK.instant());
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
}
