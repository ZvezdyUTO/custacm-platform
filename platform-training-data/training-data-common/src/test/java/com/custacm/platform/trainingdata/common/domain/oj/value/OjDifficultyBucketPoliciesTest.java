package com.custacm.platform.trainingdata.common.domain.oj.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OjDifficultyBucketPoliciesTest {
    private final OjDifficultyBucketPolicies policies = OjDifficultyBucketPolicies.defaults();

    @Test
    void atcoderUsesAtcoderProblemsDifficultySegments() {
        OjDifficultyBucketPolicy policy = policies.policyFor(OjNames.ATCODER);

        assertThat(policy.ratedBucketKeys()).containsExactly(
                "0",
                "400",
                "800",
                "1200",
                "1600",
                "2000",
                "2400",
                "2800+"
        );
        assertThat(policy.bucketKeysInRange(1200, 1999)).containsExactly(
                "1200",
                "1600"
        );
        assertThat(policy.includesUnrated(null, null)).isTrue();
        assertThat(policy.includesUnrated(0, null)).isFalse();
    }

    @Test
    void codeforcesKeepsExactHundredRatingBuckets() {
        OjDifficultyBucketPolicy policy = policies.policyFor(OjNames.CODEFORCES);

        assertThat(policy.bucketKeysInRange(850, 1150)).containsExactly(
                "900",
                "1000",
                "1100"
        );
    }
}
