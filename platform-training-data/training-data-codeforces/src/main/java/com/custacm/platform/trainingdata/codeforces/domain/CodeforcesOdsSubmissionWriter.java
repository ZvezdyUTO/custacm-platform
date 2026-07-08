package com.custacm.platform.trainingdata.codeforces.domain;


import java.util.List;

public interface CodeforcesOdsSubmissionWriter {
    void upsertBatch(CodeforcesCollectBatch batch, List<CodeforcesOdsSubmission> submissions);
}
