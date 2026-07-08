package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemSourceClient;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class RestClientAtcoderSourceClient implements AtcoderSubmissionSourceClient, AtcoderProblemSourceClient {
    private final RestClient restClient;

    public RestClientAtcoderSourceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public JsonNode fetchUserSubmissions(String userId, long fromSecond) {
        String normalizedUserId = requireText(userId, "userId");
        if (fromSecond < 0) {
            throw new IllegalArgumentException("fromSecond must not be negative");
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/atcoder/atcoder-api/v3/user/submissions")
                            .queryParam("user", normalizedUserId)
                            .queryParam("from_second", fromSecond)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            return resultArray(response, "AtCoder user submissions response must be a JSON array");
        } catch (AtcoderApiException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AtcoderApiException(
                    AtcoderApiException.ErrorCode.ATCODER_API_REQUEST_FAILED,
                    "failed to request AtCoder user submissions",
                    ex
            );
        }
    }

    @Override
    public JsonNode fetchProblems() {
        try {
            JsonNode response = restClient.get()
                    .uri("/atcoder/resources/problems.json")
                    .retrieve()
                    .body(JsonNode.class);
            return resultArray(response, "AtCoder problem list response must be a JSON array");
        } catch (AtcoderApiException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AtcoderApiException(
                    AtcoderApiException.ErrorCode.ATCODER_API_REQUEST_FAILED,
                    "failed to request AtCoder problem list",
                    ex
            );
        }
    }

    private static JsonNode resultArray(JsonNode response, String invalidMessage) {
        if (response == null || !response.isArray()) {
            throw new AtcoderApiException(
                    AtcoderApiException.ErrorCode.ATCODER_API_RESPONSE_INVALID,
                    invalidMessage
            );
        }
        return response;
    }

}
