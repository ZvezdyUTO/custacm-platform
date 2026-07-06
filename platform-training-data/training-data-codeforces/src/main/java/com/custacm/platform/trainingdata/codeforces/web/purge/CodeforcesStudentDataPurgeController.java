package com.custacm.platform.trainingdata.codeforces.web.purge;

import com.custacm.platform.trainingdata.codeforces.app.purge.CodeforcesStudentDataPurgeService;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesStudentDataPurgeResult;
import com.custacm.platform.trainingdata.codeforces.web.purge.response.CodeforcesStudentDataPurgeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodeforcesStudentDataPurgeController {
    private final CodeforcesStudentDataPurgeService service;

    public CodeforcesStudentDataPurgeController(CodeforcesStudentDataPurgeService service) {
        this.service = service;
    }

    @DeleteMapping("/api/training-data/admin/codeforces/users/{studentIdentity}/data")
    public ResponseEntity<CodeforcesStudentDataPurgeResponse> purgeStudentData(
            @PathVariable("studentIdentity") String studentIdentity
    ) {
        return ResponseEntity.ok(toResponse(service.purgeStudentData(studentIdentity)));
    }

    private static CodeforcesStudentDataPurgeResponse toResponse(CodeforcesStudentDataPurgeResult result) {
        return new CodeforcesStudentDataPurgeResponse(
                result.studentIdentity(),
                result.handle(),
                result.handleAccountRows(),
                result.odsSubmissionRows(),
                result.dwdSubmissionRows(),
                result.dwmFirstAcceptedRows(),
                result.dwsAcceptedSummaryRows(),
                result.totalDeletedRows()
        );
    }
}
