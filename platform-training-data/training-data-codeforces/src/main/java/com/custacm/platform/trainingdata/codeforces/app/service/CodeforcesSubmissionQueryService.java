package com.custacm.platform.trainingdata.codeforces.app.service;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesSubmissionItem;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesSubmissionRepository;

import java.util.List;

public class CodeforcesSubmissionQueryService {
    private final CodeforcesSubmissionRepository repository;

    public CodeforcesSubmissionQueryService(CodeforcesSubmissionRepository repository) {
        this.repository = repository;
    }

    public CodeforcesHandleSubmissionReport listHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
        List<CodeforcesSubmission> rows = repository.findHandleSubmissions(query);
        return new CodeforcesHandleSubmissionReport(
                query.authorHandle(),
                submissionItems(rows)
        );
    }

    public CodeforcesProblemSubmissionReport listProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
        List<CodeforcesSubmission> rows = repository.findProblemSubmissions(query);
        return new CodeforcesProblemSubmissionReport(
                query.problemKey(),
                submissionItems(rows)
        );
    }

    private static List<CodeforcesSubmissionItem> submissionItems(List<CodeforcesSubmission> rows) {
        return rows.stream()
                .map(CodeforcesSubmissionQueryService::toSubmissionItem)
                .toList();
    }

    private static CodeforcesSubmissionItem toSubmissionItem(CodeforcesSubmission row) {
        return new CodeforcesSubmissionItem(
                row.codeforcesSubmissionId(),
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
