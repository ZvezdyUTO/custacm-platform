package com.custacm.platform.trainingdata.codeforces.app.service;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesFirstAcceptedProblem;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesFirstAcceptedProblemRepository;

import java.util.Comparator;
import java.util.List;

public class CodeforcesFirstAcceptedProblemQueryService {
    private final CodeforcesFirstAcceptedProblemRepository repository;

    public CodeforcesFirstAcceptedProblemQueryService(CodeforcesFirstAcceptedProblemRepository repository) {
        this.repository = repository;
    }

    public CodeforcesHandleFirstAcceptedProblemReport summarizeHandleFirstAcceptedProblems(
            CodeforcesHandleFirstAcceptedProblemCriteria query
    ) {
        List<CodeforcesHandleFirstAcceptedProblemReport.CodeforcesFirstAcceptedProblemItem> problems =
                repository.findHandleFirstAcceptedProblems(query).stream()
                        .sorted(Comparator
                                .comparing(CodeforcesFirstAcceptedProblem::firstAcceptedAtUtcPlus8)
                                .thenComparing(CodeforcesFirstAcceptedProblem::problemKey))
                        .map(CodeforcesFirstAcceptedProblemQueryService::toProblemItem)
                        .toList();
        return new CodeforcesHandleFirstAcceptedProblemReport(
                query.authorHandle(),
                problems.size(),
                problems
        );
    }

    public CodeforcesProblemFirstAcceptedHandleReport summarizeProblemFirstAcceptedHandles(
            CodeforcesProblemFirstAcceptedHandleCriteria query
    ) {
        List<CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle> acceptedHandles =
                repository.findProblemFirstAcceptedHandles(query).stream()
                .sorted(Comparator
                        .comparing(CodeforcesFirstAcceptedProblem::firstAcceptedAtUtcPlus8)
                        .thenComparing(CodeforcesFirstAcceptedProblem::authorHandle))
                .map(row -> new CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle(
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
