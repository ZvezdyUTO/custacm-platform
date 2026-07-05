package com.custacm.platform.trainingdata.codeforces.app.service;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesSubmissionItem;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesSubmissionRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CodeforcesSubmissionQueryService {
    private final CodeforcesSubmissionRepository repository;
    private final CodeforcesHandleAccountService handleAccountService;

    public CodeforcesSubmissionQueryService(
            CodeforcesSubmissionRepository repository,
            CodeforcesHandleAccountService handleAccountService
    ) {
        this.repository = repository;
        this.handleAccountService = handleAccountService;
    }

    public CodeforcesHandleSubmissionReport listStudentSubmissions(
            String studentIdentity,
            LocalDateTime submittedFromUtcPlus8,
            LocalDateTime submittedToUtcPlus8,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        CodeforcesHandleAccount account = handleAccountService.getByStudentIdentity(studentIdentity);
        CodeforcesHandleSubmissionCriteria query = new CodeforcesHandleSubmissionCriteria(
                account.handle(),
                submittedFromUtcPlus8,
                submittedToUtcPlus8,
                minProblemRating,
                maxProblemRating
        );
        List<CodeforcesSubmission> rows = repository.findHandleSubmissions(query);
        return new CodeforcesHandleSubmissionReport(
                account.studentIdentity(),
                account.handle(),
                submissionItems(rows, account.studentIdentity())
        );
    }

    public CodeforcesProblemSubmissionReport listProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
        List<CodeforcesSubmission> rows = repository.findProblemSubmissions(query);
        Map<String, String> studentIdentityByHandle = studentIdentityByHandle(rows);
        return new CodeforcesProblemSubmissionReport(
                query.problemKey(),
                submissionItems(rows, studentIdentityByHandle)
        );
    }

    private static List<CodeforcesSubmissionItem> submissionItems(
            List<CodeforcesSubmission> rows,
            String studentIdentity
    ) {
        return rows.stream()
                .map(row -> toSubmissionItem(row, studentIdentity))
                .toList();
    }

    private static List<CodeforcesSubmissionItem> submissionItems(
            List<CodeforcesSubmission> rows,
            Map<String, String> studentIdentityByHandle
    ) {
        return rows.stream()
                .map(row -> toSubmissionItem(row, studentIdentityByHandle.get(row.authorHandle())))
                .toList();
    }

    private Map<String, String> studentIdentityByHandle(List<CodeforcesSubmission> rows) {
        Map<String, String> studentIdentityByHandle = new LinkedHashMap<>();
        for (CodeforcesSubmission row : rows) {
            studentIdentityByHandle.computeIfAbsent(
                    row.authorHandle(),
                    handle -> handleAccountService.getByHandle(handle).studentIdentity()
            );
        }
        return studentIdentityByHandle;
    }

    private static CodeforcesSubmissionItem toSubmissionItem(CodeforcesSubmission row, String studentIdentity) {
        return new CodeforcesSubmissionItem(
                row.codeforcesSubmissionId(),
                studentIdentity,
                row.authorHandle(),
                row.contestId(),
                row.submittedAtUtcPlus8(),
                row.submittedDateUtcPlus8(),
                row.relativeTimeSeconds(),
                row.problemKey(),
                row.problemContestId(),
                row.problemIndex(),
                row.problemName(),
                row.problemType(),
                row.problemPoints(),
                row.problemRating(),
                row.problemTagsJson(),
                row.authorParticipantType(),
                row.programmingLanguage(),
                row.verdict(),
                row.accepted(),
                row.testset(),
                row.passedTestCount(),
                row.timeConsumedMillis(),
                row.memoryConsumedBytes()
        );
    }
}
