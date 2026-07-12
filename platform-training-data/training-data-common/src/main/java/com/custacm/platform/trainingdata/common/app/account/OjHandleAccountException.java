package com.custacm.platform.trainingdata.common.app.account;

public class OjHandleAccountException extends RuntimeException {
    private final ErrorCode errorCode;

    public OjHandleAccountException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        OJ_HANDLE_ACCOUNT_INVALID_REQUEST,
        OJ_HANDLE_ACCOUNT_NOT_FOUND,
        OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS,
        OJ_HANDLE_ACCOUNT_HANDLE_EXISTS,
        OJ_HANDLE_ACCOUNT_REPLACEMENT_REQUIRES_PURGE
    }
}
