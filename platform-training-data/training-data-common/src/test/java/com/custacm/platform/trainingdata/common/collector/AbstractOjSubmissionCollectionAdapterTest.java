package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.collector.result.OjHandleCollectionOutcome;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionHandleStatus;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionWriteResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractOjSubmissionCollectionAdapterTest {
    private static final Instant WINDOW_START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant WINDOW_END = Instant.parse("2026-07-02T00:00:00Z");

    @Test
    void collectHandleTrimsHandleAndBuildsSuccessfulOutcomeFromProgress() throws Exception {
        TestAdapter adapter = new TestAdapter(Mode.SUCCESS);
        RecordingBatchWriter writer = new RecordingBatchWriter();

        OjHandleCollectionOutcome outcome = adapter.collectHandle(
                "CODEFORCES",
                " alice ",
                WINDOW_START,
                WINDOW_END,
                requestExecutor(),
                writer
        );

        assertThat(adapter.capturedHandle).isEqualTo("alice");
        assertThat(outcome.result().handle()).isEqualTo("alice");
        assertThat(outcome.result().status()).isEqualTo(OjSubmissionCollectionHandleStatus.SUCCESS);
        assertThat(outcome.result().fetchedSubmissionCount()).isEqualTo(3);
        assertThat(outcome.result().matchedSubmissionCount()).isEqualTo(2);
        assertThat(writer.submissionIds()).containsExactly(1L, 2L);
    }

    @Test
    void collectHandleUsesSourceFailureErrorCodeAndPreservesFetchedCount() throws Exception {
        TestAdapter adapter = new TestAdapter(Mode.SOURCE_FAILURE);

        OjHandleCollectionOutcome outcome = adapter.collectHandle(
                "ATCODER",
                "tourist",
                WINDOW_START,
                WINDOW_END,
                requestExecutor(),
                new RecordingBatchWriter()
        );

        assertThat(outcome.result().status()).isEqualTo(OjSubmissionCollectionHandleStatus.FAILED);
        assertThat(outcome.result().fetchedSubmissionCount()).isEqualTo(4);
        assertThat(outcome.result().matchedSubmissionCount()).isZero();
        assertThat(outcome.result().errorCode()).isEqualTo("SOURCE_FAILED");
        assertThat(outcome.result().message()).isEqualTo("source failed");
    }

    @Test
    void collectHandleFallsBackToGenericErrorCodeForRuntimeFailure() throws Exception {
        TestAdapter adapter = new TestAdapter(Mode.GENERIC_FAILURE);

        OjHandleCollectionOutcome outcome = adapter.collectHandle(
                "CODEFORCES",
                "alice",
                WINDOW_START,
                WINDOW_END,
                requestExecutor(),
                new RecordingBatchWriter()
        );

        assertThat(outcome.result().status()).isEqualTo(OjSubmissionCollectionHandleStatus.FAILED);
        assertThat(outcome.result().fetchedSubmissionCount()).isEqualTo(5);
        assertThat(outcome.result().errorCode()).isEqualTo("OJ_COLLECTOR_HANDLE_FAILED");
        assertThat(outcome.result().message()).isEqualTo("generic failure");
    }

    @Test
    void collectHandleRejectsBlankHandleBeforeFailureWrapping() {
        TestAdapter adapter = new TestAdapter(Mode.SUCCESS);

        assertThatThrownBy(() -> adapter.collectHandle(
                "CODEFORCES",
                " ",
                WINDOW_START,
                WINDOW_END,
                requestExecutor(),
                new RecordingBatchWriter()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("handle must not be blank");
    }

    @Test
    void collectHandlePropagatesChunkWriteFailure() {
        TestAdapter adapter = new TestAdapter(Mode.SUCCESS);
        OjSubmissionCollectionBatchWriter failingWriter = new OjSubmissionCollectionBatchWriter() {
            @Override
            public void write(String handle, List<JsonNode> submissions) {
                throw new IllegalStateException("database unavailable");
            }

            @Override
            public int writtenRows() {
                return 0;
            }

            @Override
            public OjSubmissionCollectionWriteResult result() {
                throw new IllegalStateException("not written");
            }
        };

        assertThatThrownBy(() -> adapter.collectHandle(
                "CODEFORCES",
                "alice",
                WINDOW_START,
                WINDOW_END,
                requestExecutor(),
                failingWriter
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    @Test
    void collectorBatchIdPrefixUsesLowercaseOjName() {
        TestAdapter adapter = new TestAdapter(Mode.SUCCESS);

        assertThat(adapter.batchIdPrefix("CODEFORCES")).isEqualTo("collector-codeforces");
    }

    private static OjCollectionRequestExecutor requestExecutor() {
        return new OjCollectionRequestExecutor(1, Duration.ZERO, duration -> {
        });
    }

    private enum Mode {
        SUCCESS,
        SOURCE_FAILURE,
        GENERIC_FAILURE
    }

    private static final class TestAdapter extends AbstractOjSubmissionCollectionAdapter {
        private final Mode mode;
        private String capturedHandle;

        private TestAdapter(Mode mode) {
            super(TestAdapter.class);
            this.mode = mode;
        }

        @Override
        public String defaultOjName() {
            return "CODEFORCES";
        }

        @Override
        public OjSubmissionCollectionBatchWriter openBatch(String ojName) {
            return new RecordingBatchWriter();
        }

        @Override
        protected void collectHandleSubmissions(
                String ojName,
                String normalizedHandle,
                Instant windowStartInclusive,
                Instant windowEndExclusive,
                OjCollectionRequestExecutor requestExecutor,
                HandleCollectionProgress progress
        ) throws JsonProcessingException {
            capturedHandle = normalizedHandle;
            if (mode == Mode.SOURCE_FAILURE) {
                progress.addFetchedSubmissionCount(4);
                throw new TestSourceFailure("source failed");
            }
            if (mode == Mode.GENERIC_FAILURE) {
                progress.addFetchedSubmissionCount(5);
                throw new IllegalStateException("generic failure");
            }
            progress.addFetchedSubmissionCount(3);
            progress.writeMatchedSubmission(submission(1));
            progress.writeMatchedSubmissions(List.of(submission(2)));
        }

        private String batchIdPrefix(String ojName) {
            return collectorBatchIdPrefix(ojName);
        }

        private static JsonNode submission(long id) {
            return JsonNodeFactory.instance.objectNode().put("id", id);
        }
    }

    private static final class RecordingBatchWriter implements OjSubmissionCollectionBatchWriter {
        private final List<JsonNode> submissions = new ArrayList<>();

        @Override
        public void write(String handle, List<JsonNode> submissions) {
            this.submissions.addAll(submissions);
        }

        @Override
        public int writtenRows() {
            return submissions.size();
        }

        @Override
        public OjSubmissionCollectionWriteResult result() {
            return new OjSubmissionCollectionWriteResult("batch", "ods", submissions.size(), WINDOW_END);
        }

        private List<Long> submissionIds() {
            return submissions.stream().map(submission -> submission.path("id").asLong()).toList();
        }
    }

    private static final class TestSourceFailure extends RuntimeException implements OjCollectionSourceFailure {
        private TestSourceFailure(String message) {
            super(message);
        }

        @Override
        public String collectorErrorCode() {
            return "SOURCE_FAILED";
        }
    }
}
