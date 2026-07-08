package com.custacm.platform.trainingdata.common.domain.oj.value;

import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record OjDifficultyBucket(
        String key,
        int fromInclusive,
        Integer toInclusive
) {
    public OjDifficultyBucket {
        key = requireText(key, "key");
        if (fromInclusive < 0) {
            throw new IllegalArgumentException("fromInclusive must not be negative");
        }
        if (toInclusive != null && toInclusive < fromInclusive) {
            throw new IllegalArgumentException("toInclusive must not be less than fromInclusive");
        }
    }

    public boolean overlaps(Integer minDifficulty, Integer maxDifficulty) {
        Objects.requireNonNull(key, "key must not be null");
        int effectiveMin = minDifficulty == null ? Integer.MIN_VALUE : minDifficulty;
        int effectiveMax = maxDifficulty == null ? Integer.MAX_VALUE : maxDifficulty;
        int bucketMax = toInclusive == null ? Integer.MAX_VALUE : toInclusive;
        return fromInclusive <= effectiveMax && bucketMax >= effectiveMin;
    }
}
