package com.custacm.platform.trainingdata.codeforces.web.purge;

import com.custacm.platform.trainingdata.codeforces.app.purge.CodeforcesStudentDataPurgeException;
import com.custacm.platform.trainingdata.codeforces.web.purge.response.CodeforcesStudentDataPurgeErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.custacm.platform.trainingdata.codeforces.web")
public class CodeforcesStudentDataPurgeExceptionHandler {
    @ExceptionHandler(CodeforcesStudentDataPurgeException.class)
    public ResponseEntity<CodeforcesStudentDataPurgeErrorResponse> handlePurgeException(
            CodeforcesStudentDataPurgeException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new CodeforcesStudentDataPurgeErrorResponse(ex.errorCode().name(), ex.getMessage()));
    }
}
