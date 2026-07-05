package com.custacm.platform.trainingdata.codeforces.web.controller;

import com.custacm.platform.trainingdata.codeforces.app.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.codeforces.app.service.CodeforcesSubmissionQueryService;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.web.response.CodeforcesAcceptedSummaryResponse;
import com.custacm.platform.trainingdata.codeforces.web.response.CodeforcesProblemFirstAcceptedHandleReportResponse;
import com.custacm.platform.trainingdata.codeforces.web.response.CodeforcesProblemSubmissionReportResponse;
import com.custacm.platform.trainingdata.codeforces.web.response.CodeforcesStudentFirstAcceptedProblemReportResponse;
import com.custacm.platform.trainingdata.codeforces.web.response.CodeforcesStudentSubmissionReportResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/training-data/codeforces")
public class CodeforcesWarehouseQueryController {
    private final CodeforcesAcceptedSummaryQueryService acceptedSummaryQueryService;
    private final CodeforcesSubmissionQueryService submissionQueryService;
    private final CodeforcesFirstAcceptedProblemQueryService firstAcceptedProblemQueryService;

    public CodeforcesWarehouseQueryController(
            CodeforcesAcceptedSummaryQueryService acceptedSummaryQueryService,
            CodeforcesSubmissionQueryService submissionQueryService,
            CodeforcesFirstAcceptedProblemQueryService firstAcceptedProblemQueryService
    ) {
        this.acceptedSummaryQueryService = acceptedSummaryQueryService;
        this.submissionQueryService = submissionQueryService;
        this.firstAcceptedProblemQueryService = firstAcceptedProblemQueryService;
    }

    @GetMapping("/accepted-summary")
    public ResponseEntity<CodeforcesAcceptedSummaryResponse> summarizeAcceptedProblems(
            @RequestParam("studentIdentity") String studentIdentity,
            @RequestParam(value = "acceptedFromDateUtcPlus8", required = false) String acceptedFromDateUtcPlus8,
            @RequestParam(value = "acceptedToDateUtcPlus8", required = false) String acceptedToDateUtcPlus8,
            @RequestParam(value = "minProblemRating", required = false) Integer minProblemRating,
            @RequestParam(value = "maxProblemRating", required = false) Integer maxProblemRating
    ) {
        return ResponseEntity.ok(CodeforcesAcceptedSummaryResponse.from(
                acceptedSummaryQueryService.summarizeStudentAcceptedProblems(
                        requireRequestText(studentIdentity, "studentIdentity"),
                        parseLocalDate(acceptedFromDateUtcPlus8, "acceptedFromDateUtcPlus8"),
                        parseLocalDate(acceptedToDateUtcPlus8, "acceptedToDateUtcPlus8"),
                        minProblemRating,
                        maxProblemRating
                )
        ));
    }

    @GetMapping("/submissions/by-student")
    public ResponseEntity<CodeforcesStudentSubmissionReportResponse> listStudentSubmissions(
            @RequestParam("studentIdentity") String studentIdentity,
            @RequestParam(value = "submittedFromUtcPlus8", required = false) String submittedFromUtcPlus8,
            @RequestParam(value = "submittedToUtcPlus8", required = false) String submittedToUtcPlus8,
            @RequestParam(value = "minProblemRating", required = false) Integer minProblemRating,
            @RequestParam(value = "maxProblemRating", required = false) Integer maxProblemRating
    ) {
        return ResponseEntity.ok(CodeforcesStudentSubmissionReportResponse.from(
                submissionQueryService.listStudentSubmissions(
                        requireRequestText(studentIdentity, "studentIdentity"),
                        parseLocalDateTime(submittedFromUtcPlus8, "submittedFromUtcPlus8"),
                        parseLocalDateTime(submittedToUtcPlus8, "submittedToUtcPlus8"),
                        minProblemRating,
                        maxProblemRating
                )
        ));
    }

    @GetMapping("/submissions/by-problem")
    public ResponseEntity<CodeforcesProblemSubmissionReportResponse> listProblemSubmissions(
            @RequestParam("problemKey") String problemKey,
            @RequestParam(value = "submittedFromUtcPlus8", required = false) String submittedFromUtcPlus8,
            @RequestParam(value = "submittedToUtcPlus8", required = false) String submittedToUtcPlus8
    ) {
        CodeforcesProblemSubmissionCriteria query = new CodeforcesProblemSubmissionCriteria(
                requireRequestText(problemKey, "problemKey"),
                parseLocalDateTime(submittedFromUtcPlus8, "submittedFromUtcPlus8"),
                parseLocalDateTime(submittedToUtcPlus8, "submittedToUtcPlus8")
        );
        return ResponseEntity.ok(CodeforcesProblemSubmissionReportResponse.from(
                submissionQueryService.listProblemSubmissions(query)
        ));
    }

    @GetMapping("/first-accepted/by-student")
    public ResponseEntity<CodeforcesStudentFirstAcceptedProblemReportResponse> summarizeStudentFirstAcceptedProblems(
            @RequestParam("studentIdentity") String studentIdentity,
            @RequestParam(value = "firstAcceptedFromUtcPlus8", required = false) String firstAcceptedFromUtcPlus8,
            @RequestParam(value = "firstAcceptedToUtcPlus8", required = false) String firstAcceptedToUtcPlus8,
            @RequestParam(value = "minProblemRating", required = false) Integer minProblemRating,
            @RequestParam(value = "maxProblemRating", required = false) Integer maxProblemRating
    ) {
        return ResponseEntity.ok(CodeforcesStudentFirstAcceptedProblemReportResponse.from(
                firstAcceptedProblemQueryService.summarizeStudentFirstAcceptedProblems(
                        requireRequestText(studentIdentity, "studentIdentity"),
                        parseLocalDateTime(firstAcceptedFromUtcPlus8, "firstAcceptedFromUtcPlus8"),
                        parseLocalDateTime(firstAcceptedToUtcPlus8, "firstAcceptedToUtcPlus8"),
                        minProblemRating,
                        maxProblemRating
                )
        ));
    }

    @GetMapping("/first-accepted/by-problem")
    public ResponseEntity<CodeforcesProblemFirstAcceptedHandleReportResponse> summarizeProblemFirstAcceptedHandles(
            @RequestParam("problemKey") String problemKey,
            @RequestParam(value = "firstAcceptedFromUtcPlus8", required = false) String firstAcceptedFromUtcPlus8,
            @RequestParam(value = "firstAcceptedToUtcPlus8", required = false) String firstAcceptedToUtcPlus8
    ) {
        CodeforcesProblemFirstAcceptedHandleCriteria query = new CodeforcesProblemFirstAcceptedHandleCriteria(
                requireRequestText(problemKey, "problemKey"),
                parseLocalDateTime(firstAcceptedFromUtcPlus8, "firstAcceptedFromUtcPlus8"),
                parseLocalDateTime(firstAcceptedToUtcPlus8, "firstAcceptedToUtcPlus8")
        );
        return ResponseEntity.ok(CodeforcesProblemFirstAcceptedHandleReportResponse.from(
                firstAcceptedProblemQueryService.summarizeProblemFirstAcceptedHandles(query)
        ));
    }

    private static String requireRequestText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalidRequest(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static LocalDate parseLocalDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw invalidRequest(fieldName + " must be an ISO-8601 date");
        }
    }

    private static LocalDateTime parseLocalDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw invalidRequest(fieldName + " must be an ISO-8601 local date time");
        }
    }

    private static CodeforcesHandleAccountException invalidRequest(String message) {
        return new CodeforcesHandleAccountException(
                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST,
                message
        );
    }
}
