package com.custacm.platform.trainingdata.codeforces.domain.repo;

import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesFirstAcceptedProblem;

import java.util.List;

public interface CodeforcesFirstAcceptedProblemRepository {
    List<CodeforcesFirstAcceptedProblem> findHandleFirstAcceptedProblems(
            CodeforcesHandleFirstAcceptedProblemCriteria query
    );

    List<CodeforcesFirstAcceptedProblem> findProblemFirstAcceptedHandles(
            CodeforcesProblemFirstAcceptedHandleCriteria query
    );
}
