package com.custacm.platform.auth.app.service;

import com.custacm.platform.auth.app.exception.AuthErrorCode;
import com.custacm.platform.auth.app.exception.AuthServiceException;
import com.custacm.platform.auth.app.port.PasswordHasher;
import com.custacm.platform.auth.app.result.UserOperationResult;
import com.custacm.platform.auth.domain.model.UserAccount;
import com.custacm.platform.auth.domain.model.UserRole;
import com.custacm.platform.auth.domain.repo.UserAccountRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AdminUserService {
    private static final int GENERATED_PASSWORD_RANDOM_BYTES = 12;

    private final UserAccountRepository userAccounts;
    private final PasswordHasher passwordHasher;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AdminUserService(UserAccountRepository userAccounts, PasswordHasher passwordHasher, Clock clock) {
        this.userAccounts = userAccounts;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public UserOperationResult createUser(String studentIdentity, String password, UserRole role) {
        String normalizedIdentity = AuthAccountService.requireStudentIdentity(studentIdentity);
        String plainPassword = resolvePassword(password);
        UserRole accountRole = requireAccountRole(role);
        if (userAccounts.findByStudentIdentity(normalizedIdentity).isPresent()) {
            throw new AuthServiceException(AuthErrorCode.AUTH_USER_EXISTS, "user already exists");
        }
        Instant now = clock.instant();
        UserAccount account = userAccounts.save(new UserAccount(
                normalizedIdentity,
                passwordHasher.hash(plainPassword),
                accountRole,
                now,
                now
        ));
        return UserOperationResult.success(account, plainPassword, "user created");
    }

    public List<UserOperationResult> createUsers(List<CreateUserCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "users must not be empty");
        }
        List<UserOperationResult> results = new ArrayList<>();
        for (CreateUserCommand command : commands) {
            if (command == null) {
                results.add(UserOperationResult.failure(
                        null,
                        AuthErrorCode.AUTH_INVALID_REQUEST.name(),
                        "user must not be null"
                ));
                continue;
            }
            try {
                UserRole role = UserRole.fromValue(command.role());
                results.add(createUser(command.studentIdentity(), command.password(), role));
            } catch (AuthServiceException ex) {
                results.add(UserOperationResult.failure(command.studentIdentity(), ex.errorCode().name(), ex.getMessage()));
            } catch (IllegalArgumentException ex) {
                results.add(UserOperationResult.failure(
                        command.studentIdentity(),
                        AuthErrorCode.AUTH_INVALID_REQUEST.name(),
                        ex.getMessage()
                ));
            }
        }
        return List.copyOf(results);
    }

    public List<UserAccount> listUsers() {
        return userAccounts.findAll();
    }

    public UserOperationResult updateUser(
            String actorStudentIdentity,
            String targetStudentIdentity,
            UpdateUserCommand command
    ) {
        if (command == null) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "update request must not be empty");
        }
        String normalizedActor = AuthAccountService.requireStudentIdentity(actorStudentIdentity);
        String normalizedTarget = AuthAccountService.requireStudentIdentity(targetStudentIdentity);
        UserAccount account = userAccounts.findByStudentIdentity(normalizedTarget)
                .orElseThrow(() -> new AuthServiceException(AuthErrorCode.AUTH_USER_NOT_FOUND, "user not found"));
        boolean changesRole = command.role() != null;
        boolean changesPassword = command.newPassword() != null;
        if (!changesRole && !changesPassword) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "at least one user update field is required");
        }
        UserRole nextRole = changesRole ? requireAccountRole(command.role()) : account.role();
        rejectSelfDowngrade(normalizedActor, account, nextRole);
        UserAccount updated = account.withRole(nextRole, clock.instant());
        String plainPassword = null;
        if (changesPassword) {
            plainPassword = resolvePassword(command.newPassword());
            updated = updated.withPasswordHash(passwordHasher.hash(plainPassword), clock.instant());
        }
        return UserOperationResult.success(userAccounts.update(updated), plainPassword, "user updated");
    }

    public UserOperationResult deleteUser(String actorStudentIdentity, String targetStudentIdentity) {
        String normalizedActor = AuthAccountService.requireStudentIdentity(actorStudentIdentity);
        String normalizedTarget = AuthAccountService.requireStudentIdentity(targetStudentIdentity);
        UserAccount account = userAccounts.findByStudentIdentity(normalizedTarget)
                .orElseThrow(() -> new AuthServiceException(AuthErrorCode.AUTH_USER_NOT_FOUND, "user not found"));
        if (normalizedActor.equals(account.studentIdentity())) {
            throw new AuthServiceException(AuthErrorCode.AUTH_FORBIDDEN, "admin cannot delete self");
        }
        userAccounts.deleteByStudentIdentity(normalizedTarget);
        return UserOperationResult.success(account, null, "user deleted");
    }

    private static UserRole requireAccountRole(UserRole role) {
        if (role == null) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "role must not be blank");
        }
        return role;
    }

    private String resolvePassword(String password) {
        if (password != null && !password.isBlank()) {
            return password;
        }
        byte[] randomBytes = new byte[GENERATED_PASSWORD_RANDOM_BYTES];
        random.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static void rejectSelfDowngrade(
            String actorStudentIdentity,
            UserAccount target,
            UserRole nextRole
    ) {
        if (!actorStudentIdentity.equals(target.studentIdentity())) {
            return;
        }
        if (target.role() == UserRole.ADMIN && nextRole != UserRole.ADMIN) {
            throw new AuthServiceException(AuthErrorCode.AUTH_FORBIDDEN, "admin cannot downgrade self");
        }
    }

    public record CreateUserCommand(String studentIdentity, String password, String role) {
    }

    public record UpdateUserCommand(UserRole role, String newPassword) {
    }
}
