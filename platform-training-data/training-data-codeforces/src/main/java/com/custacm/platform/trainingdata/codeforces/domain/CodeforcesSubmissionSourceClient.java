package com.custacm.platform.trainingdata.codeforces.domain;

import com.fasterxml.jackson.databind.JsonNode;

public interface CodeforcesSubmissionSourceClient {
    JsonNode fetchUserStatus(String handle, int from, int count);
}
