package com.custacm.platform.trainingdata.common.web.purge;

import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeException;
import com.custacm.platform.trainingdata.common.web.purge.response.OjStudentDataPurgeErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.custacm.platform.trainingdata.common.web")
public class OjStudentDataPurgeExceptionHandler {
    @ExceptionHandler(OjStudentDataPurgeException.class)
    public ResponseEntity<OjStudentDataPurgeErrorResponse> handlePurgeException(
            OjStudentDataPurgeException ex
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new OjStudentDataPurgeErrorResponse(ex.errorCode().name(), ex.getMessage()));
    }
}
