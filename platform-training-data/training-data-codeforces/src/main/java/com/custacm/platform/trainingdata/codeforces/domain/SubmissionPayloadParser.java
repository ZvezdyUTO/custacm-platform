package com.custacm.platform.trainingdata.codeforces.domain;


import java.util.List;

public interface SubmissionPayloadParser {
    List<CodeforcesOdsSubmission> parse(String submissionPayload, CodeforcesCollectBatch batch);

    List<CodeforcesOdsSubmission> parseForHandle(
            String submissionPayload,
            CodeforcesCollectBatch batch,
            String handle
    );
}
