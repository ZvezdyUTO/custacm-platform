package com.custacm.platform.trainingdata.codeforces.domain.repo;

import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesSubmission;

import java.util.List;

public interface CodeforcesSubmissionRepository {
    List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria query);

    List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria query);
}
