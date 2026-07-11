package com.custacm.platform.trainingdata.common.web.purge.response;

import java.util.List;
import java.util.Map;

public record OjStudentDataPurgeResponse(
        String username,
        String ojName,
        String handle,
        Map<String, String> handles,
        List<OjDataPurgeResponse> ojResults,
        int handleAccountRows,
        int odsSubmissionRows,
        int dwdSubmissionRows,
        int dwmFirstAcceptedRows,
        int dwsAcceptedSummaryRows,
        int totalDeletedRows
) {
    public record OjDataPurgeResponse(
            String ojName,
            String handle,
            int odsSubmissionRows,
            int dwdSubmissionRows,
            int dwmFirstAcceptedRows,
            int dwsAcceptedSummaryRows,
            int totalDeletedRows
    ) {
    }
}
