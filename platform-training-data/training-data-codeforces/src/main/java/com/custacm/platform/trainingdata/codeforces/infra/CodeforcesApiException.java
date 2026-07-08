package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.common.collector.OjCollectionSourceFailure;

public class CodeforcesApiException extends RuntimeException implements OjCollectionSourceFailure {
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

    @Override
    public String collectorErrorCode() {
        return errorCode.name();
    }

    public enum ErrorCode {
        CODEFORCES_API_STATUS_FAILED,
        CODEFORCES_API_RESPONSE_INVALID,
        CODEFORCES_API_REQUEST_FAILED
    }
}
