package com.custacm.platform.trainingdata.codeforces.domain.model;

public record CodeforcesStudentDataPurgeResult(
        String studentIdentity,
        String handle,
        int handleAccountRows,
        int odsSubmissionRows,
        int dwdSubmissionRows,
        int dwmFirstAcceptedRows,
        int dwsAcceptedSummaryRows
) {
    public CodeforcesStudentDataPurgeResult {
        if (studentIdentity == null || studentIdentity.isBlank()) {
            throw new IllegalArgumentException("studentIdentity must not be blank");
        }
        requireNonNegative(handleAccountRows, "handleAccountRows");
        requireNonNegative(odsSubmissionRows, "odsSubmissionRows");
        requireNonNegative(dwdSubmissionRows, "dwdSubmissionRows");
        requireNonNegative(dwmFirstAcceptedRows, "dwmFirstAcceptedRows");
        requireNonNegative(dwsAcceptedSummaryRows, "dwsAcceptedSummaryRows");
        studentIdentity = studentIdentity.trim();
        handle = handle == null || handle.isBlank() ? null : handle.trim();
    }

    public int totalDeletedRows() {
        return handleAccountRows
                + odsSubmissionRows
                + dwdSubmissionRows
                + dwmFirstAcceptedRows
                + dwsAcceptedSummaryRows;
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
