package com.custacm.platform.auth.app.service;

import com.custacm.platform.auth.app.exception.AuthErrorCode;
import com.custacm.platform.auth.app.exception.AuthServiceException;
import com.custacm.platform.auth.app.port.AccessTokenIssuer;
import com.custacm.platform.auth.app.port.PasswordHasher;
import com.custacm.platform.auth.app.result.LoginResult;
import com.custacm.platform.auth.domain.model.UserAccount;
import com.custacm.platform.auth.domain.repo.UserAccountRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AuthAccountService {
    private static final Duration LOGIN_FAILURE_COOLDOWN = Duration.ofSeconds(5);

    private final UserAccountRepository userAccounts;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;
    private final Map<String, Instant> loginBlockedUntilByIdentity = new ConcurrentHashMap<>();

    public AuthAccountService(
            UserAccountRepository userAccounts,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        this.userAccounts = userAccounts;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
    }

    public LoginResult login(String studentIdentity, String password) {
        return login(studentIdentity, password, null);
    }

    public LoginResult login(String studentIdentity, String password, Duration accessTokenTtl) {
        String normalizedIdentity = requireStudentIdentity(studentIdentity);
        ensureLoginAllowed(normalizedIdentity);
        UserAccount account = userAccounts.findByStudentIdentity(normalizedIdentity)
                .orElse(null);
        if (account == null || !passwordHasher.matches(password, account.passwordHash())) {
            recordLoginFailure(normalizedIdentity);
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_CREDENTIALS, "invalid student identity or password");
        }
        if (!account.role().canAuthenticate()) {
            recordLoginFailure(normalizedIdentity);
            throw new AuthServiceException(AuthErrorCode.AUTH_USER_DISABLED, "user is disabled");
        }
        recordLoginSuccess(normalizedIdentity);
        return new LoginResult(account, accessTokenIssuer.issue(account.studentIdentity(), account.role(), accessTokenTtl));
    }

    public UserAccount currentUser(String studentIdentity) {
        return userAccounts.findByStudentIdentity(requireStudentIdentity(studentIdentity))
                .filter(account -> account.role().canAuthenticate())
                .orElseThrow(() -> new AuthServiceException(AuthErrorCode.AUTH_USER_NOT_FOUND, "current user not found"));
    }

    public UserAccount changeOwnPassword(
            String actorStudentIdentity,
            String oldPassword,
            String newPassword,
            String confirmNewPassword
    ) {
        String normalizedIdentity = requireStudentIdentity(actorStudentIdentity);
        requireNewPasswordPresent(newPassword);
        requireMatchingPasswordConfirmation(newPassword, confirmNewPassword);
        UserAccount account = userAccounts.findByStudentIdentity(normalizedIdentity)
                .orElseThrow(() -> new AuthServiceException(AuthErrorCode.AUTH_USER_NOT_FOUND, "current user not found"));
        if (!account.role().canAuthenticate()) {
            throw new AuthServiceException(AuthErrorCode.AUTH_USER_DISABLED, "user is disabled");
        }
        if (!passwordHasher.matches(oldPassword, account.passwordHash())) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_CREDENTIALS, "old password is invalid");
        }
        return userAccounts.update(account.withPasswordHash(passwordHasher.hash(newPassword), clock.instant()));
    }

    static String requireStudentIdentity(String studentIdentity) {
        if (studentIdentity == null || studentIdentity.isBlank()) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "studentIdentity must not be blank");
        }
        return studentIdentity.trim();
    }

    private static void requireMatchingPasswordConfirmation(String newPassword, String confirmNewPassword) {
        if (!Objects.equals(newPassword, confirmNewPassword)) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "new password confirmation does not match");
        }
    }

    private static void requireNewPasswordPresent(String newPassword) {
        if (newPassword == null) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "newPassword must not be null");
        }
    }

    private void ensureLoginAllowed(String studentIdentity) {
        Instant blockedUntil = loginBlockedUntilByIdentity.get(studentIdentity);
        if (blockedUntil == null) {
            return;
        }
        Instant now = clock.instant();
        if (!now.isBefore(blockedUntil)) {
            loginBlockedUntilByIdentity.remove(studentIdentity, blockedUntil);
            return;
        }
        throw new AuthServiceException(
                AuthErrorCode.AUTH_LOGIN_RATE_LIMITED,
                "please wait before retrying login"
        );
    }

    private void recordLoginFailure(String studentIdentity) {
        loginBlockedUntilByIdentity.put(studentIdentity, clock.instant().plus(LOGIN_FAILURE_COOLDOWN));
    }

    private void recordLoginSuccess(String studentIdentity) {
        loginBlockedUntilByIdentity.remove(studentIdentity);
    }
}
