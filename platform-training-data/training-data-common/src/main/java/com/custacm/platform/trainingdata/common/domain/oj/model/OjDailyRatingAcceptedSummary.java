package com.custacm.platform.trainingdata.common.domain.oj.model;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjDifficultyBucketPolicies;

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
        source.forEach((ratingKey, count) -> normalizedCounts.put(ratingKey, normalizedCount(ratingKey, count)));
        acceptedProblemCountsByRating = Map.copyOf(normalizedCounts);
    }

    public int acceptedProblemCount(String problemRatingKey) {
        return acceptedProblemCountsByRating.getOrDefault(problemRatingKey, 0);
    }

    public int acceptedProblemCount(int problemRating) {
        return acceptedProblemCount(Integer.toString(problemRating));
    }

    public int unratedAcceptedProblemCount() {
        return acceptedProblemCount(OjDifficultyBucketPolicies.UNRATED_KEY);
    }

    private static int normalizedCount(String ratingKey, Integer count) {
        requireText(ratingKey, "ratingKey");
        if (count == null) {
            return 0;
        }
        if (count < 0) {
            throw new IllegalArgumentException("accepted problem counts must not be negative");
        }
        return count;
    }
}
