package com.custacm.platform.trainingdata.common.web.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = OjSubmissionCollectionController.class)
public class OjSubmissionCollectionExceptionHandler {
    private static final String INVALID_REQUEST_ERROR_CODE =
            "OJ_SUBMISSION_COLLECTION_INVALID_REQUEST";
    private static final Logger log = LoggerFactory.getLogger(OjSubmissionCollectionExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("OJ submission collection request rejected, errorCode={}, reason={}",
                INVALID_REQUEST_ERROR_CODE, ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "code", INVALID_REQUEST_ERROR_CODE,
                        "message", ex.getMessage()
                ));
    }
}
