package com.custacm.platform.trainingdata.codeforces.web.query;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.app.query.CodeforcesAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.codeforces.app.query.CodeforcesFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.codeforces.app.query.CodeforcesSubmissionQueryService;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.web.query.response.CodeforcesAcceptedSummaryResponse;
import com.custacm.platform.trainingdata.codeforces.web.query.response.CodeforcesProblemFirstAcceptedHandleReportResponse;
import com.custacm.platform.trainingdata.codeforces.web.query.response.CodeforcesProblemSubmissionReportResponse;
import com.custacm.platform.trainingdata.codeforces.web.query.response.CodeforcesStudentFirstAcceptedProblemReportResponse;
import com.custacm.platform.trainingdata.codeforces.web.query.response.CodeforcesStudentSubmissionReportResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/training-data/codeforces")
public class CodeforcesWarehouseQueryController {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 2000;

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

    @GetMapping("/accepted-summary/auto-collect-users")
    public ResponseEntity<List<CodeforcesAcceptedSummaryResponse>> summarizeAutoCollectAcceptedProblems(
            @RequestParam(value = "acceptedFromDateUtcPlus8", required = false) String acceptedFromDateUtcPlus8,
            @RequestParam(value = "acceptedToDateUtcPlus8", required = false) String acceptedToDateUtcPlus8,
            @RequestParam(value = "minProblemRating", required = false) Integer minProblemRating,
            @RequestParam(value = "maxProblemRating", required = false) Integer maxProblemRating
    ) {
        return ResponseEntity.ok(acceptedSummaryQueryService.summarizeAutoCollectAcceptedProblems(
                        parseLocalDate(acceptedFromDateUtcPlus8, "acceptedFromDateUtcPlus8"),
                        parseLocalDate(acceptedToDateUtcPlus8, "acceptedToDateUtcPlus8"),
                        minProblemRating,
                        maxProblemRating
                ).stream()
                .map(CodeforcesAcceptedSummaryResponse::from)
                .toList());
    }

    @GetMapping("/submissions/by-student")
    public ResponseEntity<CodeforcesStudentSubmissionReportResponse> listStudentSubmissions(
            @RequestParam("studentIdentity") String studentIdentity,
            @RequestParam(value = "submittedFromUtcPlus8", required = false) String submittedFromUtcPlus8,
            @RequestParam(value = "submittedToUtcPlus8", required = false) String submittedToUtcPlus8,
            @RequestParam(value = "minProblemRating", required = false) Integer minProblemRating,
            @RequestParam(value = "maxProblemRating", required = false) Integer maxProblemRating,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        return ResponseEntity.ok(CodeforcesStudentSubmissionReportResponse.from(
                submissionQueryService.listStudentSubmissions(
                        requireRequestText(studentIdentity, "studentIdentity"),
                        parseLocalDateTime(submittedFromUtcPlus8, "submittedFromUtcPlus8"),
                        parseLocalDateTime(submittedToUtcPlus8, "submittedToUtcPlus8"),
                        minProblemRating,
                        maxProblemRating,
                        normalizedPage,
                        normalizedLimit
                )
        ));
    }

    @GetMapping("/submissions/by-problem")
    public ResponseEntity<CodeforcesProblemSubmissionReportResponse> listProblemSubmissions(
            @RequestParam("problemKey") String problemKey,
            @RequestParam(value = "submittedFromUtcPlus8", required = false) String submittedFromUtcPlus8,
            @RequestParam(value = "submittedToUtcPlus8", required = false) String submittedToUtcPlus8,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        CodeforcesProblemSubmissionCriteria query = new CodeforcesProblemSubmissionCriteria(
                requireRequestText(problemKey, "problemKey"),
                parseLocalDateTime(submittedFromUtcPlus8, "submittedFromUtcPlus8"),
                parseLocalDateTime(submittedToUtcPlus8, "submittedToUtcPlus8"),
                normalizedLimit,
                offset(normalizedPage, normalizedLimit)
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

    private static int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw invalidRequest("page must be greater than or equal to 1");
        }
        return page;
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw invalidRequest("limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private static long offset(int page, int limit) {
        return (long) (page - 1) * limit;
    }

    private static CodeforcesHandleAccountException invalidRequest(String message) {
        return new CodeforcesHandleAccountException(
                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST,
                message
        );
    }
}
