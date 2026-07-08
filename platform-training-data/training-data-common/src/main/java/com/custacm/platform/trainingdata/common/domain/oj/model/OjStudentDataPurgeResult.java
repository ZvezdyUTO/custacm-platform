package com.custacm.platform.trainingdata.common.domain.oj.model;

import java.util.List;
import java.util.Map;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record OjStudentDataPurgeResult(
        String studentIdentity,
        String ojName,
        String handle,
        Map<String, String> handles,
        List<OjDataPurgeResult> ojResults,
        int handleAccountRows,
        int odsSubmissionRows,
        int dwdSubmissionRows,
        int dwmFirstAcceptedRows,
        int dwsAcceptedSummaryRows
) {
    public OjStudentDataPurgeResult {
        studentIdentity = requireText(studentIdentity, "studentIdentity");
        ojName = ojName == null || ojName.isBlank() ? null : ojName.trim();
        handles = Map.copyOf(handles);
        ojResults = List.copyOf(ojResults);
        requireNonNegative(handleAccountRows, "handleAccountRows");
        requireNonNegative(odsSubmissionRows, "odsSubmissionRows");
        requireNonNegative(dwdSubmissionRows, "dwdSubmissionRows");
        requireNonNegative(dwmFirstAcceptedRows, "dwmFirstAcceptedRows");
        requireNonNegative(dwsAcceptedSummaryRows, "dwsAcceptedSummaryRows");
        handle = handle == null || handle.isBlank() ? null : handle.trim();
    }

    public OjStudentDataPurgeResult(
            String studentIdentity,
            String handle,
            int handleAccountRows,
            int odsSubmissionRows,
            int dwdSubmissionRows,
            int dwmFirstAcceptedRows,
            int dwsAcceptedSummaryRows
    ) {
        this(
                studentIdentity,
                null,
                handle,
                Map.of(),
                List.of(),
                handleAccountRows,
                odsSubmissionRows,
                dwdSubmissionRows,
                dwmFirstAcceptedRows,
                dwsAcceptedSummaryRows
        );
    }

    public static OjStudentDataPurgeResult aggregate(
            String studentIdentity,
            String ojName,
            String handle,
            Map<String, String> handles,
            List<OjDataPurgeResult> ojResults
    ) {
        int odsSubmissionRows = 0;
        int dwdSubmissionRows = 0;
        int dwmFirstAcceptedRows = 0;
        int dwsAcceptedSummaryRows = 0;
        for (OjDataPurgeResult result : ojResults) {
            odsSubmissionRows += result.odsSubmissionRows();
            dwdSubmissionRows += result.dwdSubmissionRows();
            dwmFirstAcceptedRows += result.dwmFirstAcceptedRows();
            dwsAcceptedSummaryRows += result.dwsAcceptedSummaryRows();
        }
        return new OjStudentDataPurgeResult(
                studentIdentity,
                ojName,
                handle,
                handles,
                ojResults,
                0,
                odsSubmissionRows,
                dwdSubmissionRows,
                dwmFirstAcceptedRows,
                dwsAcceptedSummaryRows
        );
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

    public record OjDataPurgeResult(
            String ojName,
            String handle,
            int odsSubmissionRows,
            int dwdSubmissionRows,
            int dwmFirstAcceptedRows,
            int dwsAcceptedSummaryRows
    ) {
        public OjDataPurgeResult {
            ojName = requireText(ojName, "ojName");
            handle = requireText(handle, "handle");
            requireNonNegative(odsSubmissionRows, "odsSubmissionRows");
            requireNonNegative(dwdSubmissionRows, "dwdSubmissionRows");
            requireNonNegative(dwmFirstAcceptedRows, "dwmFirstAcceptedRows");
            requireNonNegative(dwsAcceptedSummaryRows, "dwsAcceptedSummaryRows");
        }

        public int totalDeletedRows() {
            return odsSubmissionRows + dwdSubmissionRows + dwmFirstAcceptedRows + dwsAcceptedSummaryRows;
        }
    }
}
