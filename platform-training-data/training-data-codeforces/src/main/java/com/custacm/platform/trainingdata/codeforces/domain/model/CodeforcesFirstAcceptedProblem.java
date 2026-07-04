package com.custacm.platform.trainingdata.codeforces.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CodeforcesFirstAcceptedProblem(
        String authorHandle,
        String problemKey,
        long problemContestId,
        String problemIndex,
        String problemName,
        String problemType,
        BigDecimal problemPoints,
        Integer problemRating,
        String problemTagsJson,
        long firstAcceptedSubmissionId,
        LocalDateTime firstAcceptedAtUtcPlus8,
        LocalDate firstAcceptedDateUtcPlus8,
        String firstAcceptedLanguage
) {
}
