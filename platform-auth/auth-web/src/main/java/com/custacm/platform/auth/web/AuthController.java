package com.custacm.platform.auth.web;

import com.custacm.platform.auth.app.exception.AuthErrorCode;
import com.custacm.platform.auth.app.exception.AuthServiceException;
import com.custacm.platform.auth.app.result.LoginResult;
import com.custacm.platform.auth.app.service.AuthAccountService;
import com.custacm.platform.auth.core.CurrentUser;
import com.custacm.platform.auth.core.CurrentUserExtractor;
import com.custacm.platform.auth.domain.model.UserAccount;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthAccountService authAccountService;
    private final AuthProperties properties;

    public AuthController(AuthAccountService authAccountService, AuthProperties properties) {
        this.authAccountService = authAccountService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request == null) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "request body must not be empty");
        }
        LoginResult result = authAccountService.login(
                request.studentIdentity(),
                request.password(),
                request.rememberMeEnabled()
                        ? properties.jwt().resolvedRememberMeAccessTokenTtl()
                        : properties.jwt().resolvedAccessTokenTtl()
        );
        return ResponseEntity.ok(new LoginResponse(
                "Bearer",
                result.token().tokenValue(),
                result.token().expiresInSeconds(),
                toCurrentUserResponse(result.user())
        ));
    }

    @GetMapping("/player/me")
    public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser currentUser = CurrentUserExtractor.from(jwt);
        UserAccount account = authAccountService.currentUser(currentUser.studentIdentity());
        return ResponseEntity.ok(toCurrentUserResponse(account));
    }

    @PatchMapping("/player/me/password")
    public ResponseEntity<Void> changeOwnPassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChangePasswordRequest request
    ) {
        if (request == null) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "request body must not be empty");
        }
        CurrentUser currentUser = CurrentUserExtractor.from(jwt);
        authAccountService.changeOwnPassword(
                currentUser.studentIdentity(),
                request.oldPassword(),
                request.newPassword(),
                request.confirmNewPassword()
        );
        return ResponseEntity.noContent().build();
    }

    private static CurrentUserResponse toCurrentUserResponse(UserAccount account) {
        return new CurrentUserResponse(
                account.studentIdentity(),
                account.role().value()
        );
    }
}
