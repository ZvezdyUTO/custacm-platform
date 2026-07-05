package com.custacm.platform.trainingdata.codeforces.web.response;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesSubmissionItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CodeforcesSubmissionItemResponse(
        long codeforcesSubmissionId,
        String studentIdentity,
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
    public static CodeforcesSubmissionItemResponse from(CodeforcesSubmissionItem item) {
        return new CodeforcesSubmissionItemResponse(
                item.codeforcesSubmissionId(),
                item.studentIdentity(),
                item.authorHandle(),
                item.contestId(),
                item.submittedAtUtcPlus8(),
                item.submittedDateUtcPlus8(),
                item.relativeTimeSeconds(),
                item.problemKey(),
                item.problemContestId(),
                item.problemIndex(),
                item.problemName(),
                item.problemType(),
                item.problemPoints(),
                item.problemRating(),
                item.problemTagsJson(),
                item.authorParticipantType(),
                item.programmingLanguage(),
                item.verdict(),
                item.accepted(),
                item.testset(),
                item.passedTestCount(),
                item.timeConsumedMillis(),
                item.memoryConsumedBytes()
        );
    }
}
