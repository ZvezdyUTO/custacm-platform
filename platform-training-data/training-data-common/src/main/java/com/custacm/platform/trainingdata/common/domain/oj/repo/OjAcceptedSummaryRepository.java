package com.custacm.platform.trainingdata.common.domain.oj.repo;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjDailyRatingAcceptedSummary;

import java.util.List;

public interface OjAcceptedSummaryRepository {
    List<OjDailyRatingAcceptedSummary> findDailyRatingAcceptedSummaries(
            OjAcceptedSummaryCriteria query
    );

    List<OjDailyRatingAcceptedSummary> findDailyRatingAcceptedSummaries(
            List<OjAcceptedSummaryCriteria> queries
    );
}
