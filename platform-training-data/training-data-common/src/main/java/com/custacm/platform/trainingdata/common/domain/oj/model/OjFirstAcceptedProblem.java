package com.custacm.platform.trainingdata.common.domain.oj.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OjFirstAcceptedProblem(
        String handle,
        String problemKey,
        String problemIndex,
        String problemName,
        String difficulty,
        String firstAcceptedSubmissionId,
        LocalDateTime firstAcceptedAtUtcPlus8,
        LocalDate firstAcceptedDateUtcPlus8,
        String firstAcceptedLanguage,
        String firstAcceptedSourceUrl
) {
}
