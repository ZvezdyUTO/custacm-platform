package com.custacm.platform.trainingdata.codeforces.app.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CodeforcesSubmissionItem(
        long codeforcesSubmissionId,
        String authorHandle,
        Long contestId,
        LocalDateTime submittedAtUtcPlus8,
        LocalDate submittedDateUtcPlus8,
        Integer relativeTimeSeconds,
        String problemKey,
        Long problemContestId,
        String problemIndex,
        String problemName,
        String problemType,
        BigDecimal problemPoints,
        Integer problemRating,
        String problemTagsJson,
        String authorParticipantType,
        String programmingLanguage,
        String verdict,
        boolean accepted,
        String testset,
        Integer passedTestCount,
        Integer timeConsumedMillis,
        Long memoryConsumedBytes
) {
}
