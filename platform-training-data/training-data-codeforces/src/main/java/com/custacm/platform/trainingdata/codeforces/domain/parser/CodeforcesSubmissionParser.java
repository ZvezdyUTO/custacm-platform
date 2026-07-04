package com.custacm.platform.trainingdata.codeforces.domain.parser;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesOdsSubmission;

import java.util.List;

public interface CodeforcesSubmissionParser {
    List<CodeforcesOdsSubmission> parse(String apiResponse, CodeforcesCollectBatch batch);
}
