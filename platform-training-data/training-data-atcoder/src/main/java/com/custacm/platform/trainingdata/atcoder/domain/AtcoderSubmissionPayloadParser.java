package com.custacm.platform.trainingdata.atcoder.domain;

import java.util.List;

public interface AtcoderSubmissionPayloadParser {
    List<AtcoderOdsSubmission> parseSubmissions(String submissionPayload, AtcoderCollectBatch batch);
}
