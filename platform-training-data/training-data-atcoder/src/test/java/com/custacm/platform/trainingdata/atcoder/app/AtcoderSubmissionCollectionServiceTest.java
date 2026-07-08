package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblem;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModel;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemModelWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsProblemWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmission;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderOdsSubmissionWriter;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import com.custacm.platform.trainingdata.atcoder.infra.AtcoderApiException;
import com.custacm.platform.trainingdata.atcoder.infra.JacksonAtcoderPayloadParser;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionHandleStatus;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AtcoderSubmissionCollectionServiceTest {
    private static final Instant FETCHED_AT = Instant.parse("2026-07-05T04:00:00Z");
    private static final Instant WINDOW_START = Instant.parse("2026-06-30T04:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-07-05T04:00:00Z");
    private static final Duration LOOKBACK = Duration.between(WINDOW_START, WINDOW_END);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void collectsAtcoderSubmissionsByFromSecondUntilWindowEnd() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("tourist", page(
                submission(1, "tourist", "2026-07-01T00:00:00Z"),
                submission(2, "tourist", "2026-07-02T00:00:00Z")
        ));
        sourceClient.addPage("tourist", page(
                submission(3, "tourist", "2026-07-05T04:00:00Z")
        ));
        RecordingSubmissionWriter writer = new RecordingSubmissionWriter();
        AtcoderSubmissionCollectionService service = service(sourceClient, writer);

        var result = service.collectRecentWindowForStudentIdentity("112487张三", LOOKBACK);

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(result.ojName()).isEqualTo(OjNames.ATCODER);
        assertThat(result.requestedHandleCount()).isEqualTo(1);
        assertThat(result.fetchedSubmissionCount()).isEqualTo(3);
        assertThat(result.matchedSubmissionCount()).isEqualTo(2);
        assertThat(result.writtenRows()).isEqualTo(2);
        assertThat(result.batchId()).startsWith("collector-atcoder-");
        assertThat(sourceClient.calls).containsExactly(
                new SourceCall("tourist", WINDOW_START.getEpochSecond()),
                new SourceCall("tourist", Instant.parse("2026-07-02T00:00:00Z").getEpochSecond() + 1)
        );
        assertThat(writer.records).extracting(AtcoderOdsSubmission::atcoderSubmissionId)
                .containsExactly(1L, 2L);
    }

    @Test
    void doesNotWriteWhenNoAtcoderSubmissionsMatchWindow() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("tourist", page(submission(1, "tourist", "2026-07-05T04:00:00Z")));
        RecordingSubmissionWriter writer = new RecordingSubmissionWriter();
        AtcoderSubmissionCollectionService service = service(sourceClient, writer);

        var result = service.collectRecentWindowForStudentIdentity("112487张三", LOOKBACK);

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(result.writtenRows()).isZero();
        assertThat(result.message()).isEqualTo("No AtCoder submissions matched the collection window");
        assertThat(writer.records).isEmpty();
    }

    @Test
    void usesCeilingStartSecondForFromSecondAndWindowFiltering() throws Exception {
        Instant windowEnd = Instant.parse("2026-07-05T04:00:00.500Z");
        Instant windowStart = Instant.parse("2026-06-30T04:00:00.500Z");
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("tourist", page(
                submission(10, "tourist", "2026-06-30T04:00:00Z"),
                submission(11, "tourist", "2026-06-30T04:00:01Z")
        ));
        RecordingSubmissionWriter writer = new RecordingSubmissionWriter();
        AtcoderSubmissionCollectionService service = service(
                sourceClient,
                writer,
                Clock.fixed(windowEnd, ZoneId.of("Asia/Shanghai")),
                10
        );

        var result = service.collectRecentWindowForStudentIdentity(
                "112487张三",
                Duration.between(windowStart, windowEnd)
        );

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(sourceClient.calls).containsExactly(
                new SourceCall("tourist", Instant.parse("2026-06-30T04:00:01Z").getEpochSecond())
        );
        assertThat(writer.records).extracting(AtcoderOdsSubmission::atcoderSubmissionId)
                .containsExactly(11L);
    }

    @Test
    void mapsAtcoderSourceFailureErrorCode() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.fail(new AtcoderApiException(
                AtcoderApiException.ErrorCode.ATCODER_API_REQUEST_FAILED,
                "Kenkoooo unavailable"
        ));
        RecordingSubmissionWriter writer = new RecordingSubmissionWriter();
        AtcoderSubmissionCollectionService service = service(sourceClient, writer);

        var result = service.collectRecentWindowForStudentIdentity("112487张三", LOOKBACK);

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.FAILED);
        assertThat(result.handles()).hasSize(1);
        assertThat(result.handles().getFirst().status()).isEqualTo(OjSubmissionCollectionHandleStatus.FAILED);
        assertThat(result.handles().getFirst().errorCode()).isEqualTo("ATCODER_API_REQUEST_FAILED");
        assertThat(writer.records).isEmpty();
    }

    private AtcoderSubmissionCollectionService service(
            FakeSourceClient sourceClient,
            RecordingSubmissionWriter writer
    ) {
        return service(sourceClient, writer, Clock.fixed(FETCHED_AT, ZoneId.of("Asia/Shanghai")), 2);
    }

    private AtcoderSubmissionCollectionService service(
            FakeSourceClient sourceClient,
            RecordingSubmissionWriter writer,
            Clock clock,
            int pageSize
    ) {
        JacksonAtcoderPayloadParser parser = new JacksonAtcoderPayloadParser(objectMapper);
        AtcoderOdsIngestService ingestService = new AtcoderOdsIngestService(
                parser,
                parser,
                parser,
                writer,
                new NoopProblemWriter(),
                new NoopProblemModelWriter(),
                objectMapper,
                clock
        );
        OjHandleAccountService handleAccountService = new OjHandleAccountService(
                new FakeHandleAccountRepository(),
                clock
        );
        return new AtcoderSubmissionCollectionService(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                pageSize,
                3,
                Duration.ZERO,
                clock,
                duration -> {
                }
        );
    }

    private ArrayNode page(JsonNode... submissions) {
        ArrayNode page = objectMapper.createArrayNode();
        for (JsonNode submission : submissions) {
            page.add(submission);
        }
        return page;
    }

    private JsonNode submission(long id, String userId, String submittedAt) {
        return objectMapper.createObjectNode()
                .put("id", id)
                .put("epoch_second", Instant.parse(submittedAt).getEpochSecond())
                .put("problem_id", "abc121_c")
                .put("contest_id", "abc121")
                .put("user_id", userId)
                .put("language", "C++ 20")
                .put("point", 300.0)
                .put("length", 797)
                .put("result", "AC")
                .put("execution_time", 404);
    }

    private static final class FakeSourceClient implements AtcoderSubmissionSourceClient {
        private final Map<String, ArrayDeque<JsonNode>> pagesByHandle =
                new java.util.HashMap<>();
        private final List<SourceCall> calls = new ArrayList<>();
        private RuntimeException failure;

        private void addPage(String handle, JsonNode page) {
            pagesByHandle.computeIfAbsent(handle, ignored -> new ArrayDeque<>()).add(page);
        }

        private void fail(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public JsonNode fetchUserSubmissions(String userId, long fromSecond) {
            calls.add(new SourceCall(userId, fromSecond));
            if (failure != null) {
                throw failure;
            }
            return pagesByHandle.getOrDefault(userId, new ArrayDeque<>()).removeFirst();
        }
    }

    private record SourceCall(String handle, long fromSecond) {
    }

    private static final class RecordingSubmissionWriter implements AtcoderOdsSubmissionWriter {
        private final List<AtcoderOdsSubmission> records = new ArrayList<>();

        @Override
        public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsSubmission> submissions) {
            records.addAll(submissions);
        }
    }

    private static final class NoopProblemWriter implements AtcoderOdsProblemWriter {
        @Override
        public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblem> problems) {
        }
    }

    private static final class NoopProblemModelWriter implements AtcoderOdsProblemModelWriter {
        @Override
        public void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblemModel> problemModels) {
        }
    }

    private static final class FakeHandleAccountRepository implements OjHandleAccountRepository {
        @Override
        public List<OjHandleAccount> findAll() {
            return List.of(account());
        }

        @Override
        public Optional<OjHandleAccount> findByStudentIdentity(String studentIdentity) {
            return "112487张三".equals(studentIdentity) ? Optional.of(account()) : Optional.empty();
        }

        @Override
        public Optional<OjHandleAccount> findByHandle(String ojName, String handle) {
            return OjNames.ATCODER.equals(OjNames.normalize(ojName)) && "tourist".equals(handle)
                    ? Optional.of(account())
                    : Optional.empty();
        }

        @Override
        public OjHandleAccount save(OjHandleAccount account) {
            return account;
        }

        @Override
        public OjHandleAccount updateStudentIdentityAndNeedCollect(
                String oldStudentIdentity,
                String newStudentIdentity,
                Map<String, String> handles,
                boolean needCollect,
                Map<String, OjHandleCollectionState> collectionStates,
                Instant updatedAt
        ) {
            return new OjHandleAccount(
                    newStudentIdentity,
                    handles,
                    needCollect,
                    collectionStates,
                    FETCHED_AT,
                    updatedAt
            );
        }

        @Override
        public OjHandleAccount updateCollectionStates(
                String studentIdentity,
                Map<String, OjHandleCollectionState> collectionStates,
                Instant updatedAt
        ) {
            OjHandleAccount existing = account();
            return new OjHandleAccount(
                    existing.studentIdentity(),
                    existing.handles(),
                    existing.needCollect(),
                    collectionStates,
                    existing.createdAt(),
                    updatedAt
            );
        }

        private OjHandleAccount account() {
            return new OjHandleAccount(
                    "112487张三",
                    Map.of(OjNames.ATCODER, "tourist"),
                    true,
                    FETCHED_AT,
                    FETCHED_AT
            );
        }
    }
}
