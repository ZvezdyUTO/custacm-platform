package com.custacm.platform.trainingdata.common.domain.oj.repo;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjFirstAcceptedProblem;

import java.util.List;

public interface OjFirstAcceptedProblemRepository {
    List<OjFirstAcceptedProblem> findHandleFirstAcceptedProblems(
            OjHandleFirstAcceptedProblemCriteria query
    );

    List<OjFirstAcceptedProblem> findProblemFirstAcceptedHandles(
            OjProblemFirstAcceptedHandleCriteria query
    );
}
