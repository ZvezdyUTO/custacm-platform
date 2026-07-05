package com.custacm.platform.trainingdata.codeforces.web.warehouse;

import com.custacm.platform.common.sqltask.SqlTaskErrorCode;
import com.custacm.platform.common.sqltask.SqlTaskException;
import com.custacm.platform.trainingdata.codeforces.web.warehouse.CodeforcesWarehouseRefreshController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = CodeforcesWarehouseRefreshController.class)
public class CodeforcesWarehouseRefreshExceptionHandler {
    private static final String INVALID_REQUEST_ERROR_CODE = "CODEFORCES_WAREHOUSE_REFRESH_INVALID_REQUEST";
    private static final Logger log = LoggerFactory.getLogger(CodeforcesWarehouseRefreshExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Codeforces warehouse refresh request rejected, errorCode={}, reason={}",
                INVALID_REQUEST_ERROR_CODE, ex.getMessage());
        return ResponseEntity.badRequest()
                .body(body(INVALID_REQUEST_ERROR_CODE, ex.getMessage()));
    }

    @ExceptionHandler(SqlTaskException.class)
    public ResponseEntity<Map<String, String>> handleSqlTaskException(SqlTaskException ex) {
        if (ex.errorCode() == SqlTaskErrorCode.SQL_TASK_START_NODE_INVALID) {
            log.warn("Codeforces warehouse refresh request rejected, errorCode={}, reason={}", ex.errorCode(), ex.getMessage());
            return ResponseEntity.badRequest()
                    .body(body(ex.errorCode().name(), ex.getMessage()));
        }
        log.error("Codeforces warehouse refresh failed, errorCode={}", ex.errorCode(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(ex.errorCode().name(), ex.getMessage()));
    }

    private Map<String, String> body(String code, String message) {
        return Map.of(
                "code", code,
                "message", message
        );
    }
}
