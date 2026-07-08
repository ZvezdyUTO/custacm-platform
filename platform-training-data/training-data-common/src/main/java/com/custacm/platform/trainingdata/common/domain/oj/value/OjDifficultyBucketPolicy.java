package com.custacm.platform.trainingdata.common.domain.oj.value;

import java.util.List;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record OjDifficultyBucketPolicy(
        String ojName,
        List<OjDifficultyBucket> ratedBuckets
) {
    public OjDifficultyBucketPolicy {
        ojName = OjNames.normalize(ojName);
        ratedBuckets = List.copyOf(ratedBuckets);
        if (ratedBuckets.isEmpty()) {
            throw new IllegalArgumentException("ratedBuckets must not be empty");
        }
    }

    public List<String> ratedBucketKeys() {
        return ratedBuckets.stream()
                .map(OjDifficultyBucket::key)
                .toList();
    }

    public List<String> bucketKeysInRange(Integer minDifficulty, Integer maxDifficulty) {
        if (minDifficulty == null && maxDifficulty == null) {
            return null;
        }
        return ratedBuckets.stream()
                .filter(bucket -> bucket.overlaps(minDifficulty, maxDifficulty))
                .map(OjDifficultyBucket::key)
                .toList();
    }

    public boolean includesUnrated(Integer minDifficulty, Integer maxDifficulty) {
        return minDifficulty == null && maxDifficulty == null;
    }

    public static OjDifficultyBucket exact(String key, int difficulty) {
        return new OjDifficultyBucket(requireText(key, "key"), difficulty, difficulty);
    }

    public static OjDifficultyBucket range(String key, int fromInclusive, int toInclusive) {
        return new OjDifficultyBucket(key, fromInclusive, toInclusive);
    }

    public static OjDifficultyBucket openEnded(String key, int fromInclusive) {
        return new OjDifficultyBucket(key, fromInclusive, null);
    }
}
