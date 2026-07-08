package com.custacm.platform.trainingdata.common.domain.oj.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OjSubmission(
        String submissionId,
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
