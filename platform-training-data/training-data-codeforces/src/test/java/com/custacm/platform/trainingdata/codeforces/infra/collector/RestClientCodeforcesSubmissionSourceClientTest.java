package com.custacm.platform.trainingdata.codeforces.infra.collector;

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

class RestClientCodeforcesSubmissionSourceClientTest {
    private HttpServer server;
    private final List<String> requestUris = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesUserStatusResultArray() throws Exception {
        startServer("""
                {
                  "status": "OK",
                  "result": [
                    { "id": 1 },
                    { "id": 2 }
                  ]
                }
                """);
        RestClientCodeforcesSubmissionSourceClient client = client();

        JsonNode result = client.fetchUserStatus("tourist", 1, 1000);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).path("id").asLong()).isEqualTo(1L);
        assertThat(requestUris.getFirst())
                .contains("/api/user.status")
                .contains("handle=tourist")
                .contains("from=1")
                .contains("count=1000");
    }

    @Test
    void rejectsCodeforcesFailedStatus() throws Exception {
        startServer("""
                {
                  "status": "FAILED",
                  "comment": "handle: User with handle tourist not found"
                }
                """);
        RestClientCodeforcesSubmissionSourceClient client = client();

        assertThatThrownBy(() -> client.fetchUserStatus("tourist", 1, 1000))
                .isInstanceOfSatisfying(CodeforcesApiException.class, ex -> {
                    assertThat(ex.errorCode()).isEqualTo(
                            CodeforcesApiException.ErrorCode.CODEFORCES_API_STATUS_FAILED
                    );
                    assertThat(ex.getMessage()).contains("not found");
                });
    }

    @Test
    void rejectsInvalidResultShape() throws Exception {
        startServer("""
                {
                  "status": "OK",
                  "result": {}
                }
                """);
        RestClientCodeforcesSubmissionSourceClient client = client();

        assertThatThrownBy(() -> client.fetchUserStatus("tourist", 1, 1000))
                .isInstanceOfSatisfying(CodeforcesApiException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesApiException.ErrorCode.CODEFORCES_API_RESPONSE_INVALID
                        ));
    }

    @Test
    void wrapsTransportFailures() throws Exception {
        startServer("""
                {
                  "status": "OK",
                  "result": []
                }
                """);
        int port = server.getAddress().getPort();
        server.stop(0);
        server = null;
        RestClientCodeforcesSubmissionSourceClient client = new RestClientCodeforcesSubmissionSourceClient(
                RestClient.builder()
                        .baseUrl("http://127.0.0.1:" + port)
                        .build()
        );

        assertThatThrownBy(() -> client.fetchUserStatus("tourist", 1, 1000))
                .isInstanceOfSatisfying(CodeforcesApiException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesApiException.ErrorCode.CODEFORCES_API_REQUEST_FAILED
                        ));
    }

    private RestClientCodeforcesSubmissionSourceClient client() {
        return new RestClientCodeforcesSubmissionSourceClient(RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .build());
    }

    private void startServer(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/user.status", exchange -> {
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
