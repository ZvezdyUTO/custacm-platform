package com.custacm.platform.trainingdata.codeforces.domain.model;

import com.custacm.platform.trainingdata.codeforces.domain.value.CodeforcesProblemRatingBuckets;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record CodeforcesDailyRatingAcceptedSummary(
        String authorHandle,
        LocalDate acceptedDateUtcPlus8,
        Map<String, Integer> acceptedProblemCountsByRating
) {
    public CodeforcesDailyRatingAcceptedSummary {
        if (authorHandle == null || authorHandle.isBlank()) {
            throw new IllegalArgumentException("authorHandle must not be blank");
        }
        authorHandle = authorHandle.trim();
        Objects.requireNonNull(acceptedDateUtcPlus8, "acceptedDateUtcPlus8 must not be null");

        Map<String, Integer> source = Objects.requireNonNull(
                acceptedProblemCountsByRating,
                "acceptedProblemCountsByRating must not be null"
        );
        LinkedHashMap<String, Integer> normalizedCounts = new LinkedHashMap<>();
        for (Integer rating : CodeforcesProblemRatingBuckets.RATINGS) {
            String ratingKey = CodeforcesProblemRatingBuckets.keyForRating(rating);
            normalizedCounts.put(ratingKey, normalizedCount(source, ratingKey));
        }
        normalizedCounts.put(
                CodeforcesProblemRatingBuckets.UNRATED_KEY,
                normalizedCount(source, CodeforcesProblemRatingBuckets.UNRATED_KEY)
        );
        acceptedProblemCountsByRating = Map.copyOf(normalizedCounts);
    }

    public int acceptedProblemCount(String problemRatingKey) {
        return acceptedProblemCountsByRating.getOrDefault(problemRatingKey, 0);
    }

    public int acceptedProblemCount(int problemRating) {
        return acceptedProblemCount(CodeforcesProblemRatingBuckets.keyForRating(problemRating));
    }

    public int unratedAcceptedProblemCount() {
        return acceptedProblemCount(CodeforcesProblemRatingBuckets.UNRATED_KEY);
    }

    private static int normalizedCount(Map<String, Integer> source, String ratingKey) {
        Integer count = source.get(ratingKey);
        if (count == null) {
            return 0;
        }
        if (count < 0) {
            throw new IllegalArgumentException("accepted problem counts must not be negative");
        }
        return count;
    }
}
