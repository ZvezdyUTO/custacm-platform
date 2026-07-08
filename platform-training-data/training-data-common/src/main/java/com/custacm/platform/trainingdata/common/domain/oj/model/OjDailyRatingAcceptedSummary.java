package com.custacm.platform.trainingdata.common.domain.oj.model;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjProblemRatingBuckets;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record OjDailyRatingAcceptedSummary(
        String authorHandle,
        LocalDate acceptedDateUtcPlus8,
        Map<String, Integer> acceptedProblemCountsByRating
) {
    public OjDailyRatingAcceptedSummary {
        authorHandle = requireText(authorHandle, "authorHandle");
        Objects.requireNonNull(acceptedDateUtcPlus8, "acceptedDateUtcPlus8 must not be null");

        Map<String, Integer> source = Objects.requireNonNull(
                acceptedProblemCountsByRating,
                "acceptedProblemCountsByRating must not be null"
        );
        LinkedHashMap<String, Integer> normalizedCounts = new LinkedHashMap<>();
        for (Integer rating : OjProblemRatingBuckets.RATINGS) {
            String ratingKey = OjProblemRatingBuckets.keyForRating(rating);
            normalizedCounts.put(ratingKey, normalizedCount(source, ratingKey));
        }
        normalizedCounts.put(
                OjProblemRatingBuckets.UNRATED_KEY,
                normalizedCount(source, OjProblemRatingBuckets.UNRATED_KEY)
        );
        acceptedProblemCountsByRating = Map.copyOf(normalizedCounts);
    }

    public int acceptedProblemCount(String problemRatingKey) {
        return acceptedProblemCountsByRating.getOrDefault(problemRatingKey, 0);
    }

    public int acceptedProblemCount(int problemRating) {
        return acceptedProblemCount(OjProblemRatingBuckets.keyForRating(problemRating));
    }

    public int unratedAcceptedProblemCount() {
        return acceptedProblemCount(OjProblemRatingBuckets.UNRATED_KEY);
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
