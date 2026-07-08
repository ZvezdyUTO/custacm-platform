package com.custacm.platform.trainingdata.common.app.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjSubmissionItem;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjSubmission;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjSubmissionRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OjSubmissionQueryService {
    private final OjSubmissionRepository repository;
    private final OjHandleAccountService handleAccountService;

    public OjSubmissionQueryService(
            OjSubmissionRepository repository,
            OjHandleAccountService handleAccountService
    ) {
        this.repository = repository;
        this.handleAccountService = handleAccountService;
    }

    public OjHandleSubmissionReport listStudentSubmissions(
            String studentIdentity,
            LocalDateTime submittedFromUtcPlus8,
            LocalDateTime submittedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating,
            int page,
            int limit
    ) {
        return listStudentSubmissions(
                OjNames.CODEFORCES,
                studentIdentity,
                submittedFromUtcPlus8,
                submittedToUtcPlus8,
                minProblemRating,
                maxProblemRating,
                page,
                limit
        );
    }

    public OjHandleSubmissionReport listStudentSubmissions(
            String ojName,
            String studentIdentity,
            LocalDateTime submittedFromUtcPlus8,
            LocalDateTime submittedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating,
            int page,
            int limit
    ) {
        OjHandleAccount account = handleAccountService.getByStudentIdentity(studentIdentity);
        String ojHandle = handleAccountService.getHandle(account, ojName);
        OjHandleSubmissionCriteria query = new OjHandleSubmissionCriteria(
                ojName,
                ojHandle,
                submittedFromUtcPlus8,
                submittedToUtcPlus8,
                minProblemRating,
                maxProblemRating,
                limit,
                offset(page, limit)
        );
        long total = repository.countHandleSubmissions(query);
        long totalPages = totalPages(total, limit);
        if (query.offset() >= total) {
            return new OjHandleSubmissionReport(
                    account.studentIdentity(),
                    ojHandle,
                    page,
                    limit,
                    total,
                    totalPages,
                    false,
                    List.of()
            );
        }
        List<OjSubmission> rows = repository.findHandleSubmissions(query);
        return new OjHandleSubmissionReport(
                account.studentIdentity(),
                ojHandle,
                page,
                limit,
                total,
                totalPages,
                page < totalPages,
                submissionItems(rows, account.studentIdentity())
        );
    }

    public OjProblemSubmissionReport listProblemSubmissions(OjProblemSubmissionCriteria query) {
        long total = repository.countProblemSubmissions(query);
        long totalPages = totalPages(total, query.limit());
        int page = pageFrom(query);
        if (query.offset() >= total) {
            return new OjProblemSubmissionReport(
                    query.problemKey(),
                    page,
                    query.limit(),
                    total,
                    totalPages,
                    false,
                    List.of()
            );
        }
        List<OjSubmission> rows = repository.findProblemSubmissions(query);
        Map<String, String> studentIdentityByHandle = studentIdentityByHandle(query.ojName(), rows);
        return new OjProblemSubmissionReport(
                query.problemKey(),
                page,
                query.limit(),
                total,
                totalPages,
                page < totalPages,
                submissionItems(rows, studentIdentityByHandle)
        );
    }

    private static long offset(int page, int limit) {
        return (long) (page - 1) * limit;
    }

    private static long totalPages(long total, int limit) {
        if (total == 0) {
            return 0;
        }
        return (total - 1) / limit + 1;
    }

    private static int pageFrom(OjProblemSubmissionCriteria query) {
        return Math.toIntExact(query.offset() / query.limit() + 1);
    }

    private static List<OjSubmissionItem> submissionItems(
            List<OjSubmission> rows,
            String studentIdentity
    ) {
        return rows.stream()
                .map(row -> toSubmissionItem(row, studentIdentity))
                .toList();
    }

    private static List<OjSubmissionItem> submissionItems(
            List<OjSubmission> rows,
            Map<String, String> studentIdentityByHandle
    ) {
        return rows.stream()
                .map(row -> toSubmissionItem(row, studentIdentityByHandle.get(row.handle())))
                .toList();
    }

    private Map<String, String> studentIdentityByHandle(String ojName, List<OjSubmission> rows) {
        Map<String, String> studentIdentityByHandle = new LinkedHashMap<>();
        for (OjSubmission row : rows) {
            studentIdentityByHandle.computeIfAbsent(
                    row.handle(),
                    handle -> handleAccountService.getByHandle(ojName, handle).studentIdentity()
            );
        }
        return studentIdentityByHandle;
    }

    private static OjSubmissionItem toSubmissionItem(OjSubmission row, String studentIdentity) {
        return new OjSubmissionItem(
                row.submissionId(),
                studentIdentity,
                row.handle(),
                row.submittedAtUtcPlus8(),
                row.submittedDateUtcPlus8(),
                row.problemKey(),
                row.problemIndex(),
                row.problemName(),
                row.difficulty(),
                row.language(),
                row.verdict(),
                row.accepted(),
                row.timeConsumedMillis(),
                row.sourceUrl()
        );
    }
}
