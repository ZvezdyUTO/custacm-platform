package com.custacm.platform.trainingdata.codeforces.app.collector;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountService;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionHandleStatus;
import com.custacm.platform.trainingdata.codeforces.app.collector.result.CodeforcesSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.codeforces.app.ingest.CodeforcesOdsSubmissionIngestService;
import com.custacm.platform.trainingdata.codeforces.domain.collector.CodeforcesSubmissionSourceClient;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesOdsSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.parser.CodeforcesSubmissionParser;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesHandleAccountRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.codeforces.infra.collector.CodeforcesApiException;
import com.custacm.platform.trainingdata.codeforces.infra.parser.JacksonCodeforcesSubmissionParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CodeforcesSubmissionCollectionServiceTest {
    private static final Instant FETCHED_AT = Instant.parse("2026-07-05T04:00:00Z");
    private static final Instant WINDOW_START = Instant.parse("2026-06-30T04:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-07-05T04:00:00Z");
    private static final Duration LOOKBACK = Duration.between(WINDOW_START, WINDOW_END);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void collectsWindowForHandlesSkipsFailedHandleAndWritesSuccessfulMatches() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("alice", page(
                submission(101, "alice", "2026-07-01T00:00:00Z"),
                submission(102, "alice", "2026-06-29T23:59:59Z")
        ));
        sourceClient.fail("broken", new CodeforcesApiException(
                CodeforcesApiException.ErrorCode.CODEFORCES_API_STATUS_FAILED,
                "handle not found"
        ));
        sourceClient.addPage("bob", page(
                submission(201, "bob", "2026-07-02T00:00:00Z"),
                submission(202, "bob", "2026-07-05T04:00:00Z")
        ));
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(
                List.of("alice", "broken", "bob"),
                sourceClient,
                writer,
                100
        );

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.PARTIAL_SUCCESS);
        assertThat(result.requestedHandleCount()).isEqualTo(3);
        assertThat(result.succeededHandleCount()).isEqualTo(2);
        assertThat(result.failedHandleCount()).isEqualTo(1);
        assertThat(result.fetchedSubmissionCount()).isEqualTo(4);
        assertThat(result.matchedSubmissionCount()).isEqualTo(2);
        assertThat(result.batchId()).startsWith("collector-codeforces-");
        assertThat(result.writtenRows()).isEqualTo(2);
        assertThat(writer.records).extracting(CodeforcesOdsSubmission::authorHandle)
                .containsExactly("alice", "bob");
        assertThat(result.handles()).extracting("handle")
                .containsExactly("alice", "broken", "bob");
        assertThat(result.handles().get(1).status())
                .isEqualTo(CodeforcesSubmissionCollectionHandleStatus.FAILED);
        assertThat(result.handles().get(1).errorCode())
                .isEqualTo("CODEFORCES_API_STATUS_FAILED");
    }

    @Test
    void collectRecentWindowForStudentIdentityResolvesHandleBeforeFetchingSource() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("alice", page(submission(601, "alice", "2026-07-01T00:00:00Z")));
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(
                sourceClient,
                writer,
                100,
                Clock.fixed(FETCHED_AT, ZoneId.of("Asia/Shanghai")),
                Map.of("112487张三", "alice")
        );

        var result = service.collectRecentWindowForStudentIdentity(" 112487张三 ", LOOKBACK);

        assertThat(result.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.SUCCESS);
        assertThat(result.requestedHandleCount()).isEqualTo(1);
        assertThat(result.handles()).extracting("handle").containsExactly("alice");
        assertThat(sourceClient.calls()).containsExactly(new SourceCall("alice", 1, 100));
        assertThat(writer.records).extracting(CodeforcesOdsSubmission::authorHandle)
                .containsExactly("alice");
    }

    @Test
    void configuredCollectionOnlyUsesAccountsMarkedNeedCollect() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("alice", page(submission(701, "alice", "2026-07-01T00:00:00Z")));
        sourceClient.addPage("bob", page(submission(702, "bob", "2026-07-01T00:00:00Z")));
        RecordingWriter writer = new RecordingWriter();
        FakeHandleAccountRepository repository = new FakeHandleAccountRepository(Map.of(
                "112487张三", "alice",
                "112488李四", "bob"
        ));
        repository.setNeedCollect("112488李四", false);
        CodeforcesSubmissionCollectionService service = service(repository, sourceClient, writer, 100);

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.requestedHandleCount()).isEqualTo(1);
        assertThat(result.handles()).extracting("handle").containsExactly("alice");
        assertThat(sourceClient.calls()).containsExactly(new SourceCall("alice", 1, 100));
        assertThat(writer.records).extracting(CodeforcesOdsSubmission::authorHandle)
                .containsExactly("alice");
    }

    @Test
    void paginatesUntilAFullPageIsOlderThanTheCollectionWindow() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("alice", page(
                submission(301, "alice", "2026-07-04T00:00:00Z"),
                submission(302, "alice", "2026-07-03T00:00:00Z")
        ));
        sourceClient.addPage("alice", page(
                submission(303, "alice", "2026-06-29T00:00:00Z"),
                submission(304, "alice", "2026-06-28T00:00:00Z")
        ));
        sourceClient.addPage("alice", page(submission(305, "alice", "2026-07-02T00:00:00Z")));
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(
                List.of("alice"),
                sourceClient,
                writer,
                2
        );

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.SUCCESS);
        assertThat(result.fetchedSubmissionCount()).isEqualTo(4);
        assertThat(result.matchedSubmissionCount()).isEqualTo(2);
        assertThat(sourceClient.calls()).containsExactly(
                new SourceCall("alice", 1, 2),
                new SourceCall("alice", 3, 2)
        );
        assertThat(writer.records).extracting(CodeforcesOdsSubmission::codeforcesSubmissionId)
                .containsExactly(301L, 302L);
    }

    @Test
    void doesNotCallSourceOrWriteOdsWhenNoHandlesAreAvailable() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(List.of(), sourceClient, writer, 100);

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.SUCCESS);
        assertThat(result.message()).isEqualTo("No Codeforces handles configured for collection");
        assertThat(sourceClient.calls()).isEmpty();
        assertThat(writer.records).isEmpty();
        assertThat(result.batchId()).isNull();
    }

    @Test
    void doesNotWriteOdsWhenNoSubmissionMatchesWindow() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("alice", page(submission(401, "alice", "2026-06-01T00:00:00Z")));
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(
                List.of("alice"),
                sourceClient,
                writer,
                100
        );

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.SUCCESS);
        assertThat(result.message()).isEqualTo("No Codeforces submissions matched the collection window");
        assertThat(result.writtenRows()).isZero();
        assertThat(writer.records).isEmpty();
    }

    @Test
    void retriesFailedPageFetchBeforeMarkingHandleSuccessful() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.failTimes("alice", new IllegalStateException("temporary source failure"), 2);
        sourceClient.addPage("alice", page(submission(501, "alice", "2026-07-01T00:00:00Z")));
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(
                List.of("alice"),
                sourceClient,
                writer,
                100
        );

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.SUCCESS);
        assertThat(result.failedHandleCount()).isZero();
        assertThat(result.matchedSubmissionCount()).isEqualTo(1);
        assertThat(sourceClient.calls()).containsExactly(
                new SourceCall("alice", 1, 100),
                new SourceCall("alice", 1, 100),
                new SourceCall("alice", 1, 100)
        );
        assertThat(writer.records).extracting(CodeforcesOdsSubmission::codeforcesSubmissionId)
                .containsExactly(501L);
    }

    @Test
    void marksHandleFailedWhenPageFetchRetriesAreExhausted() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.failTimes("alice", new IllegalStateException("source still unavailable"), 3);
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(
                List.of("alice"),
                sourceClient,
                writer,
                100
        );

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.FAILED);
        assertThat(result.failedHandleCount()).isEqualTo(1);
        assertThat(result.handles().getFirst().status()).isEqualTo(CodeforcesSubmissionCollectionHandleStatus.FAILED);
        assertThat(result.handles().getFirst().errorCode()).isEqualTo("CODEFORCES_COLLECTOR_HANDLE_FAILED");
        assertThat(sourceClient.calls()).containsExactly(
                new SourceCall("alice", 1, 100),
                new SourceCall("alice", 1, 100),
                new SourceCall("alice", 1, 100)
        );
        assertThat(writer.records).isEmpty();
    }

    @Test
    void collectRecentWindowForConfiguredHandlesUsesCurrentInstantAsRightBound() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        RecordingWriter writer = new RecordingWriter();
        Clock clock = Clock.fixed(Instant.parse("2026-07-05T04:34:56Z"), ZoneId.of("Asia/Shanghai"));
        CodeforcesSubmissionCollectionService service = service(
                List.of(),
                sourceClient,
                writer,
                100,
                clock
        );

        var result = service.collectRecentWindowForConfiguredHandles(Duration.ofHours(120));

        assertThat(result.windowEndExclusive()).isEqualTo(Instant.parse("2026-07-05T04:34:56Z"));
        assertThat(result.windowStartInclusive()).isEqualTo(Instant.parse("2026-06-30T04:34:56Z"));
    }

    @Test
    void skipsOverlappingCollectionRunInSameJvm() throws Exception {
        CodeforcesSubmissionCollectionService[] holder = new CodeforcesSubmissionCollectionService[1];
        List<Object> nestedResults = new ArrayList<>();
        FakeHandleAccountRepository repository = new FakeHandleAccountRepository(Map.of());
        repository.beforeFindAll(() -> {
            try {
                nestedResults.add(holder[0].collectRecentWindowForConfiguredHandles(LOOKBACK));
            } catch (Exception ex) {
                nestedResults.add(ex);
            }
        });
        holder[0] = service(repository, new FakeSourceClient(), new RecordingWriter(), 100);

        var outer = holder[0].collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(outer.status()).isEqualTo(CodeforcesSubmissionCollectionStatus.SUCCESS);
        assertThat(nestedResults).hasSize(1);
        assertThat(nestedResults.getFirst())
                .extracting("status")
                .isEqualTo(CodeforcesSubmissionCollectionStatus.SKIPPED);
    }

    private CodeforcesSubmissionCollectionService service(
            List<String> handles,
            CodeforcesSubmissionSourceClient sourceClient,
            RecordingWriter writer,
            int pageSize
    ) {
        return service(handles, sourceClient, writer, pageSize, Clock.fixed(FETCHED_AT, ZoneId.of("Asia/Shanghai")));
    }

    private CodeforcesSubmissionCollectionService service(
            List<String> handles,
            CodeforcesSubmissionSourceClient sourceClient,
            RecordingWriter writer,
            int pageSize,
            Clock clock
    ) {
        return service(sourceClient, writer, pageSize, clock, handlesByIdentity(handles));
    }

    private CodeforcesSubmissionCollectionService service(
            CodeforcesSubmissionSourceClient sourceClient,
            RecordingWriter writer,
            int pageSize,
            Clock clock,
            Map<String, String> handlesByIdentity
    ) {
        return service(new FakeHandleAccountRepository(handlesByIdentity), sourceClient, writer, pageSize, clock);
    }

    private CodeforcesSubmissionCollectionService service(
            FakeHandleAccountRepository handleAccountRepository,
            CodeforcesSubmissionSourceClient sourceClient,
            RecordingWriter writer,
            int pageSize
    ) {
        return service(
                handleAccountRepository,
                sourceClient,
                writer,
                pageSize,
                Clock.fixed(FETCHED_AT, ZoneId.of("Asia/Shanghai"))
        );
    }

    private CodeforcesSubmissionCollectionService service(
            FakeHandleAccountRepository handleAccountRepository,
            CodeforcesSubmissionSourceClient sourceClient,
            RecordingWriter writer,
            int pageSize,
            Clock clock
    ) {
        CodeforcesSubmissionParser parser = new JacksonCodeforcesSubmissionParser(objectMapper);
        CodeforcesOdsSubmissionIngestService ingestService = new CodeforcesOdsSubmissionIngestService(
                parser,
                writer,
                objectMapper,
                clock
        );
        return new CodeforcesSubmissionCollectionService(
                new CodeforcesHandleAccountService(handleAccountRepository),
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

    private static Map<String, String> handlesByIdentity(List<String> handles) {
        Map<String, String> handlesByIdentity = new LinkedHashMap<>();
        for (int index = 0; index < handles.size(); index++) {
            handlesByIdentity.put("student-" + index, handles.get(index));
        }
        return handlesByIdentity;
    }

    private ArrayNode page(JsonNode... submissions) {
        ArrayNode page = objectMapper.createArrayNode();
        for (JsonNode submission : submissions) {
            page.add(submission);
        }
        return page;
    }

    private JsonNode submission(long id, String handle, String submittedAt) {
        long creationTimeSeconds = Instant.parse(submittedAt).getEpochSecond();
        return objectMapper.createObjectNode()
                .put("id", id)
                .put("creationTimeSeconds", creationTimeSeconds)
                .set("author", objectMapper.createObjectNode()
                        .set("members", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode().put("handle", handle))));
    }

    private record SourceCall(String handle, int from, int count) {
    }

    private static final class FakeHandleAccountRepository implements CodeforcesHandleAccountRepository {
        private final Map<String, CodeforcesHandleAccount> accountsByIdentity = new LinkedHashMap<>();
        private Runnable beforeFindAll = () -> {
        };

        private FakeHandleAccountRepository(Map<String, String> handlesByIdentity) {
            handlesByIdentity.forEach((studentIdentity, handle) -> accountsByIdentity.put(
                    studentIdentity,
                    new CodeforcesHandleAccount(studentIdentity, handle, Instant.EPOCH, Instant.EPOCH)
            ));
        }

        private void setNeedCollect(String studentIdentity, boolean needCollect) {
            CodeforcesHandleAccount existing = accountsByIdentity.get(studentIdentity);
            accountsByIdentity.put(studentIdentity, new CodeforcesHandleAccount(
                    existing.studentIdentity(),
                    existing.handle(),
                    needCollect,
                    existing.createdAt(),
                    existing.updatedAt()
            ));
        }

        private void beforeFindAll(Runnable beforeFindAll) {
            this.beforeFindAll = beforeFindAll;
        }

        @Override
        public List<CodeforcesHandleAccount> findAll() {
            beforeFindAll.run();
            return List.copyOf(accountsByIdentity.values());
        }

        @Override
        public Optional<CodeforcesHandleAccount> findByStudentIdentity(String studentIdentity) {
            return Optional.ofNullable(accountsByIdentity.get(studentIdentity));
        }

        @Override
        public Optional<CodeforcesHandleAccount> findByHandle(String handle) {
            return accountsByIdentity.values().stream()
                    .filter(account -> account.handle().equals(handle))
                    .findFirst();
        }

        @Override
        public CodeforcesHandleAccount save(CodeforcesHandleAccount account) {
            throw new UnsupportedOperationException("save is not used by collection tests");
        }

        @Override
        public CodeforcesHandleAccount updateStudentIdentityAndNeedCollect(
                String oldStudentIdentity,
                String newStudentIdentity,
                boolean needCollect,
                Instant updatedAt
        ) {
            throw new UnsupportedOperationException("updateStudentIdentityAndNeedCollect is not used by collection tests");
        }
    }

    private static final class FakeSourceClient implements CodeforcesSubmissionSourceClient {
        private final Map<String, List<JsonNode>> pagesByHandle = new HashMap<>();
        private final Map<String, RuntimeException> failuresByHandle = new HashMap<>();
        private final Map<String, RemainingFailure> remainingFailuresByHandle = new HashMap<>();
        private final List<SourceCall> calls = new ArrayList<>();

        void addPage(String handle, JsonNode page) {
            pagesByHandle.computeIfAbsent(handle, ignored -> new ArrayList<>()).add(page);
        }

        void fail(String handle, RuntimeException ex) {
            failuresByHandle.put(handle, ex);
        }

        void failTimes(String handle, RuntimeException ex, int times) {
            remainingFailuresByHandle.put(handle, new RemainingFailure(ex, times));
        }

        List<SourceCall> calls() {
            return List.copyOf(calls);
        }

        @Override
        public JsonNode fetchUserStatus(String handle, int from, int count) {
            calls.add(new SourceCall(handle, from, count));
            RuntimeException failure = failuresByHandle.get(handle);
            if (failure != null) {
                throw failure;
            }
            RemainingFailure remainingFailure = remainingFailuresByHandle.get(handle);
            if (remainingFailure != null && remainingFailure.remainingCount > 0) {
                remainingFailure.remainingCount--;
                throw remainingFailure.failure;
            }
            int pageIndex = Math.max(0, (from - 1) / count);
            List<JsonNode> pages = pagesByHandle.getOrDefault(handle, List.of());
            if (pageIndex >= pages.size()) {
                return new ObjectMapper().createArrayNode();
            }
            return pages.get(pageIndex);
        }

        private static final class RemainingFailure {
            private final RuntimeException failure;
            private int remainingCount;

            private RemainingFailure(RuntimeException failure, int remainingCount) {
                this.failure = failure;
                this.remainingCount = remainingCount;
            }
        }
    }

    private static final class RecordingWriter implements CodeforcesOdsSubmissionWriter {
        private CodeforcesCollectBatch batch;
        private List<CodeforcesOdsSubmission> records = List.of();

        @Override
        public void upsertBatch(CodeforcesCollectBatch batch, List<CodeforcesOdsSubmission> submissions) {
            this.batch = batch;
            this.records = List.copyOf(submissions);
        }
    }
}
