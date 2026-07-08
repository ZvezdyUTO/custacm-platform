package com.custacm.platform.trainingdata.common.app.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.query.result.OjAcceptedSummaryReport;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjDailyRatingAcceptedSummary;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjDifficultyBucketPolicies;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjDifficultyBucketPolicy;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OjAcceptedSummaryQueryService {
    private final OjAcceptedSummaryRepository repository;
    private final OjHandleAccountService handleAccountService;
    private final OjDifficultyBucketPolicies bucketPolicies;

    public OjAcceptedSummaryQueryService(
            OjAcceptedSummaryRepository repository,
            OjHandleAccountService handleAccountService,
            OjDifficultyBucketPolicies bucketPolicies
    ) {
        this.repository = repository;
        this.handleAccountService = handleAccountService;
        this.bucketPolicies = bucketPolicies;
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
        OjDifficultyBucketPolicy bucketPolicy = bucketPolicies.policyFor(query.ojName());
        List<OjAcceptedSummaryReport.OjRatingAcceptedCount> ratingCounts = ratingCounts(
                rows,
                bucketPolicy,
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
            OjDifficultyBucketPolicy bucketPolicy,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        List<OjAcceptedSummaryReport.OjRatingAcceptedCount> counts = new ArrayList<>();
        List<String> allRatedBucketKeys = bucketPolicy.ratedBucketKeys();
        List<String> ratedBucketKeys = bucketPolicy.bucketKeysInRange(minProblemRating, maxProblemRating);
        if (ratedBucketKeys == null) {
            ratedBucketKeys = allRatedBucketKeys;
        }
        for (String bucketKey : ratedBucketKeys) {
            int acceptedProblemCount = rows.stream()
                    .mapToInt(row -> row.acceptedProblemCount(bucketKey))
                    .sum();
            if (acceptedProblemCount > 0) {
                counts.add(new OjAcceptedSummaryReport.OjRatingAcceptedCount(
                        bucketKey,
                        acceptedProblemCount
                ));
            }
        }

        if (bucketPolicy.includesUnrated(minProblemRating, maxProblemRating)) {
            int acceptedProblemCount = rows.stream()
                    .mapToInt(row -> unratedOrUnknownAcceptedProblemCount(row, allRatedBucketKeys))
                    .sum();
            if (acceptedProblemCount > 0) {
                counts.add(new OjAcceptedSummaryReport.OjRatingAcceptedCount(
                        OjDifficultyBucketPolicies.UNRATED_KEY,
                        acceptedProblemCount
                ));
            }
        }

        return counts;
    }

    private static int unratedOrUnknownAcceptedProblemCount(
            OjDailyRatingAcceptedSummary row,
            List<String> ratedBucketKeys
    ) {
        return row.acceptedProblemCountsByRating().entrySet().stream()
                .filter(entry -> OjDifficultyBucketPolicies.UNRATED_KEY.equals(entry.getKey())
                        || !ratedBucketKeys.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    private static int totalCount(List<OjAcceptedSummaryReport.OjRatingAcceptedCount> counts) {
        return counts.stream()
                .mapToInt(OjAcceptedSummaryReport.OjRatingAcceptedCount::acceptedProblemCount)
                .sum();
    }
}
