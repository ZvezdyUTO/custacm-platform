package com.custacm.platform.common.sqltask;

public class SqlTaskException extends RuntimeException {
    private final SqlTaskErrorCode errorCode;

    public SqlTaskException(SqlTaskErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SqlTaskException(SqlTaskErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public SqlTaskErrorCode errorCode() {
        return errorCode;
    }
}
