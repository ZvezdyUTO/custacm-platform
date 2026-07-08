package com.custacm.platform.trainingdata.atcoder.infra;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblem;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModel;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmission;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemModelPayloadParser;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemPayloadParser;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionPayloadParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public class JacksonAtcoderPayloadParser implements AtcoderSubmissionPayloadParser,
        AtcoderProblemPayloadParser,
        AtcoderProblemModelPayloadParser {
    private final ObjectMapper objectMapper;

    public JacksonAtcoderPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AtcoderOdsSubmission> parseSubmissions(String submissionPayload, AtcoderCollectBatch batch) {
        try {
            JsonNode submissions = arrayPayload(submissionPayload, "AtCoder submissions payload must be an array");
            List<AtcoderOdsSubmission> records = new ArrayList<>();
            for (JsonNode item : submissions) {
                records.add(toSubmission(item, batch));
            }
            return List.copyOf(records);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to parse AtCoder submissions", ex);
        }
    }

    @Override
    public List<AtcoderOdsProblem> parseProblems(String problemPayload, AtcoderCollectBatch batch) {
        try {
            JsonNode problems = arrayPayload(problemPayload, "AtCoder problems payload must be an array");
            List<AtcoderOdsProblem> records = new ArrayList<>();
            for (JsonNode item : problems) {
                records.add(toProblem(item, batch));
            }
            return List.copyOf(records);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to parse AtCoder problems", ex);
        }
    }

    @Override
    public List<AtcoderOdsProblemModel> parseProblemModels(String problemModelPayload, AtcoderCollectBatch batch) {
        try {
            JsonNode problemModels = objectPayload(
                    problemModelPayload,
                    "AtCoder problem models payload must be an object"
            );
            List<AtcoderOdsProblemModel> records = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = problemModels.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                records.add(toProblemModel(field.getKey(), field.getValue(), batch));
            }
            return List.copyOf(records);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to parse AtCoder problem models", ex);
        }
    }

    private JsonNode arrayPayload(String payload, String message)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode root = objectMapper.readTree(payload);
        if (!root.isArray()) {
            throw new IllegalArgumentException(message);
        }
        return root;
    }

    private JsonNode objectPayload(String payload, String message)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode root = objectMapper.readTree(payload);
        if (!root.isObject()) {
            throw new IllegalArgumentException(message);
        }
        return root;
    }

    private AtcoderOdsSubmission toSubmission(JsonNode item, AtcoderCollectBatch batch)
            throws com.fasterxml.jackson.core.JsonProcessingException, NoSuchAlgorithmException {
        String rawPayload = objectMapper.writeValueAsString(item);
        return new AtcoderOdsSubmission(
                requiredLong(item, "id"),
                requiredLong(item, "epoch_second"),
                nullableText(item, "problem_id"),
                nullableText(item, "contest_id"),
                requiredText(item, "user_id"),
                nullableText(item, "language"),
                nullableDecimal(item, "point"),
                nullableInt(item, "length"),
                nullableText(item, "result"),
                nullableInt(item, "execution_time"),
                batch.batchId(),
                batch.fetchedAt(),
                rawPayload,
                sha256(rawPayload)
        );
    }

    private AtcoderOdsProblem toProblem(JsonNode item, AtcoderCollectBatch batch)
            throws com.fasterxml.jackson.core.JsonProcessingException, NoSuchAlgorithmException {
        String rawPayload = objectMapper.writeValueAsString(item);
        return new AtcoderOdsProblem(
                requiredText(item, "id"),
                nullableText(item, "contest_id"),
                nullableText(item, "problem_index"),
                nullableText(item, "name"),
                nullableText(item, "title"),
                batch.batchId(),
                batch.fetchedAt(),
                rawPayload,
                sha256(rawPayload)
        );
    }

    private AtcoderOdsProblemModel toProblemModel(String problemId, JsonNode item, AtcoderCollectBatch batch)
            throws com.fasterxml.jackson.core.JsonProcessingException, NoSuchAlgorithmException {
        if (problemId == null || problemId.isBlank()) {
            throw new IllegalArgumentException("missing AtCoder problem model id");
        }
        if (!item.isObject()) {
            throw new IllegalArgumentException("AtCoder problem model item must be an object: " + problemId);
        }
        String rawPayload = objectMapper.writeValueAsString(item);
        Integer rawDifficulty = nullableInt(item, "difficulty");
        return new AtcoderOdsProblemModel(
                problemId,
                nullableDecimal(item, "slope"),
                nullableDecimal(item, "intercept"),
                nullableDecimal(item, "variance"),
                rawDifficulty,
                clipDifficulty(rawDifficulty),
                nullableDecimal(item, "discrimination"),
                nullableDecimal(item, "irt_loglikelihood"),
                nullableInt(item, "irt_users"),
                nullableBoolean(item, "is_experimental"),
                batch.batchId(),
                batch.fetchedAt(),
                rawPayload,
                sha256(rawPayload)
        );
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("missing AtCoder field: " + fieldName);
        }
        return value.asText();
    }

    private static Long requiredLong(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("missing AtCoder field: " + fieldName);
        }
        long parsedValue = value.asLong();
        if (!Long.toString(parsedValue).equals(value.asText())) {
            throw new IllegalArgumentException("invalid AtCoder numeric field: " + fieldName);
        }
        return parsedValue;
    }

    private static String nullableText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static Integer nullableInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private static BigDecimal nullableDecimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.decimalValue();
    }

    private static Boolean nullableBoolean(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asBoolean();
    }

    private static Integer clipDifficulty(Integer rawDifficulty) {
        if (rawDifficulty == null) {
            return null;
        }
        if (rawDifficulty >= 400) {
            return rawDifficulty;
        }
        return Math.round((float) (400 / Math.exp(1.0 - rawDifficulty / 400.0)));
    }

    private static String sha256(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
