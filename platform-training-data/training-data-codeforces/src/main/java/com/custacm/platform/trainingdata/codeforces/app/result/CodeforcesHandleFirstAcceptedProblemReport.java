package com.custacm.platform.trainingdata.codeforces.app.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CodeforcesHandleFirstAcceptedProblemReport(
        String studentIdentity,
        String authorHandle,
        int totalAcceptedProblemCount,
        List<CodeforcesFirstAcceptedProblemItem> problems
) {
    public record CodeforcesFirstAcceptedProblemItem(
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
}
