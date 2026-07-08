package com.custacm.platform.trainingdata.atcoder.domain;

import com.fasterxml.jackson.databind.JsonNode;

public interface AtcoderSubmissionSourceClient {
    JsonNode fetchUserSubmissions(String userId, long fromSecond);
}
