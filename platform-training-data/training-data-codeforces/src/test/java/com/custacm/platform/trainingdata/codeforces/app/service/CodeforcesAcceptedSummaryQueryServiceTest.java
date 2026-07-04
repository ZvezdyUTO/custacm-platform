package com.custacm.platform.trainingdata.codeforces.app.service;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesAcceptedSummaryReport;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesDailyRatingAcceptedSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CodeforcesAcceptedSummaryQueryServiceTest {
    @Test
    void summarizesDwsRowsByRatingInAppLayer() {
        CodeforcesAcceptedSummaryCriteria query = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-03"),
                null,
                null
        );
        CodeforcesAcceptedSummaryRepository repository = actualQuery -> {
            assertThat(actualQuery).isEqualTo(query);
            return List.of(
                    row("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)),
                    row("tourist", "2026-07-02", Map.of("800", 3, "UNRATED", 4))
            );
        };
        CodeforcesAcceptedSummaryQueryService service = new CodeforcesAcceptedSummaryQueryService(repository);

        CodeforcesAcceptedSummaryReport report = service.summarizeAcceptedProblems(query);

        assertThat(report.authorHandle()).isEqualTo("tourist");
        assertThat(report.totalAcceptedProblemCount()).isEqualTo(10);
        assertThat(report.ratingCounts()).containsExactly(
                rating("800", 5),
                rating("1200", 1),
                rating("UNRATED", 4)
        );
    }

    @Test
    void appliesProblemRatingBoundsToWideDwsRowsInAppLayer() {
        CodeforcesAcceptedSummaryCriteria query = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-03"),
                1200,
                1600
        );
        CodeforcesAcceptedSummaryRepository repository = actualQuery -> List.of(
                row("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)),
                row("tourist", "2026-07-02", Map.of("1600", 3, "UNRATED", 4))
        );
        CodeforcesAcceptedSummaryQueryService service = new CodeforcesAcceptedSummaryQueryService(repository);

        CodeforcesAcceptedSummaryReport report = service.summarizeAcceptedProblems(query);

        assertThat(report.totalAcceptedProblemCount()).isEqualTo(4);
        assertThat(report.ratingCounts()).containsExactly(
                rating("1200", 1),
                rating("1600", 3)
        );
    }

    private static CodeforcesDailyRatingAcceptedSummary row(
            String authorHandle,
            String acceptedDateUtcPlus8,
            Map<String, Integer> acceptedProblemCountsByRating
    ) {
        return new CodeforcesDailyRatingAcceptedSummary(
                authorHandle,
                LocalDate.parse(acceptedDateUtcPlus8),
                acceptedProblemCountsByRating
        );
    }

    private static CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount rating(
            String problemRating,
            int acceptedProblemCount
    ) {
        return new CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount(
                problemRating,
                acceptedProblemCount
        );
    }
}
