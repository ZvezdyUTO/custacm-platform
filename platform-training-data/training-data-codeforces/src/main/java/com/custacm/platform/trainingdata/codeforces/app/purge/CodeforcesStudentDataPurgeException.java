package com.custacm.platform.trainingdata.codeforces.app.purge;

public class CodeforcesStudentDataPurgeException extends RuntimeException {
    private final ErrorCode errorCode;

    public CodeforcesStudentDataPurgeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        CODEFORCES_STUDENT_DATA_PURGE_INVALID_REQUEST
    }
}
