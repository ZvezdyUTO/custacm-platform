package com.custacm.platform.trainingdata.codeforces.web.collector;

import com.custacm.platform.trainingdata.codeforces.app.collector.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.app.collector.job.CodeforcesSubmissionCollectionJobService;
import com.custacm.platform.trainingdata.codeforces.web.collector.request.CodeforcesSubmissionCollectionJobStartRequest;
import com.custacm.platform.trainingdata.codeforces.web.collector.request.CodeforcesSubmissionCollectionRequest;
import com.custacm.platform.trainingdata.codeforces.web.collector.response.CodeforcesSubmissionCollectionJobResponse;
import com.custacm.platform.trainingdata.codeforces.web.collector.response.CodeforcesSubmissionCollectionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CodeforcesSubmissionCollectionController {
    private final CodeforcesSubmissionCollectionService collectionService;
    private final CodeforcesSubmissionCollectionJobService collectionJobService;

    public CodeforcesSubmissionCollectionController(
            CodeforcesSubmissionCollectionService collectionService,
            CodeforcesSubmissionCollectionJobService collectionJobService
    ) {
        this.collectionService = collectionService;
        this.collectionJobService = collectionJobService;
    }

    @PostMapping("/api/training-data/admin/codeforces/submissions:collect")
    public CodeforcesSubmissionCollectionResponse collectSubmissions(
            @RequestBody CodeforcesSubmissionCollectionRequest request
    ) throws JsonProcessingException {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be empty");
        }
        return CodeforcesSubmissionCollectionResponse.from(
                collectionService.collectRecentWindowForStudentIdentity(
                        request.requireStudentIdentity(),
                        request.requireLookbackDuration()
                )
        );
    }

    @PostMapping("/api/training-data/admin/codeforces/submissions:collect-batch-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CodeforcesSubmissionCollectionJobResponse startCollectionJob(
            @RequestBody CodeforcesSubmissionCollectionJobStartRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("request body must not be empty");
        }
        return CodeforcesSubmissionCollectionJobResponse.from(
                collectionJobService.startBatchCollection(
                        request.requireStudentIdentities(),
                        request.requireLookbackDuration(),
                        request.refreshWarehouseOrDefault()
                )
        );
    }

    @GetMapping("/api/training-data/admin/codeforces/submissions/collect-batch-jobs")
    public List<CodeforcesSubmissionCollectionJobResponse> listCollectionJobs() {
        return collectionJobService.listJobs().stream()
                .map(CodeforcesSubmissionCollectionJobResponse::from)
                .toList();
    }

    @GetMapping("/api/training-data/admin/codeforces/submissions/collect-batch-jobs/{jobId}")
    public CodeforcesSubmissionCollectionJobResponse getCollectionJob(@PathVariable("jobId") String jobId) {
        return CodeforcesSubmissionCollectionJobResponse.from(collectionJobService.getJob(jobId));
    }
}
