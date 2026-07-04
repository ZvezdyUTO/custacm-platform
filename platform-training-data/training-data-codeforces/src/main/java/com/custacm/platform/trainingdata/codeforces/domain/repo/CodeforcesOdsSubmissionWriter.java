package com.custacm.platform.trainingdata.codeforces.domain.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesOdsSubmission;

import java.util.List;

public interface CodeforcesOdsSubmissionWriter {
    void upsertBatch(CodeforcesCollectBatch batch, List<CodeforcesOdsSubmission> submissions);
}
