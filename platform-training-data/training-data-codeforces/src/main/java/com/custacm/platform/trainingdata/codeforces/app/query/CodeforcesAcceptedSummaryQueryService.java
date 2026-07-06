package com.custacm.platform.trainingdata.codeforces.app.query;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountService;
import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesAcceptedSummaryReport;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesDailyRatingAcceptedSummary;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.codeforces.domain.value.CodeforcesProblemRatingBuckets;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CodeforcesAcceptedSummaryQueryService {
    private final CodeforcesAcceptedSummaryRepository repository;
    private final CodeforcesHandleAccountService handleAccountService;

    public CodeforcesAcceptedSummaryQueryService(
            CodeforcesAcceptedSummaryRepository repository,
            CodeforcesHandleAccountService handleAccountService
    ) {
        this.repository = repository;
        this.handleAccountService = handleAccountService;
    }

    public CodeforcesAcceptedSummaryReport summarizeStudentAcceptedProblems(
            String studentIdentity,
            LocalDate acceptedFromDateUtcPlus8,
            LocalDate acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        CodeforcesHandleAccount account = handleAccountService.getByStudentIdentity(studentIdentity);
        return summarizeAccountAcceptedProblems(
                account,
                acceptedFromDateUtcPlus8,
                acceptedToDateUtcPlus8,
                minProblemRating,
                maxProblemRating
        );
    }

    public List<CodeforcesAcceptedSummaryReport> summarizeAutoCollectAcceptedProblems(
            LocalDate acceptedFromDateUtcPlus8,
            LocalDate acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        return handleAccountService.listAll().stream()
                .filter(CodeforcesHandleAccount::needCollect)
                .map(account -> summarizeAccountAcceptedProblems(
                        account,
                        acceptedFromDateUtcPlus8,
                        acceptedToDateUtcPlus8,
                        minProblemRating,
                        maxProblemRating
                ))
                .sorted(Comparator.comparingInt(CodeforcesAcceptedSummaryReport::totalAcceptedProblemCount)
                        .reversed()
                        .thenComparing(CodeforcesAcceptedSummaryReport::studentIdentity))
                .toList();
    }

    private CodeforcesAcceptedSummaryReport summarizeAccountAcceptedProblems(
            CodeforcesHandleAccount account,
            LocalDate acceptedFromDateUtcPlus8,
            LocalDate acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        CodeforcesAcceptedSummaryCriteria query = new CodeforcesAcceptedSummaryCriteria(
                account.handle(),
                acceptedFromDateUtcPlus8,
                acceptedToDateUtcPlus8,
                minProblemRating,
                maxProblemRating
        );
        List<CodeforcesDailyRatingAcceptedSummary> rows = repository.findDailyRatingAcceptedSummaries(query);
        List<CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount> ratingCounts = ratingCounts(
                rows,
                query.minProblemRating(),
                query.maxProblemRating()
        );
        return new CodeforcesAcceptedSummaryReport(
                account.studentIdentity(),
                account.handle(),
                totalCount(ratingCounts),
                ratingCounts
        );
    }

    private static List<CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount> ratingCounts(
            List<CodeforcesDailyRatingAcceptedSummary> rows,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        List<CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount> counts = new ArrayList<>();
        for (Integer rating : CodeforcesProblemRatingBuckets.RATINGS) {
            if (!withinProblemRatingBounds(rating, minProblemRating, maxProblemRating)) {
                continue;
            }
            int acceptedProblemCount = rows.stream()
                    .mapToInt(row -> row.acceptedProblemCount(rating))
                    .sum();
            if (acceptedProblemCount > 0) {
                counts.add(new CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount(
                        CodeforcesProblemRatingBuckets.keyForRating(rating),
                        acceptedProblemCount
                ));
            }
        }

        if (includesUnrated(minProblemRating, maxProblemRating)) {
            int acceptedProblemCount = rows.stream()
                    .mapToInt(CodeforcesDailyRatingAcceptedSummary::unratedAcceptedProblemCount)
                    .sum();
            if (acceptedProblemCount > 0) {
                counts.add(new CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount(
                        CodeforcesProblemRatingBuckets.UNRATED_KEY,
                        acceptedProblemCount
                ));
            }
        }

        return counts;
    }

    private static boolean withinProblemRatingBounds(
            int problemRating,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        if (minProblemRating != null && problemRating < minProblemRating) {
            return false;
        }
        return maxProblemRating == null || problemRating <= maxProblemRating;
    }

    private static boolean includesUnrated(Integer minProblemRating, Integer maxProblemRating) {
        return minProblemRating == null && maxProblemRating == null;
    }

    private static int totalCount(List<CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount> counts) {
        return counts.stream()
                .mapToInt(CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount::acceptedProblemCount)
                .sum();
    }
}
