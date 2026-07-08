package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesSubmissionSourceClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class RestClientCodeforcesSubmissionSourceClient implements CodeforcesSubmissionSourceClient {
    private final RestClient restClient;

    public RestClientCodeforcesSubmissionSourceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public JsonNode fetchUserStatus(String handle, int from, int count) {
        String normalizedHandle = requireText(handle, "handle");
        if (from <= 0) {
            throw new IllegalArgumentException("from must be positive");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/user.status")
                            .queryParam("handle", normalizedHandle)
                            .queryParam("from", from)
                            .queryParam("count", count)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            return resultArray(response);
        } catch (CodeforcesApiException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new CodeforcesApiException(
                    CodeforcesApiException.ErrorCode.CODEFORCES_API_REQUEST_FAILED,
                    "failed to request Codeforces user.status",
                    ex
            );
        }
    }

    private static JsonNode resultArray(JsonNode response) {
        if (response == null || !response.isObject()) {
            throw invalidResponse("Codeforces response must be a JSON object");
        }
        String status = response.path("status").asText();
        if ("FAILED".equals(status)) {
            String comment = response.path("comment").asText("Codeforces API status is FAILED");
            throw new CodeforcesApiException(
                    CodeforcesApiException.ErrorCode.CODEFORCES_API_STATUS_FAILED,
                    comment
            );
        }
        if (!"OK".equals(status)) {
            throw invalidResponse("Codeforces response status is not OK");
        }
        JsonNode result = response.path("result");
        if (!result.isArray()) {
            throw invalidResponse("Codeforces response result must be an array");
        }
        return result;
    }

    private static CodeforcesApiException invalidResponse(String message) {
        return new CodeforcesApiException(
                CodeforcesApiException.ErrorCode.CODEFORCES_API_RESPONSE_INVALID,
                message
        );
    }

}
