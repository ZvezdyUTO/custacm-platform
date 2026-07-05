package com.custacm.platform.trainingdata.codeforces.web.collector;

import com.custacm.platform.trainingdata.codeforces.app.collector.CodeforcesSubmissionCollectionService;
import com.custacm.platform.trainingdata.codeforces.web.collector.request.CodeforcesSubmissionCollectionRequest;
import com.custacm.platform.trainingdata.codeforces.web.collector.response.CodeforcesSubmissionCollectionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodeforcesSubmissionCollectionController {
    private final CodeforcesSubmissionCollectionService collectionService;

    public CodeforcesSubmissionCollectionController(CodeforcesSubmissionCollectionService collectionService) {
        this.collectionService = collectionService;
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
}
