package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblem;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmission;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmissionWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderProblemSourceClient;
import com.custacm.platform.trainingdata.atcoder.infra.JacksonAtcoderPayloadParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
        FakeProblemSourceClient sourceClient = new FakeProblemSourceClient(problemPage());
        RecordingProblemWriter writer = new RecordingProblemWriter();
        AtcoderProblemListCollectionService service = service(sourceClient, writer);

        var result = service.collectProblems();

        assertThat(result.tableName()).isEqualTo("ods_atcoder__problem");
        assertThat(result.batchId()).startsWith("collector-atcoder-problems-");
        assertThat(result.writtenRows()).isEqualTo(1);
        assertThat(sourceClient.calls).isEqualTo(1);
        assertThat(writer.records).singleElement().satisfies(problem -> {
            assertThat(problem.problemId()).isEqualTo("abc121_c");
            assertThat(problem.title()).isEqualTo("ABC121 C - Energy Drink Collector");
        });
    }

    private AtcoderProblemListCollectionService service(
            FakeProblemSourceClient sourceClient,
            RecordingProblemWriter writer
    ) {
        JacksonAtcoderPayloadParser parser = new JacksonAtcoderPayloadParser(objectMapper);
        AtcoderOdsIngestService ingestService = new AtcoderOdsIngestService(
                parser,
                parser,
                new NoopSubmissionWriter(),
                writer,
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

    private static final class FakeProblemSourceClient implements AtcoderProblemSourceClient {
        private final JsonNode response;
        private int calls;

        private FakeProblemSourceClient(JsonNode response) {
            this.response = response;
        }

        @Override
        public JsonNode fetchProblems() {
            calls++;
            return response;
        }
    }

    private static final class RecordingProblemWriter implements AtcoderOdsProblemWriter {
        private final List<AtcoderOdsProblem> records = new ArrayList<>();

        @Override
        public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblem> problems) {
            records.addAll(problems);
        }
    }

    private static final class NoopSubmissionWriter implements AtcoderOdsSubmissionWriter {
        @Override
        public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsSubmission> submissions) {
        }
    }
}
