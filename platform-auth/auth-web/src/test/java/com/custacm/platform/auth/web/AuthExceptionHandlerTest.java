package com.custacm.platform.auth.web;

import com.custacm.platform.auth.app.exception.AuthErrorCode;
import com.custacm.platform.auth.app.exception.AuthServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {
    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void mapsAuthServiceErrorsToHttpStatuses() {
        Map<AuthErrorCode, HttpStatus> statuses = Map.of(
                AuthErrorCode.AUTH_INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                AuthErrorCode.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                AuthErrorCode.AUTH_LOGIN_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS,
                AuthErrorCode.AUTH_USER_DISABLED, HttpStatus.FORBIDDEN,
                AuthErrorCode.AUTH_USER_EXISTS, HttpStatus.CONFLICT,
                AuthErrorCode.AUTH_USER_NOT_FOUND, HttpStatus.NOT_FOUND,
                AuthErrorCode.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN
        );

        statuses.forEach((code, status) -> {
            var response = handler.handleAuthServiceException(new AuthServiceException(code, "message"));
            assertThat(response.getStatusCode()).isEqualTo(status);
            assertThat(response.getBody()).isEqualTo(new ErrorResponse(code.name(), "message"));
        });
    }

    @Test
    void mapsIllegalArgumentAndAccessDenied() {
        assertThat(handler.handleIllegalArgumentException(new IllegalArgumentException("bad")).getBody())
                .isEqualTo(new ErrorResponse(AuthErrorCode.AUTH_INVALID_REQUEST.name(), "bad"));
        assertThat(handler.handleAccessDeniedException().getBody())
                .isEqualTo(new ErrorResponse(AuthErrorCode.AUTH_FORBIDDEN.name(), "forbidden"));
        assertThat(handler.handleAccessDeniedException().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
