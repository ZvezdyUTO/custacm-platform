package com.custacm.platform.trainingdata.atcoder.domain;

import java.time.Instant;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record AtcoderOdsProblem(
        String problemId,
        String contestId,
        String problemIndex,
        String problemName,
        String title,
        String batchId,
        Instant fetchedAt,
        String rawPayload,
        String payloadHash
) {
    public AtcoderOdsProblem {
        requireText(problemId, "problemId");
        requireText(batchId, "batchId");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
        requireText(rawPayload, "rawPayload");
        requireText(payloadHash, "payloadHash");
    }
}
