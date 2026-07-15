package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.collector.result.OjHandleCollectionOutcome;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionHandleResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public abstract class AbstractOjSubmissionCollectionAdapter implements OjSubmissionCollectionAdapter {
    private static final String COLLECTOR_BATCH_ID_PREFIX = "collector";
    private static final String HANDLE_FAILED_ERROR_CODE = "OJ_COLLECTOR_HANDLE_FAILED";

    private final Logger log;

    protected AbstractOjSubmissionCollectionAdapter(Class<?> adapterClass) {
        this.log = LoggerFactory.getLogger(Objects.requireNonNull(adapterClass, "adapterClass must not be null"));
    }

    @Override
    public final OjHandleCollectionOutcome collectHandle(
            String ojName,
            String handle,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            OjCollectionRequestExecutor requestExecutor,
            OjSubmissionCollectionBatchWriter batchWriter
    ) throws com.fasterxml.jackson.core.JsonProcessingException {
        String normalizedHandle = requireText(handle, "handle");
        HandleCollectionProgress progress = new HandleCollectionProgress(normalizedHandle, batchWriter);
        try {
            collectHandleSubmissions(
                    ojName,
                    normalizedHandle,
                    windowStartInclusive,
                    windowEndExclusive,
                    requestExecutor,
                    progress
            );
            return new OjHandleCollectionOutcome(
                    OjSubmissionCollectionHandleResult.success(
                            normalizedHandle,
                            progress.fetchedSubmissionCount(),
                            progress.matchedSubmissionCount()
                    )
            );
        } catch (ChunkWriteFailure ex) {
            throw ex.failure();
        } catch (RuntimeException ex) {
            String errorCode = errorCode(ex);
            log.warn(
                    "OJ handle collection failed, errorCode={}, ojName={}, handleHash={}",
                    errorCode,
                    ojName,
                    sha256(normalizedHandle),
                    ex
            );
            return new OjHandleCollectionOutcome(
                    OjSubmissionCollectionHandleResult.failed(
                            normalizedHandle,
                            progress.fetchedSubmissionCount(),
                            progress.matchedSubmissionCount(),
                            errorCode,
                            ex.getMessage()
                    )
            );
        }
    }

    protected abstract void collectHandleSubmissions(
            String ojName,
            String normalizedHandle,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            OjCollectionRequestExecutor requestExecutor,
            HandleCollectionProgress progress
    ) throws com.fasterxml.jackson.core.JsonProcessingException;

    protected final String collectorBatchIdPrefix(String ojName) {
        return COLLECTOR_BATCH_ID_PREFIX + "-" + ojName.toLowerCase(Locale.ROOT);
    }

    private static String errorCode(RuntimeException ex) {
        if (ex instanceof OjCollectionSourceFailure sourceFailure) {
            String errorCode = sourceFailure.collectorErrorCode();
            if (errorCode != null && !errorCode.isBlank()) {
                return errorCode.trim();
            }
        }
        return HANDLE_FAILED_ERROR_CODE;
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    protected static final class HandleCollectionProgress {
        private final String handle;
        private final OjSubmissionCollectionBatchWriter batchWriter;
        private int fetchedSubmissionCount;
        private int matchedSubmissionCount;

        private HandleCollectionProgress(String handle, OjSubmissionCollectionBatchWriter batchWriter) {
            this.handle = requireText(handle, "handle");
            this.batchWriter = Objects.requireNonNull(batchWriter, "batchWriter must not be null");
        }

        public void addFetchedSubmissionCount(int count) {
            if (count < 0) {
                throw new IllegalArgumentException("fetched submission count must not be negative");
            }
            fetchedSubmissionCount = Math.addExact(fetchedSubmissionCount, count);
        }

        public void writeMatchedSubmission(JsonNode submission)
                throws com.fasterxml.jackson.core.JsonProcessingException {
            writeMatchedSubmissions(List.of(Objects.requireNonNull(submission, "submission must not be null")));
        }

        public void writeMatchedSubmissions(Collection<? extends JsonNode> submissions)
                throws com.fasterxml.jackson.core.JsonProcessingException {
            Collection<? extends JsonNode> nonNullSubmissions =
                    Objects.requireNonNull(submissions, "submissions must not be null");
            if (nonNullSubmissions.isEmpty()) {
                return;
            }
            List<JsonNode> chunk = nonNullSubmissions.stream()
                    .map(submission -> Objects.requireNonNull(submission, "submission must not be null"))
                    .toList();
            try {
                batchWriter.write(handle, chunk);
            } catch (RuntimeException ex) {
                throw new ChunkWriteFailure(ex);
            }
            matchedSubmissionCount = Math.addExact(matchedSubmissionCount, chunk.size());
        }

        private int fetchedSubmissionCount() {
            return fetchedSubmissionCount;
        }

        private int matchedSubmissionCount() {
            return matchedSubmissionCount;
        }
    }

    private static final class ChunkWriteFailure extends RuntimeException {
        private final RuntimeException failure;

        private ChunkWriteFailure(RuntimeException failure) {
            super(failure);
            this.failure = failure;
        }

        private RuntimeException failure() {
            return failure;
        }
    }
}
