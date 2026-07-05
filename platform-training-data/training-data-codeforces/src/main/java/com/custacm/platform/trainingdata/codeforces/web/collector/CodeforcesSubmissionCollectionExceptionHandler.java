package com.custacm.platform.trainingdata.codeforces.web.collector;

import com.custacm.platform.trainingdata.codeforces.web.collector.CodeforcesSubmissionCollectionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = CodeforcesSubmissionCollectionController.class)
public class CodeforcesSubmissionCollectionExceptionHandler {
    private static final String INVALID_REQUEST_ERROR_CODE =
            "CODEFORCES_SUBMISSION_COLLECTION_INVALID_REQUEST";
    private static final Logger log = LoggerFactory.getLogger(CodeforcesSubmissionCollectionExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Codeforces submission collection request rejected, errorCode={}, reason={}",
                INVALID_REQUEST_ERROR_CODE, ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "code", INVALID_REQUEST_ERROR_CODE,
                        "message", ex.getMessage()
                ));
    }
}
