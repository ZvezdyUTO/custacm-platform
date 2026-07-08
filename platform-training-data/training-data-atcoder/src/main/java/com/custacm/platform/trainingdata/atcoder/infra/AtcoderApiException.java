package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.common.collector.OjCollectionSourceFailure;

public class AtcoderApiException extends RuntimeException implements OjCollectionSourceFailure {
    private final ErrorCode errorCode;

    public AtcoderApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AtcoderApiException(ErrorCode errorCode, String message, Throwable cause) {
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
        ATCODER_API_RESPONSE_INVALID,
        ATCODER_API_REQUEST_FAILED
    }
}
