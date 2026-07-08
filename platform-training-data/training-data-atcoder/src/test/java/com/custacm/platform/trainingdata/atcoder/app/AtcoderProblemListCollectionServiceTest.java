package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblem;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModel;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModelWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmission;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmissionWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemSourceClient;
import com.custacm.platform.trainingdata.atcoder.infra.JacksonAtcoderPayloadParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AtcoderProblemListCollectionServiceTest {
    private static final Instant FETCHED_AT = Instant.parse("2026-07-07T01:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void collectsProblemListIntoOds() throws Exception {
        FakeProblemSourceClient sourceClient = new FakeProblemSourceClient(problemPage(), problemModels());
        RecordingProblemWriter writer = new RecordingProblemWriter();
        RecordingProblemModelWriter problemModelWriter = new RecordingProblemModelWriter();
        AtcoderProblemListCollectionService service = service(sourceClient, writer, problemModelWriter);

        var result = service.collectProblems();

        assertThat(result.problemResult().tableName()).isEqualTo("ods_atcoder__problem");
        assertThat(result.problemResult().batchId()).startsWith("collector-atcoder-problems-");
        assertThat(result.problemModelResult().tableName()).isEqualTo("ods_atcoder__problem_model");
        assertThat(result.problemModelResult().batchId()).startsWith("collector-atcoder-problem-models-");
        assertThat(result.writtenRows()).isEqualTo(2);
        assertThat(sourceClient.problemCalls).isEqualTo(1);
        assertThat(sourceClient.problemModelCalls).isEqualTo(1);
        assertThat(writer.records).singleElement().satisfies(problem -> {
            assertThat(problem.problemId()).isEqualTo("abc121_c");
            assertThat(problem.title()).isEqualTo("ABC121 C - Energy Drink Collector");
        });
        assertThat(problemModelWriter.records).singleElement().satisfies(problemModel -> {
            assertThat(problemModel.problemId()).isEqualTo("abc121_c");
            assertThat(problemModel.rawDifficulty()).isEqualTo(873);
            assertThat(problemModel.clippedDifficulty()).isEqualTo(873);
        });
    }

    private AtcoderProblemListCollectionService service(
            FakeProblemSourceClient sourceClient,
            RecordingProblemWriter writer,
            RecordingProblemModelWriter problemModelWriter
    ) {
        JacksonAtcoderPayloadParser parser = new JacksonAtcoderPayloadParser(objectMapper);
        AtcoderOdsIngestService ingestService = new AtcoderOdsIngestService(
                parser,
                parser,
                parser,
                new NoopSubmissionWriter(),
                writer,
                problemModelWriter,
                objectMapper,
                Clock.fixed(FETCHED_AT, ZoneId.of("Asia/Shanghai"))
        );
        return new AtcoderProblemListCollectionService(
                sourceClient,
                ingestService,
                3,
                Duration.ZERO,
                duration -> {
                }
        );
    }

    private ArrayNode problemPage() {
        ArrayNode problems = objectMapper.createArrayNode();
        problems.add(objectMapper.createObjectNode()
                .put("id", "abc121_c")
                .put("contest_id", "abc121")
                .put("problem_index", "C")
                .put("name", "Energy Drink Collector")
                .put("title", "ABC121 C - Energy Drink Collector"));
        return problems;
    }

    private ObjectNode problemModels() {
        ObjectNode problemModels = objectMapper.createObjectNode();
        problemModels.set("abc121_c", objectMapper.createObjectNode()
                .put("slope", -0.001)
                .put("intercept", 5.5)
                .put("variance", 0.9)
                .put("difficulty", 873)
                .put("discrimination", 0.004)
                .put("irt_loglikelihood", -260.5)
                .put("irt_users", 4554)
                .put("is_experimental", false));
        return problemModels;
    }

    private static final class FakeProblemSourceClient implements AtcoderProblemSourceClient {
        private final JsonNode problemResponse;
        private final JsonNode problemModelResponse;
        private int problemCalls;
        private int problemModelCalls;

        private FakeProblemSourceClient(JsonNode problemResponse, JsonNode problemModelResponse) {
            this.problemResponse = problemResponse;
            this.problemModelResponse = problemModelResponse;
        }

        @Override
        public JsonNode fetchProblems() {
            problemCalls++;
            return problemResponse;
        }

        @Override
        public JsonNode fetchProblemModels() {
            problemModelCalls++;
            return problemModelResponse;
        }
    }

    private static final class RecordingProblemWriter implements AtcoderOdsProblemWriter {
        private final List<AtcoderOdsProblem> records = new ArrayList<>();

        @Override
        public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblem> problems) {
            records.addAll(problems);
        }
    }

    private static final class RecordingProblemModelWriter implements AtcoderOdsProblemModelWriter {
        private final List<AtcoderOdsProblemModel> records = new ArrayList<>();

        @Override
        public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblemModel> problemModels) {
            records.addAll(problemModels);
        }
    }

    private static final class NoopSubmissionWriter implements AtcoderOdsSubmissionWriter {
        @Override
        public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsSubmission> submissions) {
        }
    }
}
