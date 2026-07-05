package com.custacm.platform.trainingdata.codeforces.infra.collector;

public class CodeforcesApiException extends RuntimeException {
    private final ErrorCode errorCode;

    public CodeforcesApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CodeforcesApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        CODEFORCES_API_STATUS_FAILED,
        CODEFORCES_API_RESPONSE_INVALID,
        CODEFORCES_API_REQUEST_FAILED
    }
}
