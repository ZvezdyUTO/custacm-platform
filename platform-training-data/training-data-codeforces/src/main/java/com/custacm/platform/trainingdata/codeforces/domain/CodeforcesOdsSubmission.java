package com.custacm.platform.trainingdata.codeforces.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record CodeforcesOdsSubmission(
        Long codeforcesSubmissionId,
        Long contestId,
        Long creationTimeSeconds,
        Integer relativeTimeSeconds,
        Long problemContestId,
        String problemIndex,
        String problemName,
        String problemType,
        BigDecimal problemPoints,
        Integer problemRating,
        String problemTagsJson,
        String authorHandle,
        String authorParticipantType,
        String authorJson,
        String programmingLanguage,
        String verdict,
        String testset,
        Integer passedTestCount,
        Integer timeConsumedMillis,
        Long memoryConsumedBytes,
        String batchId,
        Instant fetchedAt,
        String rawPayload,
        String payloadHash
) {
    public CodeforcesOdsSubmission {
        Objects.requireNonNull(codeforcesSubmissionId, "codeforcesSubmissionId must not be null");
        requireText(authorHandle, "authorHandle");
        requireText(batchId, "batchId");
        Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
        requireText(rawPayload, "rawPayload");
        requireText(payloadHash, "payloadHash");
    }
}
