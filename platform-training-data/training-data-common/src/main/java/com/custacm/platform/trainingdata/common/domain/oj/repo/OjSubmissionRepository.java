package com.custacm.platform.trainingdata.common.domain.oj.repo;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjSubmission;

import java.util.List;

public interface OjSubmissionRepository {
    long countHandleSubmissions(OjHandleSubmissionCriteria query);

    List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria query);

    long countProblemSubmissions(OjProblemSubmissionCriteria query);

    List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria query);
}
