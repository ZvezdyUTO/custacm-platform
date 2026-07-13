package com.custacm.platform.trainingdata.codeforces.app;

import com.custacm.platform.trainingdata.codeforces.domain.CodeforcesSubmissionSourceClient;
import com.custacm.platform.trainingdata.common.collector.AbstractOjSubmissionCollectionAdapter;
import com.custacm.platform.trainingdata.common.collector.OjCollectionRequestExecutor;
import com.custacm.platform.trainingdata.common.collector.OjSubmissionWindowFilter;
import com.custacm.platform.trainingdata.common.collector.result.OjHandleCollectionOutcome;
import com.custacm.platform.trainingdata.common.collector.result.OjSubmissionCollectionWriteResult;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

final class CodeforcesSubmissionCollectionAdapter extends AbstractOjSubmissionCollectionAdapter {
    private final CodeforcesSubmissionSourceClient sourceClient;
    private final CodeforcesOdsSubmissionIngestService ingestService;
    private final ObjectMapper objectMapper;
    private final int pageSize;

    CodeforcesSubmissionCollectionAdapter(
            CodeforcesSubmissionSourceClient sourceClient,
            CodeforcesOdsSubmissionIngestService ingestService,
            ObjectMapper objectMapper,
            int pageSize
    ) {
        super(CodeforcesSubmissionCollectionAdapter.class);
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
        return OjNames.CODEFORCES;
    }

    @Override
    public String displayName(String ojName) {
        if (OjNames.CODEFORCES.equals(ojName)) {
            return "Codeforces";
        }
        return ojName;
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
        int from = 1;
        while (true) {
            int currentFrom = from;
            JsonNode responsePage = requestExecutor.execute(
                    () -> sourceClient.fetchUserStatus(normalizedHandle, currentFrom, pageSize)
            );
            if (!responsePage.isArray()) {
                throw new IllegalArgumentException("Codeforces user.status result must be an array");
            }
            progress.addFetchedSubmissionCount(responsePage.size());
            var filteredPage = OjSubmissionWindowFilter.filterSortedPage(
                    responsePage,
                    windowStartInclusive,
                    windowEndExclusive,
                    CodeforcesSubmissionCollectionAdapter::creationTimeSeconds
            );
            progress.addMatchedSubmissions(filteredPage.matchedSubmissions());
            boolean reachedSourceEnd = responsePage.size() < pageSize;
            if (reachedSourceEnd || filteredPage.allSubmissionsAreOlderThanWindow()) {
                break;
            }
            from += pageSize;
        }
    }

    @Override
    public OjSubmissionCollectionWriteResult writeBatch(
            String ojName,
            List<OjHandleCollectionOutcome> outcomes
    ) throws JsonProcessingException {
        Map<String, ArrayNode> submissionsByHandle = new LinkedHashMap<>();
        for (OjHandleCollectionOutcome outcome : outcomes) {
            ArrayNode handleSubmissions = submissionsByHandle.computeIfAbsent(
                    outcome.result().handle(),
                    ignored -> objectMapper.createArrayNode()
            );
            outcome.submissions().forEach(submission -> handleSubmissions.add(submission.deepCopy()));
        }
        CodeforcesOdsBatchUpsertResult upsertResult = ingestService.upsertSubmissions(
                submissionsByHandle,
                collectorBatchIdPrefix(ojName)
        );
        return new OjSubmissionCollectionWriteResult(
                upsertResult.batchId(),
                upsertResult.tableName(),
                upsertResult.writtenRows(),
                upsertResult.fetchedAt()
        );
    }

    private static OptionalLong creationTimeSeconds(JsonNode submission) {
        JsonNode value = submission.path("creationTimeSeconds");
        if (!value.canConvertToLong()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(value.asLong());
    }

}
