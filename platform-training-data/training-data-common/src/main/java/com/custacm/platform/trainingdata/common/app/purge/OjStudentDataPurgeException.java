package com.custacm.platform.trainingdata.common.app.purge;

public class OjStudentDataPurgeException extends RuntimeException {
    private final ErrorCode errorCode;

    public OjStudentDataPurgeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        OJ_STUDENT_DATA_PURGE_INVALID_REQUEST
    }
}
