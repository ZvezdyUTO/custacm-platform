package com.custacm.platform.trainingdata.atcoder.app;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import com.custacm.platform.trainingdata.common.collector.AbstractOjSubmissionCollectionAdapter;
import com.custacm.platform.trainingdata.common.collector.OjCollectionRequestExecutor;
import com.custacm.platform.trainingdata.common.collector.OjEpochSeconds;
import com.custacm.platform.trainingdata.common.collector.OjSubmissionWindowFilter;
import com.custacm.platform.trainingdata.common.collector.result.OjHandleCollectionOutcome;
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
    protected void collectHandleSubmissions(
            String ojName,
            String normalizedHandle,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            OjCollectionRequestExecutor requestExecutor,
            HandleCollectionProgress progress
    ) {
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
            progress.addMatchedSubmissions(filteredPage.matchedSubmissions());
            long maxEpochSecond = filteredPage.maxEpochSecond().orElse(Long.MIN_VALUE);
            if (responsePage.isEmpty() || responsePage.size() < pageSize
                    || maxEpochSecond >= endEpochSecondExclusive) {
                break;
            }
            fromSecond = Math.addExact(maxEpochSecond, 1L);
        }
    }

    @Override
    public OjSubmissionCollectionWriteResult writeBatch(
            String ojName,
            List<OjHandleCollectionOutcome> outcomes
    ) throws JsonProcessingException {
        ArrayNode submissionArray = objectMapper.createArrayNode();
        outcomes.forEach(outcome -> outcome.submissions()
                .forEach(submission -> submissionArray.add(submission.deepCopy())));
        AtcoderOdsBatchUpsertResult upsertResult = ingestService.upsertSubmissions(
                submissionArray,
                collectorBatchIdPrefix(ojName)
        );
        return new OjSubmissionCollectionWriteResult(
                upsertResult.batchId(),
                upsertResult.tableName(),
                upsertResult.writtenRows(),
                upsertResult.fetchedAt()
        );
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

}
