package com.custacm.platform.trainingdata.common.domain.oj.repo;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjFirstAcceptedProblem;

import java.util.List;

public interface OjFirstAcceptedProblemRepository {
    long countHandleFirstAcceptedProblems(
            OjHandleFirstAcceptedProblemCriteria query
    );

    List<OjFirstAcceptedProblem> findHandleFirstAcceptedProblems(
            OjHandleFirstAcceptedProblemCriteria query
    );

    long countProblemFirstAcceptedHandles(
            OjProblemFirstAcceptedHandleCriteria query
    );

    List<OjFirstAcceptedProblem> findProblemFirstAcceptedHandles(
            OjProblemFirstAcceptedHandleCriteria query
    );
}
