package com.custacm.platform.trainingdata.common.app.query.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OjSubmissionItem(
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
}
