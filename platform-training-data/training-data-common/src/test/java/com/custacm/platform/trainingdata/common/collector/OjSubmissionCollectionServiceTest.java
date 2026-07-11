package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.collector.result.OjHandleCollectionOutcome;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionHandleResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionWriteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OjSubmissionCollectionServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-05T04:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void collectsConfiguredHandlesAggregatesResultsAndWritesMatchedSubmissions() throws Exception {
        FakeHandleResolver resolver = new FakeHandleResolver();
        resolver.handlesByOj.put("CODEFORCES", List.of(" alice ", "broken", "alice", "bob"));
        FakeAdapter adapter = new FakeAdapter();
        adapter.failedHandles.add("broken");
        OjSubmissionCollectionService service = service(resolver, adapter);

        OjSubmissionCollectionResult result = service.collectRecentWindowForConfiguredHandles(
                "codeforces",
                Duration.ofHours(24)
        );

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.PARTIAL_SUCCESS);
        assertThat(result.ojName()).isEqualTo("CODEFORCES");
        assertThat(result.windowStartInclusive()).isEqualTo(NOW.minus(Duration.ofHours(24)));
        assertThat(result.windowEndExclusive()).isEqualTo(NOW);
        assertThat(result.requestedHandleCount()).isEqualTo(3);
        assertThat(result.succeededHandleCount()).isEqualTo(2);
        assertThat(result.failedHandleCount()).isEqualTo(1);
        assertThat(result.fetchedSubmissionCount()).isEqualTo(9);
        assertThat(result.matchedSubmissionCount()).isEqualTo(2);
        assertThat(result.batchId()).isEqualTo("batch-codeforces");
        assertThat(adapter.writtenHandles).containsExactly("alice", "bob");
        assertThat(resolver.markedCollections).containsExactly(
                new MarkedCollection("CODEFORCES", "alice", false, NOW),
                new MarkedCollection("CODEFORCES", "bob", false, NOW)
        );
    }

    @Test
    void resolvesUsernameBeforeCollectingOneHandle() throws Exception {
        FakeHandleResolver resolver = new FakeHandleResolver();
        resolver.handlesByIdentity.put("112487张三", "tourist");
        FakeAdapter adapter = new FakeAdapter();
        OjSubmissionCollectionService service = service(resolver, adapter);

        OjSubmissionCollectionResult result = service.collectRecentWindowForUsername(
                " 112487张三 ",
                Duration.ofHours(12)
        );

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(result.ojName()).isEqualTo("CODEFORCES");
        assertThat(result.handles()).extracting("handle").containsExactly("tourist");
        assertThat(adapter.collectedHandles).containsExactly("tourist");
    }

    @Test
    void skipsNestedCollectionInSameJvm() throws Exception {
        FakeHandleResolver resolver = new FakeHandleResolver();
        FakeAdapter adapter = new FakeAdapter();
        OjSubmissionCollectionService[] holder = new OjSubmissionCollectionService[1];
        List<Object> nestedResults = new ArrayList<>();
        resolver.beforeList = () -> {
            try {
                nestedResults.add(holder[0].collectRecentWindowForConfiguredHandles(Duration.ofHours(1)));
            } catch (Exception ex) {
                nestedResults.add(ex);
            }
        };
        holder[0] = service(resolver, adapter);

        OjSubmissionCollectionResult result = holder[0].collectRecentWindowForConfiguredHandles(Duration.ofHours(1));

        assertThat(result.status()).isEqualTo(OjSubmissionCollectionStatus.SUCCESS);
        assertThat(nestedResults).singleElement()
                .extracting("status")
                .isEqualTo(OjSubmissionCollectionStatus.SKIPPED);
    }

    private OjSubmissionCollectionService service(FakeHandleResolver resolver, FakeAdapter adapter) {
        return new OjSubmissionCollectionService(
                resolver,
                adapter,
                3,
                Duration.ZERO,
                Clock.fixed(NOW, ZoneOffset.UTC),
                duration -> {
                }
        );
    }

    private final class FakeAdapter implements OjSubmissionCollectionAdapter {
        private final List<String> failedHandles = new ArrayList<>();
        private final List<String> collectedHandles = new ArrayList<>();
        private final List<String> writtenHandles = new ArrayList<>();

        @Override
        public String defaultOjName() {
            return "CODEFORCES";
        }

        @Override
        public String displayName(String ojName) {
            return "Codeforces";
        }

        @Override
        public OjHandleCollectionOutcome collectHandle(
                String ojName,
                String handle,
                Instant windowStartInclusive,
                Instant windowEndExclusive,
                OjCollectionRequestExecutor requestExecutor
        ) {
            collectedHandles.add(handle);
            requestExecutor.execute(() -> objectMapper.createObjectNode());
            if (failedHandles.contains(handle)) {
                return new OjHandleCollectionOutcome(
                        OjSubmissionCollectionHandleResult.failed(handle, 3, "SOURCE_FAILED", "source failed"),
                        List.of()
                );
            }
            return new OjHandleCollectionOutcome(
                    OjSubmissionCollectionHandleResult.success(handle, 3, 1),
                    List.of(objectMapper.createObjectNode().put("handle", handle))
            );
        }

        @Override
        public OjSubmissionCollectionWriteResult writeBatch(String ojName, List<OjHandleCollectionOutcome> outcomes) {
            outcomes.forEach(outcome -> writtenHandles.add(outcome.result().handle()));
            return new OjSubmissionCollectionWriteResult(
                    "batch-" + ojName.toLowerCase(),
                    "ods_" + ojName.toLowerCase(),
                    outcomes.stream().mapToInt(outcome -> outcome.submissions().size()).sum(),
                    NOW
            );
        }
    }

    private static final class FakeHandleResolver implements OjCollectionHandleResolver {
        private final Map<String, List<String>> handlesByOj = new LinkedHashMap<>();
        private final Map<String, String> handlesByIdentity = new LinkedHashMap<>();
        private final List<MarkedCollection> markedCollections = new ArrayList<>();
        private Runnable beforeList = () -> {
        };

        @Override
        public String getHandleByUsername(String ojName, String username) {
            return handlesByIdentity.get(username);
        }

        @Override
        public List<String> listHandlesForCollection(String ojName) {
            beforeList.run();
            return handlesByOj.getOrDefault(ojName, List.of());
        }

        @Override
        public void markHandleCollected(
                String ojName,
                String handle,
                boolean historyStartReached,
                Instant collectedAt
        ) {
            markedCollections.add(new MarkedCollection(ojName, handle, historyStartReached, collectedAt));
        }
    }

    private record MarkedCollection(
            String ojName,
            String handle,
            boolean historyStartReached,
            Instant collectedAt
    ) {
    }
}
