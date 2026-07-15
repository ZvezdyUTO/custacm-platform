package com.custacm.platform.trainingdata.common.domain.oj.model;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

/**
 * Accepted-problem aggregate for one handle and normalized difficulty key.
 *
 * @author huangbingrui.awa
 */
public record OjRatingAcceptedSummary(
        String authorHandle,
        String difficultyKey,
        int acceptedProblemCount
) {
    public OjRatingAcceptedSummary {
        authorHandle = requireText(authorHandle, "authorHandle");
        difficultyKey = requireText(difficultyKey, "difficultyKey");
        if (acceptedProblemCount < 0) {
            throw new IllegalArgumentException("acceptedProblemCount must not be negative");
        }
    }
}
