package com.custacm.platform.trainingdata.codeforces.domain.repo;

import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesDailyRatingAcceptedSummary;

import java.util.List;

public interface CodeforcesAcceptedSummaryRepository {
    List<CodeforcesDailyRatingAcceptedSummary> findDailyRatingAcceptedSummaries(
            CodeforcesAcceptedSummaryCriteria query
    );
}
