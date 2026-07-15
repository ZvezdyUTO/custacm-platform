package com.custacm.platform.trainingdata.common.domain.oj.repo;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjRatingAcceptedSummary;

import java.util.List;

public interface OjAcceptedSummaryRepository {
    List<OjRatingAcceptedSummary> summarizeAcceptedProblemsByRating(
            OjAcceptedSummaryCriteria query
    );

    List<OjRatingAcceptedSummary> summarizeAcceptedProblemsByRating(
            List<OjAcceptedSummaryCriteria> queries
    );
}
