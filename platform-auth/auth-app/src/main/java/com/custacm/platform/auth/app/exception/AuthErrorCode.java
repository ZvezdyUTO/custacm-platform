package com.custacm.platform.auth.app.exception;

public enum AuthErrorCode {
    AUTH_INVALID_REQUEST,
    AUTH_INVALID_CREDENTIALS,
    AUTH_LOGIN_RATE_LIMITED,
    AUTH_USER_DISABLED,
    AUTH_USER_EXISTS,
    AUTH_USER_NOT_FOUND,
    AUTH_FORBIDDEN
}
