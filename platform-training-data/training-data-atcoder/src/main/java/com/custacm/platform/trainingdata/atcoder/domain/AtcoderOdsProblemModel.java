package com.custacm.platform.trainingdata.atcoder.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record AtcoderOdsProblemModel(
        String problemId,
        BigDecimal slope,
        BigDecimal intercept,
        BigDecimal variance,
        Integer rawDifficulty,
        Integer clippedDifficulty,
        BigDecimal discrimination,
        BigDecimal irtLogLikelihood,
        Integer irtUsers,
        Boolean experimental,
        String batchId,
        Instant fetchedAt,
        String rawPayload,
        String payloadHash
) {
    public AtcoderOdsProblemModel {
        requireText(problemId, "problemId");
        requireText(batchId, "batchId");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
        requireText(rawPayload, "rawPayload");
        requireText(payloadHash, "payloadHash");
    }
}
