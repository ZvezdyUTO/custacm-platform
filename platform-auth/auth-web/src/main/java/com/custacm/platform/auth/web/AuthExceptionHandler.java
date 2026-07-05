package com.custacm.platform.auth.web;

import com.custacm.platform.auth.app.exception.AuthErrorCode;
import com.custacm.platform.auth.app.exception.AuthServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<ErrorResponse> handleAuthServiceException(AuthServiceException ex) {
        return ResponseEntity.status(statusFor(ex.errorCode()))
                .body(new ErrorResponse(ex.errorCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(AuthErrorCode.AUTH_INVALID_REQUEST.name(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(AuthErrorCode.AUTH_FORBIDDEN.name(), "forbidden"));
    }

    private static HttpStatus statusFor(AuthErrorCode errorCode) {
        return switch (errorCode) {
            case AUTH_INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case AUTH_INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case AUTH_LOGIN_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case AUTH_USER_DISABLED, AUTH_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case AUTH_USER_EXISTS -> HttpStatus.CONFLICT;
            case AUTH_USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
    }
}
