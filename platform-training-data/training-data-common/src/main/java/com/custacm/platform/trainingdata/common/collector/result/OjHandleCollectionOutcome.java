package com.custacm.platform.trainingdata.common.collector.result;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record OjHandleCollectionOutcome(
        OjSubmissionCollectionHandleResult result,
        List<JsonNode> submissions
) {
    public OjHandleCollectionOutcome {
        submissions = submissions == null ? List.of() : List.copyOf(submissions);
    }
}
