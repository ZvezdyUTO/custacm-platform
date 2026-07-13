package com.custacm.platform.trainingdata.common.app.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.query.result.OjAcceptedSummaryReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemSubmissionReport;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

/**
 * OJ warehouse query entry point shared by transport adapters.
 *
 * @author huangbingrui.awa
 */
public class OjWarehouseQueryFacade {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 15;
    private static final int MAX_LIMIT = 2000;

    private final OjAcceptedSummaryQueryService acceptedSummaryQueryService;
    private final OjSubmissionQueryService submissionQueryService;
    private final OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService;

    public OjWarehouseQueryFacade(
            OjAcceptedSummaryQueryService acceptedSummaryQueryService,
            OjSubmissionQueryService submissionQueryService,
            OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService
    ) {
        this.acceptedSummaryQueryService = acceptedSummaryQueryService;
        this.submissionQueryService = submissionQueryService;
        this.firstAcceptedProblemQueryService = firstAcceptedProblemQueryService;
    }

    public OjAcceptedSummaryReport summarizeAcceptedProblems(
            String ojName,
            String username,
            String acceptedFromDateUtcPlus8,
            String acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        return acceptedSummaryQueryService.summarizeStudentAcceptedProblems(
                requireText(ojName, "ojName", OjWarehouseQueryFacade::invalidRequest),
                requireText(username, "username", OjWarehouseQueryFacade::invalidRequest),
                parseLocalDate(acceptedFromDateUtcPlus8, "acceptedFromDateUtcPlus8"),
                parseLocalDate(acceptedToDateUtcPlus8, "acceptedToDateUtcPlus8"),
                minProblemRating,
                maxProblemRating
        );
    }

    public List<OjAcceptedSummaryReport> summarizeAcceptedProblems(
            String ojName,
            boolean includeRetired,
            String acceptedFromDateUtcPlus8,
            String acceptedToDateUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        return acceptedSummaryQueryService.summarizeStudentsAcceptedProblems(
                requireText(ojName, "ojName", OjWarehouseQueryFacade::invalidRequest),
                includeRetired,
                parseLocalDate(acceptedFromDateUtcPlus8, "acceptedFromDateUtcPlus8"),
                parseLocalDate(acceptedToDateUtcPlus8, "acceptedToDateUtcPlus8"),
                minProblemRating,
                maxProblemRating
        );
    }

    public OjHandleSubmissionReport listStudentSubmissions(
            String ojName,
            String username,
            String submittedFromUtcPlus8,
            String submittedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating,
            Integer page,
            Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        return submissionQueryService.listStudentSubmissions(
                requireText(ojName, "ojName", OjWarehouseQueryFacade::invalidRequest),
                requireText(username, "username", OjWarehouseQueryFacade::invalidRequest),
                parseLocalDateTime(submittedFromUtcPlus8, "submittedFromUtcPlus8"),
                parseLocalDateTime(submittedToUtcPlus8, "submittedToUtcPlus8"),
                minProblemRating,
                maxProblemRating,
                normalizedPage,
                normalizedLimit
        );
    }

    public OjProblemSubmissionReport listProblemSubmissions(
            String ojName,
            String problemKey,
            String submittedFromUtcPlus8,
            String submittedToUtcPlus8,
            Integer page,
            Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        return submissionQueryService.listProblemSubmissions(new OjProblemSubmissionCriteria(
                requireText(ojName, "ojName", OjWarehouseQueryFacade::invalidRequest),
                requireText(problemKey, "problemKey", OjWarehouseQueryFacade::invalidRequest),
                parseLocalDateTime(submittedFromUtcPlus8, "submittedFromUtcPlus8"),
                parseLocalDateTime(submittedToUtcPlus8, "submittedToUtcPlus8"),
                normalizedLimit,
                offset(normalizedPage, normalizedLimit)
        ));
    }

    public OjHandleFirstAcceptedProblemReport summarizeStudentFirstAcceptedProblems(
            String ojName,
            String username,
            String firstAcceptedFromUtcPlus8,
            String firstAcceptedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating,
            Integer page,
            Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        return firstAcceptedProblemQueryService.summarizeStudentFirstAcceptedProblems(
                requireText(ojName, "ojName", OjWarehouseQueryFacade::invalidRequest),
                requireText(username, "username", OjWarehouseQueryFacade::invalidRequest),
                parseLocalDateTime(firstAcceptedFromUtcPlus8, "firstAcceptedFromUtcPlus8"),
                parseLocalDateTime(firstAcceptedToUtcPlus8, "firstAcceptedToUtcPlus8"),
                minProblemRating,
                maxProblemRating,
                normalizedPage,
                normalizedLimit
        );
    }

    public OjProblemFirstAcceptedHandleReport summarizeProblemFirstAcceptedHandles(
            String ojName,
            String problemKey,
            String firstAcceptedFromUtcPlus8,
            String firstAcceptedToUtcPlus8,
            Integer page,
            Integer limit
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedLimit = normalizeLimit(limit);
        return firstAcceptedProblemQueryService.summarizeProblemFirstAcceptedHandles(
                new OjProblemFirstAcceptedHandleCriteria(
                        requireText(ojName, "ojName", OjWarehouseQueryFacade::invalidRequest),
                        requireText(problemKey, "problemKey", OjWarehouseQueryFacade::invalidRequest),
                        parseLocalDateTime(firstAcceptedFromUtcPlus8, "firstAcceptedFromUtcPlus8"),
                        parseLocalDateTime(firstAcceptedToUtcPlus8, "firstAcceptedToUtcPlus8"),
                        normalizedLimit,
                        offset(normalizedPage, normalizedLimit)
                )
        );
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
