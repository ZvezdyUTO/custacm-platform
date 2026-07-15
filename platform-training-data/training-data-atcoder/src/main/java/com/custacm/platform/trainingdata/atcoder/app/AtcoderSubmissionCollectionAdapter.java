package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import com.custacm.platform.trainingdata.atcoder.domain.AtcoderCollectBatch;
import com.custacm.platform.trainingdata.common.collector.AbstractOjSubmissionCollectionAdapter;
import com.custacm.platform.trainingdata.common.collector.OjCollectionRequestExecutor;
import com.custacm.platform.trainingdata.common.collector.OjEpochSeconds;
import com.custacm.platform.trainingdata.common.collector.OjSubmissionCollectionBatchWriter;
import com.custacm.platform.trainingdata.common.collector.OjSubmissionWindowFilter;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionWriteResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;

final class AtcoderSubmissionCollectionAdapter extends AbstractOjSubmissionCollectionAdapter {
    private final AtcoderSubmissionSourceClient sourceClient;
    private final AtcoderOdsIngestService ingestService;
    private final ObjectMapper objectMapper;
    private final int pageSize;

    AtcoderSubmissionCollectionAdapter(
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            int pageSize
    ) {
        super(AtcoderSubmissionCollectionAdapter.class);
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        if (pageSize > AtcoderSubmissionSourceClient.USER_SUBMISSIONS_PAGE_LIMIT) {
            throw new IllegalArgumentException(
                    "pageSize must not exceed the AtCoder user submissions limit of "
                            + AtcoderSubmissionSourceClient.USER_SUBMISSIONS_PAGE_LIMIT
            );
        }
        this.sourceClient = sourceClient;
        this.ingestService = ingestService;
        this.objectMapper = objectMapper;
        this.pageSize = pageSize;
    }

    @Override
    public String defaultOjName() {
        return OjNames.ATCODER;
    }

    @Override
    public String displayName(String ojName) {
        return OjNames.ATCODER.equals(ojName) ? "AtCoder" : ojName;
    }

    @Override
    public OjSubmissionCollectionBatchWriter openBatch(String ojName) {
        return new BatchWriter(ingestService.startSubmissionBatch(collectorBatchIdPrefix(ojName)));
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
        long fromSecond = Math.max(0L, OjEpochSeconds.ceilingEpochSecond(windowStartInclusive));
        long endEpochSecondExclusive = OjEpochSeconds.ceilingEpochSecond(windowEndExclusive);
        while (true) {
            long currentFromSecond = fromSecond;
            JsonNode responsePage = requestExecutor.execute(
                    () -> sourceClient.fetchUserSubmissions(normalizedHandle, currentFromSecond)
            );
            if (!responsePage.isArray()) {
                throw new IllegalArgumentException("AtCoder user submissions result must be an array");
            }
            progress.addFetchedSubmissionCount(responsePage.size());
            var filteredPage = OjSubmissionWindowFilter.filterSortedPage(
                    responsePage,
                    windowStartInclusive,
                    windowEndExclusive,
                    AtcoderSubmissionCollectionAdapter::requiredEpochSecond
            );
            progress.writeMatchedSubmissions(filteredPage.matchedSubmissions());
            long maxEpochSecond = filteredPage.maxEpochSecond().orElse(Long.MIN_VALUE);
            if (responsePage.isEmpty() || responsePage.size() < pageSize
                    || maxEpochSecond >= endEpochSecondExclusive) {
                break;
            }
            fromSecond = Math.addExact(maxEpochSecond, 1L);
        }
    }

    private static long epochSecond(JsonNode submission) {
        OptionalLong value = epochSecondOptional(submission);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("missing AtCoder submission epoch_second");
        }
        return value.getAsLong();
    }

    private static OptionalLong requiredEpochSecond(JsonNode submission) {
        return OptionalLong.of(epochSecond(submission));
    }

    private static OptionalLong epochSecondOptional(JsonNode submission) {
        JsonNode value = submission.path("epoch_second");
        if (!value.canConvertToLong()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(value.asLong());
    }

    private final class BatchWriter implements OjSubmissionCollectionBatchWriter {
        private final AtcoderCollectBatch batch;
        private int writtenRows;
        private String tableName;

        private BatchWriter(AtcoderCollectBatch batch) {
            this.batch = batch;
        }

        @Override
        public void write(String handle, List<JsonNode> submissions) throws JsonProcessingException {
            if (submissions.isEmpty()) {
                return;
            }
            ArrayNode submissionArray = objectMapper.createArrayNode().addAll(submissions);
            AtcoderOdsBatchUpsertResult chunk = ingestService.upsertSubmissions(submissionArray, batch);
            tableName = chunk.tableName();
            writtenRows = Math.addExact(writtenRows, chunk.writtenRows());
        }

        @Override
        public int writtenRows() {
            return writtenRows;
        }

        @Override
        public OjSubmissionCollectionWriteResult result() {
            if (!hasWrites() || tableName == null) {
                throw new IllegalStateException("collection batch has no written submissions");
            }
            return new OjSubmissionCollectionWriteResult(
                    batch.batchId(),
                    tableName,
                    writtenRows,
                    batch.fetchedAt()
            );
        }
    }

}
