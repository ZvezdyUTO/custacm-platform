package com.custacm.platform.trainingdata.common.app.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjFirstAcceptedProblem;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OjFirstAcceptedProblemQueryService {
    private final OjFirstAcceptedProblemRepository repository;
    private final OjHandleAccountService handleAccountService;

    public OjFirstAcceptedProblemQueryService(
            OjFirstAcceptedProblemRepository repository,
            OjHandleAccountService handleAccountService
    ) {
        this.repository = repository;
        this.handleAccountService = handleAccountService;
    }

    public OjHandleFirstAcceptedProblemReport summarizeStudentFirstAcceptedProblems(
            String studentIdentity,
            LocalDateTime firstAcceptedFromUtcPlus8,
            LocalDateTime firstAcceptedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating,
            int page,
            int limit
    ) {
        return summarizeStudentFirstAcceptedProblems(
                OjNames.CODEFORCES,
                studentIdentity,
                firstAcceptedFromUtcPlus8,
                firstAcceptedToUtcPlus8,
                minProblemRating,
                maxProblemRating,
                page,
                limit
        );
    }

    public OjHandleFirstAcceptedProblemReport summarizeStudentFirstAcceptedProblems(
            String ojName,
            String studentIdentity,
            LocalDateTime firstAcceptedFromUtcPlus8,
            LocalDateTime firstAcceptedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating,
            int page,
            int limit
    ) {
        OjHandleAccount account = handleAccountService.getByStudentIdentity(studentIdentity);
        String ojHandle = handleAccountService.getHandle(account, ojName);
        OjHandleFirstAcceptedProblemCriteria query = new OjHandleFirstAcceptedProblemCriteria(
                ojName,
                ojHandle,
                firstAcceptedFromUtcPlus8,
                firstAcceptedToUtcPlus8,
                minProblemRating,
                maxProblemRating,
                limit,
                offset(page, limit)
        );
        long total = repository.countHandleFirstAcceptedProblems(query);
        long totalPages = totalPages(total, limit);
        if (query.offset() >= total) {
            return new OjHandleFirstAcceptedProblemReport(
                    account.studentIdentity(),
                    ojHandle,
                    Math.toIntExact(total),
                    page,
                    limit,
                    total,
                    totalPages,
                    false,
                    List.of()
            );
        }
        List<OjHandleFirstAcceptedProblemReport.OjFirstAcceptedProblemItem> problems =
                repository.findHandleFirstAcceptedProblems(query).stream()
                        .map(OjFirstAcceptedProblemQueryService::toProblemItem)
                        .toList();
        return new OjHandleFirstAcceptedProblemReport(
                account.studentIdentity(),
                ojHandle,
                Math.toIntExact(total),
                page,
                limit,
                total,
                totalPages,
                page < totalPages,
                problems
        );
    }

    public OjProblemFirstAcceptedHandleReport summarizeProblemFirstAcceptedHandles(
            OjProblemFirstAcceptedHandleCriteria query
    ) {
        long total = repository.countProblemFirstAcceptedHandles(query);
        long totalPages = totalPages(total, query.limit());
        int page = pageFrom(query);
        if (query.offset() >= total) {
            return new OjProblemFirstAcceptedHandleReport(
                    query.problemKey(),
                    Math.toIntExact(total),
                    page,
                    query.limit(),
                    total,
                    totalPages,
                    false,
                    List.of()
            );
        }
        List<OjFirstAcceptedProblem> rows = repository.findProblemFirstAcceptedHandles(query);
        Map<String, String> studentIdentityByHandle = studentIdentityByHandle(query.ojName(), rows);
        List<OjProblemFirstAcceptedHandleReport.OjFirstAcceptedHandle> acceptedHandles =
                rows.stream()
                        .map(row -> new OjProblemFirstAcceptedHandleReport.OjFirstAcceptedHandle(
                                studentIdentityByHandle.get(row.handle()),
                                row.handle(),
                                row.firstAcceptedAtUtcPlus8()
                        ))
                        .distinct()
                        .toList();
        return new OjProblemFirstAcceptedHandleReport(
                query.problemKey(),
                Math.toIntExact(total),
                page,
                query.limit(),
                total,
                totalPages,
                page < totalPages,
                acceptedHandles
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

    private static int pageFrom(OjProblemFirstAcceptedHandleCriteria query) {
        return Math.toIntExact(query.offset() / query.limit() + 1);
    }

    private Map<String, String> studentIdentityByHandle(String ojName, List<OjFirstAcceptedProblem> rows) {
        Map<String, String> studentIdentityByHandle = new LinkedHashMap<>();
        for (OjFirstAcceptedProblem row : rows) {
            studentIdentityByHandle.computeIfAbsent(
                    row.handle(),
                    handle -> handleAccountService.getByHandle(ojName, handle).studentIdentity()
            );
        }
        return studentIdentityByHandle;
    }

    private static OjHandleFirstAcceptedProblemReport.OjFirstAcceptedProblemItem toProblemItem(
            OjFirstAcceptedProblem row
    ) {
        return new OjHandleFirstAcceptedProblemReport.OjFirstAcceptedProblemItem(
                row.problemKey(),
                row.problemIndex(),
                row.problemName(),
                row.difficulty(),
                row.firstAcceptedSubmissionId(),
                row.firstAcceptedAtUtcPlus8(),
                row.firstAcceptedDateUtcPlus8(),
                row.firstAcceptedLanguage(),
                row.firstAcceptedSourceUrl()
        );
    }
}
