package com.custacm.platform.trainingdata.codeforces.web;

import com.custacm.platform.trainingdata.codeforces.app.CodeforcesOdsSubmissionIngestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/training-data/admin/ods/codeforces")
public class CodeforcesOdsSubmissionIngestController {
    private final CodeforcesOdsSubmissionIngestService ingestService;

    public CodeforcesOdsSubmissionIngestController(CodeforcesOdsSubmissionIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/submissions:batch-upsert")
    public ResponseEntity<CodeforcesOdsBatchUpsertResponse> upsertSubmissions(@RequestBody JsonNode submissions)
            throws JsonProcessingException {
        try {
            var result = ingestService.upsertSubmissions(submissions);
            return ResponseEntity.ok(new CodeforcesOdsBatchUpsertResponse(
                    result.batchId(),
                    result.tableName(),
                    result.writtenRows(),
                    result.fetchedAt()
            ));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
