package com.custacm.platform.trainingdata.common.web.collector;

import com.custacm.platform.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.custacm.platform.trainingdata.common.scheduler.OjScheduledSubmissionCollectionService;
import com.custacm.platform.trainingdata.common.web.collector.request.OjSubmissionCollectionJobStartRequest;
import com.custacm.platform.trainingdata.common.web.collector.request.OjSubmissionCollectionRequest;
import com.custacm.platform.trainingdata.common.web.collector.response.OjSubmissionCollectionJobResponse;
import com.custacm.platform.trainingdata.common.web.collector.response.OjSubmissionCollectionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OjSubmissionCollectionController {
    private final OjScheduledSubmissionCollectionService collectionService;
    private final OjSubmissionCollectionJobService collectionJobService;

    public OjSubmissionCollectionController(
            OjScheduledSubmissionCollectionService collectionService,
            OjSubmissionCollectionJobService collectionJobService
    ) {
        this.collectionService = collectionService;
        this.collectionJobService = collectionJobService;
    }

    @PostMapping("/api/training-data/admin/codeforces/submissions:collect")
    public OjSubmissionCollectionResponse collectSubmissions(
            @RequestBody OjSubmissionCollectionRequest request
    ) throws JsonProcessingException {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be empty");
        }
        return OjSubmissionCollectionResponse.from(
                collectionService.collectRecentWindowForStudentIdentity(
                        request.optionalOjName(),
                        request.requireStudentIdentity(),
                        request.requireLookbackDuration()
                )
        );
    }

    @PostMapping("/api/training-data/admin/codeforces/submissions:collect-batch-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OjSubmissionCollectionJobResponse startCollectionJob(
            @RequestBody OjSubmissionCollectionJobStartRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be empty");
        }
        return OjSubmissionCollectionJobResponse.from(
                collectionJobService.startBatchCollection(
                        request.requireStudentIdentities(),
                        request.requireLookbackDuration(),
                        request.refreshWarehouseOrDefault(),
                        request.optionalOjName()
                )
        );
    }

    @GetMapping("/api/training-data/admin/codeforces/submissions/collect-batch-jobs")
    public List<OjSubmissionCollectionJobResponse> listCollectionJobs() {
        return collectionJobService.listJobs().stream()
                .map(OjSubmissionCollectionJobResponse::from)
                .toList();
    }
}
