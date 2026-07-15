package com.custacm.platform.trainingdata.atcoder.domain;

import com.fasterxml.jackson.databind.JsonNode;

public interface AtcoderSubmissionSourceClient {
    int USER_SUBMISSIONS_PAGE_LIMIT = 500;

    JsonNode fetchUserSubmissions(String userId, long fromSecond);
}
