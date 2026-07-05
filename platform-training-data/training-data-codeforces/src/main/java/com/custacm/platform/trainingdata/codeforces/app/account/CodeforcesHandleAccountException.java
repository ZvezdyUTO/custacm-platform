package com.custacm.platform.trainingdata.codeforces.app.account;

public class CodeforcesHandleAccountException extends RuntimeException {
    private final ErrorCode errorCode;

    public CodeforcesHandleAccountException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST,
        CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND,
        CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS,
        CODEFORCES_HANDLE_ACCOUNT_HANDLE_EXISTS
    }
}
