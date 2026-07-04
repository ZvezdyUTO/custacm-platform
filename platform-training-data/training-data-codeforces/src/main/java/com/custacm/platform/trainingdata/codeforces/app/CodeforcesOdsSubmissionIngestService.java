package com.custacm.platform.trainingdata.codeforces.app;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.infra.CodeforcesSubmissionParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
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
        this(parser, writer, objectMapper, Clock.systemUTC());
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
        if (submissions == null || !submissions.isArray()) {
            throw new IllegalArgumentException("Codeforces submissions body must be a JSON array");
        }
        CodeforcesCollectBatch batch = newBatch();
        var records = parser.parse(objectMapper.writeValueAsString(submissions), batch);
        writer.upsertBatch(batch, records);
        return new CodeforcesOdsBatchUpsertResult(
                batch.batchId(),
                TABLE_NAME,
                records.size(),
                batch.fetchedAt()
        );
    }

    private CodeforcesCollectBatch newBatch() {
        Instant fetchedAt = clock.instant();
        return new CodeforcesCollectBatch(
                "external-codeforces-" + fetchedAt.toEpochMilli() + "-" + UUID.randomUUID(),
                fetchedAt
        );
    }
}
