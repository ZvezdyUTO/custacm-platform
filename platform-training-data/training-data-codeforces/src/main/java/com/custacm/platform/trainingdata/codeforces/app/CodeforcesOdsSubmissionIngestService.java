package com.custacm.platform.trainingdata.codeforces.app;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.SubmissionPayloadParser;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmissionWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class CodeforcesOdsSubmissionIngestService {
    private static final String TABLE_NAME = "ods_codeforces__submission";

    private final SubmissionPayloadParser parser;
    private final CodeforcesOdsSubmissionWriter writer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CodeforcesOdsSubmissionIngestService(
            SubmissionPayloadParser parser,
            CodeforcesOdsSubmissionWriter writer,
            ObjectMapper objectMapper
    ) {
        this(parser, writer, objectMapper, Clock.system(ZoneOffset.ofHours(8)));
    }

    public CodeforcesOdsSubmissionIngestService(
            SubmissionPayloadParser parser,
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

    public CodeforcesOdsBatchUpsertResult upsertSubmissions(
            Map<String, ? extends JsonNode> submissionsByHandle,
            String batchIdPrefix
    ) throws JsonProcessingException {
        return upsertSubmissions(submissionsByHandle, newBatch(batchIdPrefix));
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

    public CodeforcesOdsBatchUpsertResult upsertSubmissions(
            Map<String, ? extends JsonNode> submissionsByHandle,
            CodeforcesCollectBatch batch
    ) throws JsonProcessingException {
        if (submissionsByHandle == null) {
            throw new IllegalArgumentException("Codeforces submissions by handle must not be null");
        }
        List<CodeforcesOdsSubmission> records = new ArrayList<>();
        for (Map.Entry<String, ? extends JsonNode> entry : submissionsByHandle.entrySet()) {
            String handle = requireText(entry.getKey(), "handle");
            JsonNode submissions = entry.getValue();
            if (submissions == null || !submissions.isArray()) {
                throw new IllegalArgumentException("Codeforces submissions body must be a JSON array");
            }
            records.addAll(parser.parseForHandle(objectMapper.writeValueAsString(submissions), batch, handle));
        }
        writer.upsertBatch(batch, records);
        return new CodeforcesOdsBatchUpsertResult(
                batch.batchId(),
                TABLE_NAME,
                records.size(),
                batch.fetchedAt()
        );
    }

    private CodeforcesCollectBatch newBatch(String batchIdPrefix) {
        String normalizedPrefix = requireText(batchIdPrefix, "batchIdPrefix");
        Instant fetchedAt = clock.instant();
        return new CodeforcesCollectBatch(
                normalizedPrefix + "-" + fetchedAt.toEpochMilli() + "-" + UUID.randomUUID(),
                fetchedAt
        );
    }
}
