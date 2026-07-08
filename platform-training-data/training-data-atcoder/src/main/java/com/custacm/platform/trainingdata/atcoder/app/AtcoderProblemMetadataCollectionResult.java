package com.custacm.platform.trainingdata.atcoder.app;

public record AtcoderProblemMetadataCollectionResult(
        AtcoderOdsBatchUpsertResult problemResult,
        AtcoderOdsBatchUpsertResult problemModelResult
) {
    public int writtenRows() {
        return problemResult.writtenRows() + problemModelResult.writtenRows();
    }
}
