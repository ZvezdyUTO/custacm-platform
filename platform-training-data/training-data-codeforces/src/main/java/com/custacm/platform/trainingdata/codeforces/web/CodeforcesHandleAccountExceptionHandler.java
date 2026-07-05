package com.custacm.platform.trainingdata.codeforces.web;

import com.custacm.platform.trainingdata.codeforces.app.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.web.response.CodeforcesHandleAccountErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.custacm.platform.trainingdata.codeforces.web")
public class CodeforcesHandleAccountExceptionHandler {
    @ExceptionHandler(CodeforcesHandleAccountException.class)
    public ResponseEntity<CodeforcesHandleAccountErrorResponse> handleCodeforcesHandleAccountException(
            CodeforcesHandleAccountException ex
    ) {
        return ResponseEntity.status(statusFor(ex.errorCode()))
                .body(new CodeforcesHandleAccountErrorResponse(ex.errorCode().name(), ex.getMessage()));
    }

    private static HttpStatus statusFor(CodeforcesHandleAccountException.ErrorCode errorCode) {
        return switch (errorCode) {
            case CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS, CODEFORCES_HANDLE_ACCOUNT_HANDLE_EXISTS ->
                    HttpStatus.CONFLICT;
        };
    }
}
