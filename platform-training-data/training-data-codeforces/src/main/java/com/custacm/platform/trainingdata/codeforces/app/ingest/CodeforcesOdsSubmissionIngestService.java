package com.custacm.platform.trainingdata.codeforces.app.ingest;

import com.custacm.platform.trainingdata.codeforces.app.ingest.result.CodeforcesOdsBatchUpsertResult;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.parser.CodeforcesSubmissionParser;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesOdsSubmissionWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

public class CodeforcesOdsSubmissionIngestService {
    private static final String TABLE_NAME = "ods_codeforces__submission";

    private final CodeforcesSubmissionParser parser;
    private final CodeforcesOdsSubmissionWriter writer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CodeforcesOdsSubmissionIngestService(
            CodeforcesSubmissionParser parser,
            CodeforcesOdsSubmissionWriter writer,
            ObjectMapper objectMapper
    ) {
        this(parser, writer, objectMapper, Clock.system(ZoneOffset.ofHours(8)));
    }

    public CodeforcesOdsSubmissionIngestService(
            CodeforcesSubmissionParser parser,
            CodeforcesOdsSubmissionWriter writer,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.parser = parser;
        this.writer = writer;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public CodeforcesOdsBatchUpsertResult upsertSubmissions(JsonNode submissions) throws JsonProcessingException {
        return upsertSubmissions(submissions, "external-codeforces");
    }

    public CodeforcesOdsBatchUpsertResult upsertSubmissions(JsonNode submissions, String batchIdPrefix)
            throws JsonProcessingException {
        return upsertSubmissions(submissions, newBatch(batchIdPrefix));
    }

    public CodeforcesOdsBatchUpsertResult upsertSubmissions(JsonNode submissions, CodeforcesCollectBatch batch)
            throws JsonProcessingException {
        if (submissions == null || !submissions.isArray()) {
            throw new IllegalArgumentException("Codeforces submissions body must be a JSON array");
        }
        var records = parser.parse(objectMapper.writeValueAsString(submissions), batch);
        writer.upsertBatch(batch, records);
        return new CodeforcesOdsBatchUpsertResult(
                batch.batchId(),
                TABLE_NAME,
                records.size(),
                batch.fetchedAt()
        );
    }

    private CodeforcesCollectBatch newBatch(String batchIdPrefix) {
        String normalizedPrefix = requireBatchIdPrefix(batchIdPrefix);
        Instant fetchedAt = clock.instant();
        return new CodeforcesCollectBatch(
                normalizedPrefix + "-" + fetchedAt.toEpochMilli() + "-" + UUID.randomUUID(),
                fetchedAt
        );
    }

    private static String requireBatchIdPrefix(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("batchIdPrefix must not be blank");
        }
        return value.trim();
    }
}
