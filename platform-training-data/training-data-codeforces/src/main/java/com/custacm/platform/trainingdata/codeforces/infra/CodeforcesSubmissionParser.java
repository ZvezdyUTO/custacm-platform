package com.custacm.platform.trainingdata.codeforces.infra;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmission;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class CodeforcesSubmissionParser {
    private final ObjectMapper objectMapper;

    public CodeforcesSubmissionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CodeforcesOdsSubmission> parse(String apiResponse, CodeforcesCollectBatch batch) {
        try {
            JsonNode root = objectMapper.readTree(apiResponse);
            JsonNode submissions = submissions(root);
            List<CodeforcesOdsSubmission> records = new ArrayList<>();
            for (JsonNode item : submissions) {
                records.add(toSubmission(item, batch));
            }
            return List.copyOf(records);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to parse Codeforces submissions", ex);
        }
    }

    private JsonNode submissions(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        if (!"OK".equals(root.path("status").asText())) {
            throw new IllegalArgumentException("Codeforces response status is not OK");
        }
        return root.withArray("result");
    }

    private CodeforcesOdsSubmission toSubmission(JsonNode item, CodeforcesCollectBatch batch)
            throws com.fasterxml.jackson.core.JsonProcessingException, NoSuchAlgorithmException {
        String rawPayload = objectMapper.writeValueAsString(item);
        JsonNode problem = item.path("problem");
        JsonNode author = item.path("author");
        return new CodeforcesOdsSubmission(
                requiredLong(item, "id"),
                nullableLong(item, "contestId"),
                nullableLong(item, "creationTimeSeconds"),
                nullableInt(item, "relativeTimeSeconds"),
                nullableLong(problem, "contestId"),
                nullableText(problem, "index"),
                nullableText(problem, "name"),
                nullableText(problem, "type"),
                nullableDecimal(problem, "points"),
                nullableInt(problem, "rating"),
                nullableJson(problem.path("tags")),
                firstMemberHandle(item),
                nullableText(author, "participantType"),
                nullableJson(author),
                nullableText(item, "programmingLanguage"),
                nullableText(item, "verdict"),
                nullableText(item, "testset"),
                nullableInt(item, "passedTestCount"),
                nullableInt(item, "timeConsumedMillis"),
                nullableLong(item, "memoryConsumedBytes"),
                batch.batchId(),
                batch.fetchedAt(),
                rawPayload,
                sha256(rawPayload)
        );
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("missing Codeforces field: " + fieldName);
        }
        return value.asText();
    }

    private static String firstMemberHandle(JsonNode item) {
        JsonNode members = item.path("author").path("members");
        if (!members.isArray() || members.isEmpty()) {
            throw new IllegalArgumentException("missing Codeforces author member handle");
        }
        return requiredText(members.get(0), "handle");
    }

    private static Long requiredLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("missing Codeforces field: " + fieldName);
        }
        return value.asLong();
    }

    private static String nullableText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static Integer nullableInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private static Long nullableLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private static BigDecimal nullableDecimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.decimalValue();
    }

    private String nullableJson(JsonNode node) throws com.fasterxml.jackson.core.JsonProcessingException {
        return node.isMissingNode() || node.isNull() ? null : objectMapper.writeValueAsString(node);
    }

    private static String sha256(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
