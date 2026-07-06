package com.custacm.platform.trainingdata.codeforces.web.account;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountService;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.web.account.request.ChangeCodeforcesHandleIdentityRequest;
import com.custacm.platform.trainingdata.codeforces.web.account.request.CreateCodeforcesHandleAccountRequest;
import com.custacm.platform.trainingdata.codeforces.web.account.response.CodeforcesHandleAccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodeforcesHandleAccountController {
    private final CodeforcesHandleAccountService service;

    public CodeforcesHandleAccountController(CodeforcesHandleAccountService service) {
        this.service = service;
    }

    @PostMapping("/api/training-data/admin/codeforces/handles")
    public ResponseEntity<CodeforcesHandleAccountResponse> create(@RequestBody CreateCodeforcesHandleAccountRequest request) {
        if (request == null) {
            throw invalidRequest("request body must not be empty");
        }
        String studentIdentity = requireRequestText(request.studentIdentity(), "studentIdentity");
        String handle = requireRequestText(request.handle(), "handle");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(service.create(studentIdentity, handle)));
    }

    @GetMapping(value = "/api/training-data/codeforces/handles", params = "studentIdentity")
    public ResponseEntity<CodeforcesHandleAccountResponse> getByStudentIdentity(
            @RequestParam("studentIdentity") String studentIdentity
    ) {
        return ResponseEntity.ok(toResponse(service.getByStudentIdentity(
                requireRequestText(studentIdentity, "studentIdentity")
        )));
    }

    @PatchMapping("/api/training-data/admin/codeforces/handles:change-identity")
    public ResponseEntity<CodeforcesHandleAccountResponse> changeStudentIdentity(
            @RequestBody ChangeCodeforcesHandleIdentityRequest request
    ) {
        if (request == null) {
            throw invalidRequest("request body must not be empty");
        }
        String oldStudentIdentity = requireRequestText(request.oldStudentIdentity(), "oldStudentIdentity");
        String newStudentIdentity = requireRequestText(request.newStudentIdentity(), "newStudentIdentity");
        return ResponseEntity.ok(toResponse(service.changeStudentIdentity(
                oldStudentIdentity,
                newStudentIdentity,
                request.needCollect()
        )));
    }

    private static CodeforcesHandleAccountResponse toResponse(CodeforcesHandleAccount account) {
        return new CodeforcesHandleAccountResponse(account.studentIdentity(), account.handle(), account.needCollect());
    }

    private static String requireRequestText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalidRequest(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static CodeforcesHandleAccountException invalidRequest(String message) {
        return new CodeforcesHandleAccountException(
                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST,
                message
        );
    }
}
