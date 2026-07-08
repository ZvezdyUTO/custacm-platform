package com.custacm.platform.trainingdata.common.web.account;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.web.account.response.OjHandleAccountErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.custacm.platform.trainingdata.common.web")
public class OjHandleAccountExceptionHandler {
    @ExceptionHandler(OjHandleAccountException.class)
    public ResponseEntity<OjHandleAccountErrorResponse> handleOjHandleAccountException(
            OjHandleAccountException ex
    ) {
        return ResponseEntity.status(statusFor(ex.errorCode()))
                .body(new OjHandleAccountErrorResponse(ex.errorCode().name(), ex.getMessage()));
    }

    private static HttpStatus statusFor(OjHandleAccountException.ErrorCode errorCode) {
        return switch (errorCode) {
            case OJ_HANDLE_ACCOUNT_INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case OJ_HANDLE_ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS,
                 OJ_HANDLE_ACCOUNT_HANDLE_EXISTS ->
                    HttpStatus.CONFLICT;
        };
    }
}
