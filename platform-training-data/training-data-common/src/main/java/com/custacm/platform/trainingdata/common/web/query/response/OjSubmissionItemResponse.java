package com.custacm.platform.trainingdata.common.web.query.response;

import com.custacm.platform.trainingdata.common.app.query.result.OjSubmissionItem;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OjSubmissionItemResponse(
        String submissionId,
        String studentIdentity,
        String handle,
        LocalDateTime submittedAtUtcPlus8,
        LocalDate submittedDateUtcPlus8,
        String problemKey,
        String problemIndex,
        String problemName,
        String difficulty,
        String language,
        String verdict,
        boolean accepted,
        Integer timeConsumedMillis,
        String sourceUrl
) {
    public static OjSubmissionItemResponse from(OjSubmissionItem item) {
        return new OjSubmissionItemResponse(
                item.submissionId(),
                item.studentIdentity(),
                item.handle(),
                item.submittedAtUtcPlus8(),
                item.submittedDateUtcPlus8(),
                item.problemKey(),
                item.problemIndex(),
                item.problemName(),
                item.difficulty(),
                item.language(),
                item.verdict(),
                item.accepted(),
                item.timeConsumedMillis(),
                item.sourceUrl()
        );
    }
}
