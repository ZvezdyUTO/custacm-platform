package com.custacm.platform.trainingdata.common.collector.dispatch;

import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionStatus;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjSubmissionCollectionDispatcherTest {
    private static final Duration LOOKBACK = Duration.ofHours(120);

    @Test
    void dispatchesBlankOjNameToDefaultCollector() throws Exception {
        RecordingCollector codeforces = new RecordingCollector(OjNames.CODEFORCES);
        RecordingCollector atcoder = new RecordingCollector(OjNames.ATCODER);
        OjSubmissionCollectionDispatcher dispatcher = new OjSubmissionCollectionDispatcher(
                OjNames.CODEFORCES,
                List.of(codeforces, atcoder)
        );

        var result = dispatcher.collectRecentWindowForUsername(null, "112487张三", LOOKBACK);

        assertThat(result.ojName()).isEqualTo(OjNames.CODEFORCES);
        assertThat(codeforces.studentCalls).isEqualTo(1);
        assertThat(atcoder.studentCalls).isZero();
    }

    @Test
    void dispatchesExplicitOjNameToMatchingCollector() throws Exception {
        RecordingCollector codeforces = new RecordingCollector(OjNames.CODEFORCES);
        RecordingCollector atcoder = new RecordingCollector(OjNames.ATCODER);
        OjSubmissionCollectionDispatcher dispatcher = new OjSubmissionCollectionDispatcher(
                OjNames.CODEFORCES,
                List.of(codeforces, atcoder)
        );

        var result = dispatcher.collectRecentWindowForConfiguredHandles(OjNames.ATCODER, LOOKBACK);

        assertThat(result.ojName()).isEqualTo(OjNames.ATCODER);
        assertThat(codeforces.configuredCalls).isZero();
        assertThat(atcoder.configuredCalls).isEqualTo(1);
    }

    @Test
    void rejectsOjNameWithoutCollector() {
        OjSubmissionCollectionDispatcher dispatcher = new OjSubmissionCollectionDispatcher(
                OjNames.CODEFORCES,
                List.of(new RecordingCollector(OjNames.CODEFORCES))
        );

        assertThatThrownBy(() -> dispatcher.collectRecentWindowForConfiguredHandles(OjNames.ATCODER, LOOKBACK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ATCODER submission collection is not implemented");
    }

    private static final class RecordingCollector implements OjRecentSubmissionCollector {
        private final String ojName;
        private int configuredCalls;
        private int studentCalls;

        private RecordingCollector(String ojName) {
            this.ojName = ojName;
        }

        @Override
        public String ojName() {
            return ojName;
        }

        @Override
        public OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(Duration lookback) {
            configuredCalls++;
            return result();
        }

        @Override
        public OjSubmissionCollectionResult collectRecentWindowForUsername(
                String username,
                Duration lookback
        ) {
            studentCalls++;
            return result();
        }

        private OjSubmissionCollectionResult result() {
            return new OjSubmissionCollectionResult(
                    ojName,
                    OjSubmissionCollectionStatus.SUCCESS,
                    Instant.parse("2026-07-01T00:00:00Z"),
                    Instant.parse("2026-07-06T00:00:00Z"),
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    null,
                    0,
                    null,
                    null,
                    List.of()
            );
        }
    }
}
