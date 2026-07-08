package com.custacm.platform.trainingdata.common.web.account;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.web.account.request.ChangeOjHandleIdentityRequest;
import com.custacm.platform.trainingdata.common.web.account.request.CreateOjHandleAccountRequest;
import com.custacm.platform.trainingdata.common.web.account.response.OjHandleAccountResponse;
import com.custacm.platform.trainingdata.common.web.account.response.OjHandleCollectionStateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

@RestController
public class OjHandleAccountController {
    private final OjHandleAccountService service;

    public OjHandleAccountController(OjHandleAccountService service) {
        this.service = service;
    }

    @PostMapping("/api/training-data/admin/oj-handles")
    public ResponseEntity<OjHandleAccountResponse> create(@RequestBody CreateOjHandleAccountRequest request) {
        if (request == null) {
            throw invalidRequest("request body must not be empty");
        }
        String studentIdentity = requireText(
                request.studentIdentity(),
                "studentIdentity",
                OjHandleAccountController::invalidRequest
        );
        if (request.handles() == null || request.handles().isEmpty()) {
            throw invalidRequest("handles must not be empty");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(service.create(studentIdentity, request.handles())));
    }

    @GetMapping("/api/training-data/oj-handles")
    public ResponseEntity<Map<String, OjHandleAccountResponse>> listAll() {
        Map<String, OjHandleAccountResponse> responses = new LinkedHashMap<>();
        for (OjHandleAccount account : service.listAll()) {
            responses.put(account.studentIdentity(), toResponse(account));
        }
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/api/training-data/admin/oj-handles:change-identity")
    public ResponseEntity<OjHandleAccountResponse> changeStudentIdentity(
            @RequestBody ChangeOjHandleIdentityRequest request
    ) {
        if (request == null) {
            throw invalidRequest("request body must not be empty");
        }
        String oldStudentIdentity = requireText(
                request.oldStudentIdentity(),
                "oldStudentIdentity",
                OjHandleAccountController::invalidRequest
        );
        String newStudentIdentity = requireText(
                request.newStudentIdentity(),
                "newStudentIdentity",
                OjHandleAccountController::invalidRequest
        );
        return ResponseEntity.ok(toResponse(service.changeStudentIdentity(
                oldStudentIdentity,
                newStudentIdentity,
                request.needCollect(),
                request.handles()
        )));
    }

    private static OjHandleAccountResponse toResponse(OjHandleAccount account) {
        return new OjHandleAccountResponse(
                account.studentIdentity(),
                account.handles(),
                account.needCollect(),
                collectionStates(account)
        );
    }

    private static Map<String, OjHandleCollectionStateResponse> collectionStates(OjHandleAccount account) {
        Map<String, OjHandleCollectionStateResponse> states = new LinkedHashMap<>();
        account.collectionStates().forEach((ojName, state) -> states.put(
                ojName,
                OjHandleCollectionStateResponse.from(state)
        ));
        return states;
    }

    private static OjHandleAccountException invalidRequest(String message) {
        return new OjHandleAccountException(
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST,
                message
        );
    }
}
