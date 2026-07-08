package com.custacm.platform.trainingdata.atcoder.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientAtcoderSourceClientTest {
    private HttpServer server;
    private final List<String> requestUris = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesUserSubmissionsArray() throws Exception {
        startServer("""
                [
                  { "id": 1, "epoch_second": 1560170952 },
                  { "id": 2, "epoch_second": 1560170960 }
                ]
                """);
        RestClientAtcoderSourceClient client = client();

        JsonNode result = client.fetchUserSubmissions("tourist", 1560170952L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).path("id").asLong()).isEqualTo(1L);
        assertThat(requestUris.getFirst())
                .contains("/atcoder/atcoder-api/v3/user/submissions")
                .contains("user=tourist")
                .contains("from_second=1560170952");
    }

    @Test
    void fetchesProblemArray() throws Exception {
        startServer("""
                [
                  { "id": "abc121_c" }
                ]
                """);
        RestClientAtcoderSourceClient client = client();

        JsonNode result = client.fetchProblems();

        assertThat(result).singleElement().satisfies(problem ->
                assertThat(problem.path("id").asText()).isEqualTo("abc121_c"));
        assertThat(requestUris.getFirst()).contains("/atcoder/resources/problems.json");
    }

    @Test
    void fetchesProblemModelObject() throws Exception {
        startServer("""
                {
                  "abc121_c": { "difficulty": 873, "is_experimental": false }
                }
                """);
        RestClientAtcoderSourceClient client = client();

        JsonNode result = client.fetchProblemModels();

        assertThat(result.path("abc121_c").path("difficulty").asInt()).isEqualTo(873);
        assertThat(requestUris.getFirst()).contains("/atcoder/resources/problem-models.json");
    }

    @Test
    void rejectsInvalidResponseShape() throws Exception {
        startServer("""
                { "status": "OK" }
                """);
        RestClientAtcoderSourceClient client = client();

        assertThatThrownBy(() -> client.fetchProblems())
                .isInstanceOfSatisfying(AtcoderApiException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                AtcoderApiException.ErrorCode.ATCODER_API_RESPONSE_INVALID
                        ));
    }

    @Test
    void wrapsTransportFailures() throws Exception {
        startServer("[]");
        int port = server.getAddress().getPort();
        server.stop(0);
        server = null;
        RestClientAtcoderSourceClient client = new RestClientAtcoderSourceClient(
                RestClient.builder()
                        .baseUrl("http://127.0.0.1:" + port)
                        .build()
        );

        assertThatThrownBy(client::fetchProblems)
                .isInstanceOfSatisfying(AtcoderApiException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                AtcoderApiException.ErrorCode.ATCODER_API_REQUEST_FAILED
                        ));
    }

    private RestClientAtcoderSourceClient client() {
        return new RestClientAtcoderSourceClient(RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build());
    }

    private void startServer(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestUris.add(exchange.getRequestURI().toString());
            writeJson(exchange, responseBody);
        });
        server.start();
    }

    private static void writeJson(HttpExchange exchange, String responseBody) throws IOException {
        byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
