package com.custacm.platform.trainingdata.common.app.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.query.result.OjAcceptedSummaryReport;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjDailyRatingAcceptedSummary;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjProblemRatingBuckets;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OjAcceptedSummaryQueryService {
    private final OjAcceptedSummaryRepository repository;
    private final OjHandleAccountService handleAccountService;

    public OjAcceptedSummaryQueryService(
            OjAcceptedSummaryRepository repository,
            OjHandleAccountService handleAccountService
    ) {
        this.repository = repository;
        this.handleAccountService = handleAccountService;
    }

    public OjAcceptedSummaryReport summarizeStudentAcceptedProblems(
            String studentIdentity,
            LocalDate acceptedFromDateUtcPlus8,
            LocalDate acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        return summarizeStudentAcceptedProblems(
                OjNames.CODEFORCES,
                studentIdentity,
                acceptedFromDateUtcPlus8,
                acceptedToDateUtcPlus8,
                minProblemRating,
                maxProblemRating
        );
    }

    public OjAcceptedSummaryReport summarizeStudentAcceptedProblems(
            String ojName,
            String studentIdentity,
            LocalDate acceptedFromDateUtcPlus8,
            LocalDate acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        OjHandleAccount account = handleAccountService.getByStudentIdentity(studentIdentity);
        return summarizeAccountAcceptedProblems(
                ojName,
                account,
                acceptedFromDateUtcPlus8,
                acceptedToDateUtcPlus8,
                minProblemRating,
                maxProblemRating
        );
    }

    private OjAcceptedSummaryReport summarizeAccountAcceptedProblems(
            String ojName,
            OjHandleAccount account,
            LocalDate acceptedFromDateUtcPlus8,
            LocalDate acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        String ojHandle = handleAccountService.getHandle(account, ojName);
        OjAcceptedSummaryCriteria query = new OjAcceptedSummaryCriteria(
                ojName,
                ojHandle,
                acceptedFromDateUtcPlus8,
                acceptedToDateUtcPlus8,
                minProblemRating,
                maxProblemRating
        );
        List<OjDailyRatingAcceptedSummary> rows = repository.findDailyRatingAcceptedSummaries(query);
        List<OjAcceptedSummaryReport.OjRatingAcceptedCount> ratingCounts = ratingCounts(
                rows,
                query.minProblemRating(),
                query.maxProblemRating()
        );
        return new OjAcceptedSummaryReport(
                account.studentIdentity(),
                ojHandle,
                totalCount(ratingCounts),
                ratingCounts
        );
    }

    private static List<OjAcceptedSummaryReport.OjRatingAcceptedCount> ratingCounts(
            List<OjDailyRatingAcceptedSummary> rows,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        List<OjAcceptedSummaryReport.OjRatingAcceptedCount> counts = new ArrayList<>();
        for (Integer rating : OjProblemRatingBuckets.RATINGS) {
            if (!withinProblemRatingBounds(rating, minProblemRating, maxProblemRating)) {
                continue;
            }
            int acceptedProblemCount = rows.stream()
                    .mapToInt(row -> row.acceptedProblemCount(rating))
                    .sum();
            if (acceptedProblemCount > 0) {
                counts.add(new OjAcceptedSummaryReport.OjRatingAcceptedCount(
                        OjProblemRatingBuckets.keyForRating(rating),
                        acceptedProblemCount
                ));
            }
        }

        if (includesUnrated(minProblemRating, maxProblemRating)) {
            int acceptedProblemCount = rows.stream()
                    .mapToInt(OjDailyRatingAcceptedSummary::unratedAcceptedProblemCount)
                    .sum();
            if (acceptedProblemCount > 0) {
                counts.add(new OjAcceptedSummaryReport.OjRatingAcceptedCount(
                        OjProblemRatingBuckets.UNRATED_KEY,
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

    private static int totalCount(List<OjAcceptedSummaryReport.OjRatingAcceptedCount> counts) {
        return counts.stream()
                .mapToInt(OjAcceptedSummaryReport.OjRatingAcceptedCount::acceptedProblemCount)
                .sum();
    }
}
