package com.custacm.platform.trainingdata.common.domain.oj.repo;

public interface OjWarehouseDataPurgeRepository {
    OjWarehouseDataPurgeCounts purgeAllByHandle(String ojName, String handle);

    record OjWarehouseDataPurgeCounts(
            int dwdSubmissionRows,
            int dwmFirstAcceptedRows,
            int dwsAcceptedSummaryRows
    ) {
        public OjWarehouseDataPurgeCounts {
            requireNonNegative(dwdSubmissionRows, "dwdSubmissionRows");
            requireNonNegative(dwmFirstAcceptedRows, "dwmFirstAcceptedRows");
            requireNonNegative(dwsAcceptedSummaryRows, "dwsAcceptedSummaryRows");
        }

        private static void requireNonNegative(int value, String fieldName) {
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " must not be negative");
            }
        }
    }
}
