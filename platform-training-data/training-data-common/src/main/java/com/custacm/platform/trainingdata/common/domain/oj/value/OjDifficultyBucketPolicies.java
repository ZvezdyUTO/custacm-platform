package com.custacm.platform.trainingdata.common.domain.oj.value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class OjDifficultyBucketPolicies {
    public static final String UNRATED_KEY = "UNRATED";

    private final Map<String, OjDifficultyBucketPolicy> policiesByOjName;

    public OjDifficultyBucketPolicies(List<OjDifficultyBucketPolicy> policies) {
        LinkedHashMap<String, OjDifficultyBucketPolicy> normalizedPolicies = new LinkedHashMap<>();
        for (OjDifficultyBucketPolicy policy : policies) {
            normalizedPolicies.put(policy.ojName(), policy);
        }
        this.policiesByOjName = Map.copyOf(normalizedPolicies);
    }

    public static OjDifficultyBucketPolicies defaults() {
        return new OjDifficultyBucketPolicies(List.of(
                codeforcesPolicy(),
                atcoderPolicy()
        ));
    }

    public OjDifficultyBucketPolicy policyFor(String ojName) {
        String normalizedOjName = OjNames.normalize(ojName);
        OjDifficultyBucketPolicy policy = policiesByOjName.get(normalizedOjName);
        if (policy == null) {
            throw new IllegalArgumentException("missing difficulty bucket policy for oj: " + ojName);
        }
        return policy;
    }

    private static OjDifficultyBucketPolicy codeforcesPolicy() {
        return new OjDifficultyBucketPolicy(
                OjNames.CODEFORCES,
                IntStream.iterate(800, rating -> rating <= 3500, rating -> rating + 100)
                        .mapToObj(rating -> OjDifficultyBucketPolicy.exact(Integer.toString(rating), rating))
                        .toList()
        );
    }

    private static OjDifficultyBucketPolicy atcoderPolicy() {
        return new OjDifficultyBucketPolicy(
                OjNames.ATCODER,
                List.of(
                        OjDifficultyBucketPolicy.range("0", 0, 399),
                        OjDifficultyBucketPolicy.range("400", 400, 799),
                        OjDifficultyBucketPolicy.range("800", 800, 1199),
                        OjDifficultyBucketPolicy.range("1200", 1200, 1599),
                        OjDifficultyBucketPolicy.range("1600", 1600, 1999),
                        OjDifficultyBucketPolicy.range("2000", 2000, 2399),
                        OjDifficultyBucketPolicy.range("2400", 2400, 2799),
                        OjDifficultyBucketPolicy.openEnded("2800+", 2800)
                )
        );
    }
}
