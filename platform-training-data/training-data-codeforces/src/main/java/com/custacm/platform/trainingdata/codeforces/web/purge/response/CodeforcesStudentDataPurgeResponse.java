package com.custacm.platform.trainingdata.codeforces.web.purge.response;

public record CodeforcesStudentDataPurgeResponse(
        String studentIdentity,
        String handle,
        int handleAccountRows,
        int odsSubmissionRows,
        int dwdSubmissionRows,
        int dwmFirstAcceptedRows,
        int dwsAcceptedSummaryRows,
        int totalDeletedRows
) {
}
