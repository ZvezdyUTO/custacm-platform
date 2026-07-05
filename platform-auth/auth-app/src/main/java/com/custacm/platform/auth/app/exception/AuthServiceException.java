package com.custacm.platform.auth.app.exception;

public class AuthServiceException extends RuntimeException {
    private final AuthErrorCode errorCode;

    public AuthServiceException(AuthErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthErrorCode errorCode() {
        return errorCode;
    }
}
