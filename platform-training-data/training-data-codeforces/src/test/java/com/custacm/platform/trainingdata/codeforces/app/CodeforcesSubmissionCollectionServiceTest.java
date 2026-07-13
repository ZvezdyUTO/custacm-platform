package com.custacm.platform.trainingdata.codeforces.app;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionHandleStatus;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesCollectBatch;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesSubmissionSourceClient;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.SubmissionPayloadParser;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesOdsSubmissionWriter;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.codeforces.infra.CodeforcesApiException;
import com.custacm.platform.trainingdata.codeforces.infra.JacksonSubmissionPayloadParser;
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

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.PARTIAL_SUCCESS);
        assertThat(result.ojName()).isEqualTo(OjNames.CODEFORCES);
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
                .isEqualTo(OjSubmissionCollectionHandleStatus.FAILED);
        assertThat(result.handles().get(1).errorCode())
                .isEqualTo("CODEFORCES_API_STATUS_FAILED");
    }

    @Test
    void collectRecentWindowForUsernameResolvesHandleBeforeFetchingSource() throws Exception {
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

        var result = service.collectRecentWindowForUsername(" 112487张三 ", LOOKBACK);

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(result.ojName()).isEqualTo(OjNames.CODEFORCES);
        assertThat(result.requestedHandleCount()).isEqualTo(1);
        assertThat(result.handles()).extracting("handle").containsExactly("alice");
        assertThat(sourceClient.calls()).containsExactly(new SourceCall("alice", 1, 100));
        assertThat(writer.records).extracting(CodeforcesOdsSubmission::authorHandle)
                .containsExactly("alice");
    }

    @Test
    void collectRecentWindowForUsernameAttributesTeamSubmissionToCollectedHandle() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("bob", page(teamSubmission(602, "2026-07-01T00:00:00Z", "alice", "bob")));
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(
                sourceClient,
                writer,
                100,
                Clock.fixed(FETCHED_AT, ZoneId.of("Asia/Shanghai")),
                Map.of("112487张三", "bob")
        );

        var result = service.collectRecentWindowForUsername("112487张三", LOOKBACK);

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(sourceClient.calls()).containsExactly(new SourceCall("bob", 1, 100));
        assertThat(writer.records).hasSize(1);
        assertThat(writer.records.getFirst().codeforcesSubmissionId()).isEqualTo(602L);
        assertThat(writer.records.getFirst().authorHandle()).isEqualTo("bob");
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
        FakeHandleAccountRepository repository = new FakeHandleAccountRepository(handlesByIdentity(List.of("alice")));
        CodeforcesSubmissionCollectionService service = service(repository, sourceClient, writer, 2);

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(result.fetchedSubmissionCount()).isEqualTo(4);
        assertThat(result.matchedSubmissionCount()).isEqualTo(2);
        assertThat(sourceClient.calls()).containsExactly(
                new SourceCall("alice", 1, 2),
                new SourceCall("alice", 3, 2)
        );
        assertThat(writer.records).extracting(CodeforcesOdsSubmission::codeforcesSubmissionId)
                .containsExactly(301L, 302L);
        OjHandleAccount account = repository.findByUsername("student-0").orElseThrow();
        assertThat(account.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt()).isEqualTo(FETCHED_AT);
    }

    @Test
    void firstSuccessfulCollectionCrawlsFullCodeforcesHistoryAndCreatesCursor() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        sourceClient.addPage("alice", page(submission(306, "alice", "2026-07-04T00:00:00Z")));
        RecordingWriter writer = new RecordingWriter();
        FakeHandleAccountRepository repository = new FakeHandleAccountRepository(handlesByIdentity(List.of("alice")));
        repository.clearCollectionState("student-0", OjNames.CODEFORCES);
        CodeforcesSubmissionCollectionService service = service(repository, sourceClient, writer, 2);

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(result.windowStartInclusive()).isEqualTo(Instant.EPOCH);
        OjHandleAccount account = repository.findByUsername("student-0").orElseThrow();
        assertThat(account.collectionStates().get(OjNames.CODEFORCES).lastCollectedAt()).isEqualTo(FETCHED_AT);
    }

    @Test
    void doesNotCallSourceOrWriteOdsWhenNoHandlesAreAvailable() throws Exception {
        FakeSourceClient sourceClient = new FakeSourceClient();
        RecordingWriter writer = new RecordingWriter();
        CodeforcesSubmissionCollectionService service = service(List.of(), sourceClient, writer, 100);

        var result = service.collectRecentWindowForConfiguredHandles(LOOKBACK);

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
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

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
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

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
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

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.FAILED);
        assertThat(result.failedHandleCount()).isEqualTo(1);
        assertThat(result.handles().getFirst().status()).isEqualTo(OjSubmissionCollectionHandleStatus.FAILED);
        assertThat(result.handles().getFirst().errorCode()).isEqualTo("OJ_COLLECTOR_HANDLE_FAILED");
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

        assertThat(outer.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(nestedResults).hasSize(1);
        assertThat(nestedResults.getFirst())
                .extracting("status")
                .isEqualTo(OjSubmissionCollectionStatus.SKIPPED);
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
        SubmissionPayloadParser parser = new JacksonSubmissionPayloadParser(objectMapper);
        CodeforcesOdsSubmissionIngestService ingestService = new CodeforcesOdsSubmissionIngestService(
                parser,
                writer,
                objectMapper,
                clock
        );
        return new CodeforcesSubmissionCollectionService(
                new OjHandleAccountService(handleAccountRepository, clock),
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

    private JsonNode teamSubmission(long id, String submittedAt, String firstHandle, String secondHandle) {
        long creationTimeSeconds = Instant.parse(submittedAt).getEpochSecond();
        return objectMapper.createObjectNode()
                .put("id", id)
                .put("creationTimeSeconds", creationTimeSeconds)
                .set("author", objectMapper.createObjectNode()
                        .set("members", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode().put("handle", firstHandle))
                                .add(objectMapper.createObjectNode().put("handle", secondHandle))));
    }

    private record SourceCall(String handle, int from, int count) {
    }

    private static final class FakeHandleAccountRepository implements OjHandleAccountRepository {
        private final Map<String, OjHandleAccount> accountsByIdentity = new LinkedHashMap<>();
        private Runnable beforeFindAll = () -> {
        };

        private FakeHandleAccountRepository(Map<String, String> handlesByIdentity) {
            handlesByIdentity.forEach((username, handle) -> accountsByIdentity.put(
                    username, account(username, Map.of(OjNames.CODEFORCES, handle), true)
            ));
        }

        private void putAccount(String username, Map<String, String> handles, boolean needCollect) {
            accountsByIdentity.put(username, account(username, handles, needCollect));
        }

        private void setNeedCollect(String username, boolean needCollect) {
            OjHandleAccount existing = accountsByIdentity.get(username);
            accountsByIdentity.put(username, new OjHandleAccount(
                    existing.username(),
                    existing.handles(),
                    needCollect,
                    existing.collectionStates(),
                    existing.createdAt(),
                    existing.updatedAt()
            ));
        }

        private void clearCollectionState(String username, String ojName) {
            OjHandleAccount existing = accountsByIdentity.get(username);
            Map<String, OjHandleCollectionState> states = new LinkedHashMap<>(existing.collectionStates());
            states.put(ojName, OjHandleCollectionState.empty());
            accountsByIdentity.put(username, new OjHandleAccount(
                    existing.username(),
                    existing.handles(),
                    existing.needCollect(),
                    states,
                    existing.createdAt(),
                    existing.updatedAt()
            ));
        }

        private static OjHandleAccount account(
                String username,
                Map<String, String> handles,
                boolean needCollect
        ) {
            return new OjHandleAccount(
                    username,
                    handles,
                    needCollect,
                    handles.keySet().stream().collect(java.util.stream.Collectors.toMap(
                            ojName -> ojName,
                            ignored -> new OjHandleCollectionState(FETCHED_AT)
                    )),
                    Instant.EPOCH,
                    Instant.EPOCH
            );
        }

        private void beforeFindAll(Runnable beforeFindAll) {
            this.beforeFindAll = beforeFindAll;
        }

        @Override
        public List<OjHandleAccount> findAll() {
            beforeFindAll.run();
            return List.copyOf(accountsByIdentity.values());
        }

        @Override
        public java.util.Optional<OjHandleAccount> findByUsername(String username) {
            return java.util.Optional.ofNullable(accountsByIdentity.get(username));
        }

        @Override
        public java.util.Optional<OjHandleAccount> findByHandle(String ojName, String handle) {
            String normalizedOjName = OjNames.normalize(ojName);
            return accountsByIdentity.values().stream()
                    .filter(account -> handle.equals(account.handles().get(normalizedOjName)))
                    .findFirst();
        }

        @Override
        public OjHandleAccount save(OjHandleAccount account) {
            throw new UnsupportedOperationException("save is not used by collection tests");
        }

        @Override
        public OjHandleAccount updateUsernameAndNeedCollect(
                String oldUsername,
                String newUsername,
                Map<String, String> handles,
                boolean needCollect,
                Map<String, OjHandleCollectionState> collectionStates,
                Instant updatedAt
        ) {
            OjHandleAccount existing = accountsByIdentity.remove(oldUsername);
            OjHandleAccount updated = new OjHandleAccount(
                    newUsername,
                    handles,
                    needCollect,
                    collectionStates,
                    existing.createdAt(),
                    updatedAt
            );
            accountsByIdentity.put(newUsername, updated);
            return updated;
        }

        @Override
        public OjHandleAccount updateCollectionStates(
                String username,
                Map<String, OjHandleCollectionState> collectionStates,
                Instant updatedAt
        ) {
            OjHandleAccount existing = accountsByIdentity.get(username);
            OjHandleAccount updated = new OjHandleAccount(
                    existing.username(),
                    existing.handles(),
                    existing.needCollect(),
                    collectionStates,
                    existing.createdAt(),
                    updatedAt
            );
            accountsByIdentity.put(username, updated);
            return updated;
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
