package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModelWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmissionWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemModelPayloadParser;
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
    private static final String PROBLEM_MODEL_TABLE_NAME = "ods_atcoder__problem_model";

    private final AtcoderSubmissionPayloadParser submissionParser;
    private final AtcoderProblemPayloadParser problemParser;
    private final AtcoderProblemModelPayloadParser problemModelParser;
    private final AtcoderOdsSubmissionWriter submissionWriter;
    private final AtcoderOdsProblemWriter problemWriter;
    private final AtcoderOdsProblemModelWriter problemModelWriter;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AtcoderOdsIngestService(
            AtcoderSubmissionPayloadParser submissionParser,
            AtcoderProblemPayloadParser problemParser,
            AtcoderProblemModelPayloadParser problemModelParser,
            AtcoderOdsSubmissionWriter submissionWriter,
            AtcoderOdsProblemWriter problemWriter,
            AtcoderOdsProblemModelWriter problemModelWriter,
            ObjectMapper objectMapper
    ) {
        this(submissionParser, problemParser, problemModelParser, submissionWriter, problemWriter, problemModelWriter, objectMapper,
                Clock.system(ZoneOffset.ofHours(8)));
    }

    public AtcoderOdsIngestService(
            AtcoderSubmissionPayloadParser submissionParser,
            AtcoderProblemPayloadParser problemParser,
            AtcoderProblemModelPayloadParser problemModelParser,
            AtcoderOdsSubmissionWriter submissionWriter,
            AtcoderOdsProblemWriter problemWriter,
            AtcoderOdsProblemModelWriter problemModelWriter,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.submissionParser = submissionParser;
        this.problemParser = problemParser;
        this.problemModelParser = problemModelParser;
        this.submissionWriter = submissionWriter;
        this.problemWriter = problemWriter;
        this.problemModelWriter = problemModelWriter;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public AtcoderOdsBatchUpsertResult upsertSubmissions(JsonNode submissions, String batchIdPrefix)
            throws JsonProcessingException {
        return upsertSubmissions(submissions, newBatch(batchIdPrefix));
    }

    public AtcoderOdsBatchUpsertResult upsertSubmissions(JsonNode submissions, AtcoderCollectBatch batch)
            throws JsonProcessingException {
        if (submissions == null || !submissions.isArray()) {
            throw new IllegalArgumentException("AtCoder submissions body must be a JSON array");
        }
        var records = submissionParser.parseSubmissions(objectMapper.writeValueAsString(submissions), batch);
        submissionWriter.upsertBatch(batch, records);
        return new AtcoderOdsBatchUpsertResult(
                batch.batchId(),
                SUBMISSION_TABLE_NAME,
                records.size(),
                batch.fetchedAt()
        );
    }

    AtcoderCollectBatch startSubmissionBatch(String batchIdPrefix) {
        return newBatch(batchIdPrefix);
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

    public AtcoderOdsBatchUpsertResult upsertProblemModels(JsonNode problemModels, String batchIdPrefix)
            throws JsonProcessingException {
        if (problemModels == null || !problemModels.isObject()) {
            throw new IllegalArgumentException("AtCoder problem models body must be a JSON object");
        }
        AtcoderCollectBatch batch = newBatch(batchIdPrefix);
        var records = problemModelParser.parseProblemModels(objectMapper.writeValueAsString(problemModels), batch);
        problemModelWriter.upsertBatch(batch, records);
        return new AtcoderOdsBatchUpsertResult(
                batch.batchId(),
                PROBLEM_MODEL_TABLE_NAME,
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
