package com.custacm.platform.trainingdata.common.app.query.result;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OjHandleFirstAcceptedProblemReport(
        String studentIdentity,
        String authorHandle,
        int totalAcceptedProblemCount,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<OjFirstAcceptedProblemItem> problems
) {
    public record OjFirstAcceptedProblemItem(
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
}
