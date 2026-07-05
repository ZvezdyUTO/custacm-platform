package com.custacm.platform.trainingdata.codeforces.app.service;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesFirstAcceptedProblem;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesFirstAcceptedProblemRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CodeforcesFirstAcceptedProblemQueryService {
    private final CodeforcesFirstAcceptedProblemRepository repository;
    private final CodeforcesHandleAccountService handleAccountService;

    public CodeforcesFirstAcceptedProblemQueryService(
            CodeforcesFirstAcceptedProblemRepository repository,
            CodeforcesHandleAccountService handleAccountService
    ) {
        this.repository = repository;
        this.handleAccountService = handleAccountService;
    }

    public CodeforcesHandleFirstAcceptedProblemReport summarizeStudentFirstAcceptedProblems(
            String studentIdentity,
            LocalDateTime firstAcceptedFromUtcPlus8,
            LocalDateTime firstAcceptedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        CodeforcesHandleAccount account = handleAccountService.getByStudentIdentity(studentIdentity);
        CodeforcesHandleFirstAcceptedProblemCriteria query = new CodeforcesHandleFirstAcceptedProblemCriteria(
                account.handle(),
                firstAcceptedFromUtcPlus8,
                firstAcceptedToUtcPlus8,
                minProblemRating,
                maxProblemRating
        );
        List<CodeforcesHandleFirstAcceptedProblemReport.CodeforcesFirstAcceptedProblemItem> problems =
                repository.findHandleFirstAcceptedProblems(query).stream()
                        .sorted(Comparator
                                .comparing(CodeforcesFirstAcceptedProblem::firstAcceptedAtUtcPlus8)
                                .thenComparing(CodeforcesFirstAcceptedProblem::problemKey))
                        .map(CodeforcesFirstAcceptedProblemQueryService::toProblemItem)
                        .toList();
        return new CodeforcesHandleFirstAcceptedProblemReport(
                account.studentIdentity(),
                account.handle(),
                problems.size(),
                problems
        );
    }

    public CodeforcesProblemFirstAcceptedHandleReport summarizeProblemFirstAcceptedHandles(
            CodeforcesProblemFirstAcceptedHandleCriteria query
    ) {
        List<CodeforcesFirstAcceptedProblem> rows = repository.findProblemFirstAcceptedHandles(query);
        Map<String, String> studentIdentityByHandle = studentIdentityByHandle(rows);
        List<CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle> acceptedHandles =
                rows.stream()
                        .sorted(Comparator
                                .comparing(CodeforcesFirstAcceptedProblem::firstAcceptedAtUtcPlus8)
                                .thenComparing(CodeforcesFirstAcceptedProblem::authorHandle))
                        .map(row -> new CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle(
                                studentIdentityByHandle.get(row.authorHandle()),
                                row.authorHandle(),
                                row.firstAcceptedAtUtcPlus8()
                        ))
                        .distinct()
                        .toList();
        return new CodeforcesProblemFirstAcceptedHandleReport(
                query.problemKey(),
                acceptedHandles.size(),
                acceptedHandles
        );
    }

    private Map<String, String> studentIdentityByHandle(List<CodeforcesFirstAcceptedProblem> rows) {
        Map<String, String> studentIdentityByHandle = new LinkedHashMap<>();
        for (CodeforcesFirstAcceptedProblem row : rows) {
            studentIdentityByHandle.computeIfAbsent(
                    row.authorHandle(),
                    handle -> handleAccountService.getByHandle(handle).studentIdentity()
            );
        }
        return studentIdentityByHandle;
    }

    private static CodeforcesHandleFirstAcceptedProblemReport.CodeforcesFirstAcceptedProblemItem toProblemItem(
            CodeforcesFirstAcceptedProblem row
    ) {
        return new CodeforcesHandleFirstAcceptedProblemReport.CodeforcesFirstAcceptedProblemItem(
                row.problemKey(),
                row.problemContestId(),
                row.problemIndex(),
                row.problemName(),
                row.problemType(),
                row.problemPoints(),
                row.problemRating(),
                row.problemTagsJson(),
                row.firstAcceptedSubmissionId(),
                row.firstAcceptedAtUtcPlus8(),
                row.firstAcceptedDateUtcPlus8(),
                row.firstAcceptedLanguage()
        );
    }
}
