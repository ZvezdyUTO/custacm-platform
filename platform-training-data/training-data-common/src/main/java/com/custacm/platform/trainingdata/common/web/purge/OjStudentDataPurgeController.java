package com.custacm.platform.trainingdata.common.web.purge;

import com.custacm.platform.trainingdata.common.app.purge.OjStudentDataPurgeService;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjStudentDataPurgeResult;
import com.custacm.platform.trainingdata.common.web.purge.response.OjStudentDataPurgeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OjStudentDataPurgeController {
    private final OjStudentDataPurgeService service;

    public OjStudentDataPurgeController(OjStudentDataPurgeService service) {
        this.service = service;
    }

    @DeleteMapping("/api/training-data/admin/students/{studentIdentity}/oj-data")
    public ResponseEntity<OjStudentDataPurgeResponse> purgeStudentData(
            @PathVariable("studentIdentity") String studentIdentity,
            @RequestParam(value = "ojName", required = false) String ojName
    ) {
        return ResponseEntity.ok(toResponse(service.purgeStudentData(studentIdentity, ojName)));
    }

    private static OjStudentDataPurgeResponse toResponse(OjStudentDataPurgeResult result) {
        return new OjStudentDataPurgeResponse(
                result.studentIdentity(),
                result.ojName(),
                result.handle(),
                result.handles(),
                result.ojResults().stream()
                        .map(OjStudentDataPurgeController::toOjResponse)
                        .toList(),
                result.handleAccountRows(),
                result.odsSubmissionRows(),
                result.dwdSubmissionRows(),
                result.dwmFirstAcceptedRows(),
                result.dwsAcceptedSummaryRows(),
                result.totalDeletedRows()
        );
    }

    private static OjStudentDataPurgeResponse.OjDataPurgeResponse toOjResponse(
            OjStudentDataPurgeResult.OjDataPurgeResult result
    ) {
        return new OjStudentDataPurgeResponse.OjDataPurgeResponse(
                result.ojName(),
                result.handle(),
                result.odsSubmissionRows(),
                result.dwdSubmissionRows(),
                result.dwmFirstAcceptedRows(),
                result.dwsAcceptedSummaryRows(),
                result.totalDeletedRows()
        );
    }
}
