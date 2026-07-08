package com.custacm.platform.trainingdata.common.web.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.query.OjAcceptedSummaryQueryService;
import com.custacm.platform.trainingdata.common.app.query.OjFirstAcceptedProblemQueryService;
import com.custacm.platform.trainingdata.common.app.query.OjSubmissionQueryService;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import com.custacm.platform.trainingdata.common.web.query.response.OjAcceptedSummaryResponse;
import com.custacm.platform.trainingdata.common.web.query.response.OjProblemFirstAcceptedHandleReportResponse;
import com.custacm.platform.trainingdata.common.web.query.response.OjProblemSubmissionReportResponse;
import com.custacm.platform.trainingdata.common.web.query.response.OjStudentFirstAcceptedProblemReportResponse;
import com.custacm.platform.trainingdata.common.web.query.response.OjStudentSubmissionReportResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

@RestController
@RequestMapping("/api/training-data/codeforces")
public class OjWarehouseQueryController {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 15;
    private static final int MAX_LIMIT = 2000;

    private final OjAcceptedSummaryQueryService acceptedSummaryQueryService;
    private final OjSubmissionQueryService submissionQueryService;
    private final OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService;

    public OjWarehouseQueryController(
            OjAcceptedSummaryQueryService acceptedSummaryQueryService,
            OjSubmissionQueryService submissionQueryService,
            OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService
    ) {
        this.acceptedSummaryQueryService = acceptedSummaryQueryService;
        this.submissionQueryService = submissionQueryService;
        this.firstAcceptedProblemQueryService = firstAcceptedProblemQueryService;
    }

    @GetMapping("/accepted-summary")
    public ResponseEntity<OjAcceptedSummaryResponse> summarizeAcceptedProblems(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam("studentIdentity") String studentIdentity,
            @RequestParam(value = "acceptedFromDateUtcPlus8", required = false) String acceptedFromDateUtcPlus8,
            @RequestParam(value = "acceptedToDateUtcPlus8", required = false) String acceptedToDateUtcPlus8,
            @RequestParam(value = "minProblemRating", required = false) Integer minProblemRating,
            @RequestParam(value = "maxProblemRating", required = false) Integer maxProblemRating
    ) {
        return ResponseEntity.ok(OjAcceptedSummaryResponse.from(
                acceptedSummaryQueryService.summarizeStudentAcceptedProblems(
                        requireText(ojName, "ojName", OjWarehouseQueryController::invalidRequest),
                        requireText(studentIdentity, "studentIdentity", OjWarehouseQueryController::invalidRequest),
                        parseLocalDate(acceptedFromDateUtcPlus8, "acceptedFromDateUtcPlus8"),
                        parseLocalDate(acceptedToDateUtcPlus8, "acceptedToDateUtcPlus8"),
                        minProblemRating,
                        maxProblemRating
                )
        ));
    }

    @GetMapping("/submissions/by-student")
    public ResponseEntity<OjStudentSubmissionReportResponse> listStudentSubmissions(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
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
        return ResponseEntity.ok(OjStudentSubmissionReportResponse.from(
                submissionQueryService.listStudentSubmissions(
                        requireText(ojName, "ojName", OjWarehouseQueryController::invalidRequest),
                        requireText(studentIdentity, "studentIdentity", OjWarehouseQueryController::invalidRequest),
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
    public ResponseEntity<OjProblemSubmissionReportResponse> listProblemSubmissions(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam("problemKey") String problemKey,
            @RequestParam(value = "submittedFromUtcPlus8", required = false) String submittedFromUtcPlus8,
            @RequestParam(value = "submittedToUtcPlus8", required = false) String submittedToUtcPlus8,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        OjProblemSubmissionCriteria query = new OjProblemSubmissionCriteria(
                requireText(ojName, "ojName", OjWarehouseQueryController::invalidRequest),
                requireText(problemKey, "problemKey", OjWarehouseQueryController::invalidRequest),
                parseLocalDateTime(submittedFromUtcPlus8, "submittedFromUtcPlus8"),
                parseLocalDateTime(submittedToUtcPlus8, "submittedToUtcPlus8"),
                normalizedLimit,
                offset(normalizedPage, normalizedLimit)
        );
        return ResponseEntity.ok(OjProblemSubmissionReportResponse.from(
                submissionQueryService.listProblemSubmissions(query)
        ));
    }

    @GetMapping("/first-accepted/by-student")
    public ResponseEntity<OjStudentFirstAcceptedProblemReportResponse> summarizeStudentFirstAcceptedProblems(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam("studentIdentity") String studentIdentity,
            @RequestParam(value = "firstAcceptedFromUtcPlus8", required = false) String firstAcceptedFromUtcPlus8,
            @RequestParam(value = "firstAcceptedToUtcPlus8", required = false) String firstAcceptedToUtcPlus8,
            @RequestParam(value = "minProblemRating", required = false) Integer minProblemRating,
            @RequestParam(value = "maxProblemRating", required = false) Integer maxProblemRating,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        return ResponseEntity.ok(OjStudentFirstAcceptedProblemReportResponse.from(
                firstAcceptedProblemQueryService.summarizeStudentFirstAcceptedProblems(
                        requireText(ojName, "ojName", OjWarehouseQueryController::invalidRequest),
                        requireText(studentIdentity, "studentIdentity", OjWarehouseQueryController::invalidRequest),
                        parseLocalDateTime(firstAcceptedFromUtcPlus8, "firstAcceptedFromUtcPlus8"),
                        parseLocalDateTime(firstAcceptedToUtcPlus8, "firstAcceptedToUtcPlus8"),
                        minProblemRating,
                        maxProblemRating,
                        normalizedPage,
                        normalizedLimit
                )
        ));
    }

    @GetMapping("/first-accepted/by-problem")
    public ResponseEntity<OjProblemFirstAcceptedHandleReportResponse> summarizeProblemFirstAcceptedHandles(
            @RequestParam(value = "ojName", defaultValue = OjNames.CODEFORCES) String ojName,
            @RequestParam("problemKey") String problemKey,
            @RequestParam(value = "firstAcceptedFromUtcPlus8", required = false) String firstAcceptedFromUtcPlus8,
            @RequestParam(value = "firstAcceptedToUtcPlus8", required = false) String firstAcceptedToUtcPlus8,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        OjProblemFirstAcceptedHandleCriteria query = new OjProblemFirstAcceptedHandleCriteria(
                requireText(ojName, "ojName", OjWarehouseQueryController::invalidRequest),
                requireText(problemKey, "problemKey", OjWarehouseQueryController::invalidRequest),
                parseLocalDateTime(firstAcceptedFromUtcPlus8, "firstAcceptedFromUtcPlus8"),
                parseLocalDateTime(firstAcceptedToUtcPlus8, "firstAcceptedToUtcPlus8"),
                normalizedLimit,
                offset(normalizedPage, normalizedLimit)
        );
        return ResponseEntity.ok(OjProblemFirstAcceptedHandleReportResponse.from(
                firstAcceptedProblemQueryService.summarizeProblemFirstAcceptedHandles(query)
        ));
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

    private static OjHandleAccountException invalidRequest(String message) {
        return new OjHandleAccountException(
                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_INVALID_REQUEST,
                message
        );
    }
}
