package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmissionWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemPayloadParser;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionPayloadParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public class AtcoderOdsIngestService {
    private static final String SUBMISSION_TABLE_NAME = "ods_atcoder__submission";
    private static final String PROBLEM_TABLE_NAME = "ods_atcoder__problem";

    private final AtcoderSubmissionPayloadParser submissionParser;
    private final AtcoderProblemPayloadParser problemParser;
    private final AtcoderOdsSubmissionWriter submissionWriter;
    private final AtcoderOdsProblemWriter problemWriter;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AtcoderOdsIngestService(
            AtcoderSubmissionPayloadParser submissionParser,
            AtcoderProblemPayloadParser problemParser,
            AtcoderOdsSubmissionWriter submissionWriter,
            AtcoderOdsProblemWriter problemWriter,
            ObjectMapper objectMapper
    ) {
        this(submissionParser, problemParser, submissionWriter, problemWriter, objectMapper,
                Clock.system(ZoneOffset.ofHours(8)));
    }

    public AtcoderOdsIngestService(
            AtcoderSubmissionPayloadParser submissionParser,
            AtcoderProblemPayloadParser problemParser,
            AtcoderOdsSubmissionWriter submissionWriter,
            AtcoderOdsProblemWriter problemWriter,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.submissionParser = submissionParser;
        this.problemParser = problemParser;
        this.submissionWriter = submissionWriter;
        this.problemWriter = problemWriter;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public AtcoderOdsBatchUpsertResult upsertSubmissions(JsonNode submissions, String batchIdPrefix)
            throws JsonProcessingException {
        if (submissions == null || !submissions.isArray()) {
            throw new IllegalArgumentException("AtCoder submissions body must be a JSON array");
        }
        AtcoderCollectBatch batch = newBatch(batchIdPrefix);
        var records = submissionParser.parseSubmissions(objectMapper.writeValueAsString(submissions), batch);
        submissionWriter.upsertBatch(batch, records);
        return new AtcoderOdsBatchUpsertResult(
                batch.batchId(),
                SUBMISSION_TABLE_NAME,
                records.size(),
                batch.fetchedAt()
        );
    }

    public AtcoderOdsBatchUpsertResult upsertProblems(JsonNode problems, String batchIdPrefix)
            throws JsonProcessingException {
        if (problems == null || !problems.isArray()) {
            throw new IllegalArgumentException("AtCoder problems body must be a JSON array");
        }
        AtcoderCollectBatch batch = newBatch(batchIdPrefix);
        var records = problemParser.parseProblems(objectMapper.writeValueAsString(problems), batch);
        problemWriter.upsertBatch(batch, records);
        return new AtcoderOdsBatchUpsertResult(
                batch.batchId(),
                PROBLEM_TABLE_NAME,
                records.size(),
                batch.fetchedAt()
        );
    }

    private AtcoderCollectBatch newBatch(String batchIdPrefix) {
        String normalizedPrefix = requireText(batchIdPrefix, "batchIdPrefix");
        Instant fetchedAt = clock.instant();
        return new AtcoderCollectBatch(
                normalizedPrefix + "-" + fetchedAt.toEpochMilli() + "-" + UUID.randomUUID(),
                fetchedAt
        );
    }
}
