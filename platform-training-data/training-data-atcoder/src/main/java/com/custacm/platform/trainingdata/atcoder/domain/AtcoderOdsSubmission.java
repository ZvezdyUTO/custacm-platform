package com.custacm.platform.trainingdata.atcoder.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record AtcoderOdsSubmission(
        Long atcoderSubmissionId,
        Long epochSecond,
        String problemId,
        String contestId,
        String userId,
        String language,
        BigDecimal point,
        Integer sourceCodeLength,
        String result,
        Integer executionTimeMillis,
        String batchId,
        Instant fetchedAt,
        String rawPayload,
        String payloadHash
) {
    public AtcoderOdsSubmission {
        Objects.requireNonNull(atcoderSubmissionId, "atcoderSubmissionId must not be null");
        Objects.requireNonNull(epochSecond, "epochSecond must not be null");
        requireText(userId, "userId");
        requireText(batchId, "batchId");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
        requireText(rawPayload, "rawPayload");
        requireText(payloadHash, "payloadHash");
    }
}
