package com.custacm.platform.trainingdata.codeforces.app.service;

import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesHandleSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesProblemSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.result.CodeforcesSubmissionItem;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesFirstAcceptedProblem;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesSubmissionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeforcesWarehouseQueryServiceTest {
    @Test
    void returnsHandleSubmissionDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-01T23:59:59");
        CodeforcesHandleSubmissionCriteria query = new CodeforcesHandleSubmissionCriteria(
                "tourist",
                from,
                to,
                null,
                null
        );
        CodeforcesSubmissionRepository repository = new CodeforcesSubmissionRepository() {
            @Override
            public List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(
                        submission(1, "tourist", 800, false),
                        submission(2, "tourist", 800, true),
                        submission(3, "tourist", 1200, true),
                        submission(4, "tourist", null, false)
                );
            }

            @Override
            public List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }
        };
        CodeforcesSubmissionQueryService service = new CodeforcesSubmissionQueryService(repository);

        CodeforcesHandleSubmissionReport report = service.listHandleSubmissions(query);

        assertThat(report.authorHandle()).isEqualTo("tourist");
        assertThat(report.submissions()).containsExactly(
                submissionItem(1, "tourist", 800, false),
                submissionItem(2, "tourist", 800, true),
                submissionItem(3, "tourist", 1200, true),
                submissionItem(4, "tourist", null, false)
        );
    }

    @Test
    void returnsProblemSubmissionDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-02T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-02T23:59:59");
        CodeforcesProblemSubmissionCriteria query = new CodeforcesProblemSubmissionCriteria("1000:A", from, to);
        CodeforcesSubmissionRepository repository = new CodeforcesSubmissionRepository() {
            @Override
            public List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(
                        submission(1, "alice", 800, false),
                        submission(2, "alice", 800, true),
                        submission(3, "bob", 800, false)
                );
            }
        };
        CodeforcesSubmissionQueryService service = new CodeforcesSubmissionQueryService(repository);

        CodeforcesProblemSubmissionReport report = service.listProblemSubmissions(query);

        assertThat(report.problemKey()).isEqualTo("1000:A");
        assertThat(report.submissions()).containsExactly(
                submissionItem(1, "alice", 800, false),
                submissionItem(2, "alice", 800, true),
                submissionItem(3, "bob", 800, false)
        );
    }

    @Test
    void returnsHandleFirstAcceptedProblemDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-03T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-03T23:59:59");
        CodeforcesHandleFirstAcceptedProblemCriteria query = new CodeforcesHandleFirstAcceptedProblemCriteria(
                "tourist",
                from,
                to,
                null,
                null
        );
        CodeforcesFirstAcceptedProblemRepository repository = new CodeforcesFirstAcceptedProblemRepository() {
            @Override
            public List<CodeforcesFirstAcceptedProblem> findHandleFirstAcceptedProblems(
                    CodeforcesHandleFirstAcceptedProblemCriteria actualQuery
            ) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(
                        firstAccepted("tourist", "1000:A", 800, "2026-07-03T09:00:00"),
                        firstAccepted("tourist", "1000:B", 800, "2026-07-03T10:00:00"),
                        firstAccepted("tourist", "1000:C", 1200, "2026-07-03T11:00:00"),
                        firstAccepted("tourist", "1000:D", null, "2026-07-03T12:00:00")
                );
            }

            @Override
            public List<CodeforcesFirstAcceptedProblem> findProblemFirstAcceptedHandles(
                    CodeforcesProblemFirstAcceptedHandleCriteria query
            ) {
                throw new UnsupportedOperationException("not used");
            }
        };
        CodeforcesFirstAcceptedProblemQueryService service =
                new CodeforcesFirstAcceptedProblemQueryService(repository);

        var report = service.summarizeHandleFirstAcceptedProblems(query);

        assertThat(report.authorHandle()).isEqualTo("tourist");
        assertThat(report.totalAcceptedProblemCount()).isEqualTo(4);
        assertThat(report.problems()).containsExactly(
                firstAcceptedProblem("1000:A", 800, "2026-07-03T09:00:00"),
                firstAcceptedProblem("1000:B", 800, "2026-07-03T10:00:00"),
                firstAcceptedProblem("1000:C", 1200, "2026-07-03T11:00:00"),
                firstAcceptedProblem("1000:D", null, "2026-07-03T12:00:00")
        );
    }

    @Test
    void summarizesProblemFirstAcceptedHandles() {
        LocalDateTime from = LocalDateTime.parse("2026-07-04T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-04T23:59:59");
        CodeforcesProblemFirstAcceptedHandleCriteria query =
                new CodeforcesProblemFirstAcceptedHandleCriteria("1000:A", from, to);
        CodeforcesFirstAcceptedProblemRepository repository = new CodeforcesFirstAcceptedProblemRepository() {
            @Override
            public List<CodeforcesFirstAcceptedProblem> findHandleFirstAcceptedProblems(
                    CodeforcesHandleFirstAcceptedProblemCriteria query
            ) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<CodeforcesFirstAcceptedProblem> findProblemFirstAcceptedHandles(
                    CodeforcesProblemFirstAcceptedHandleCriteria actualQuery
            ) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(
                        firstAccepted("bob", "1000:A", 800, "2026-07-04T10:00:00"),
                        firstAccepted("alice", "1000:A", 800, "2026-07-04T09:00:00")
                );
            }
        };
        CodeforcesFirstAcceptedProblemQueryService service =
                new CodeforcesFirstAcceptedProblemQueryService(repository);

        CodeforcesProblemFirstAcceptedHandleReport report = service.summarizeProblemFirstAcceptedHandles(query);

        assertThat(report.problemKey()).isEqualTo("1000:A");
        assertThat(report.acceptedHandleCount()).isEqualTo(2);
        assertThat(report.acceptedHandles()).containsExactly(
                firstAcceptedHandle("alice", "2026-07-04T09:00:00"),
                firstAcceptedHandle("bob", "2026-07-04T10:00:00")
        );
    }

    private static CodeforcesSubmission submission(
            long codeforcesSubmissionId,
            String authorHandle,
            Integer problemRating,
            boolean accepted
    ) {
        return new CodeforcesSubmission(
                codeforcesSubmissionId,
                authorHandle,
                1000L,
                LocalDateTime.parse("2026-07-01T12:00:00"),
                LocalDate.parse("2026-07-01"),
                0,
                "1000:A",
                1000L,
                "A",
                "A problem",
                "PROGRAMMING",
                BigDecimal.ZERO,
                problemRating,
                "[]",
                "PRACTICE",
                "Java 21",
                accepted ? "OK" : "WRONG_ANSWER",
                accepted,
                "TESTS",
                1,
                100,
                1024L
        );
    }

    private static CodeforcesFirstAcceptedProblem firstAccepted(
            String authorHandle,
            String problemKey,
            Integer problemRating,
            String firstAcceptedAtUtcPlus8
    ) {
        String[] problemParts = problemKey.split(":");
        return new CodeforcesFirstAcceptedProblem(
                authorHandle,
                problemKey,
                Long.parseLong(problemParts[0]),
                problemParts[1],
                "Problem " + problemParts[1],
                "PROGRAMMING",
                BigDecimal.ZERO,
                problemRating,
                "[]",
                1L,
                LocalDateTime.parse(firstAcceptedAtUtcPlus8),
                LocalDateTime.parse(firstAcceptedAtUtcPlus8).toLocalDate(),
                "Java 21"
        );
    }

    private static CodeforcesSubmissionItem submissionItem(
            long codeforcesSubmissionId,
            String authorHandle,
            Integer problemRating,
            boolean accepted
    ) {
        return new CodeforcesSubmissionItem(
                codeforcesSubmissionId,
                authorHandle,
                1000L,
                LocalDateTime.parse("2026-07-01T12:00:00"),
                LocalDate.parse("2026-07-01"),
                0,
                "1000:A",
                1000L,
                "A",
                "A problem",
                "PROGRAMMING",
                BigDecimal.ZERO,
                problemRating,
                "[]",
                "PRACTICE",
                "Java 21",
                accepted ? "OK" : "WRONG_ANSWER",
                accepted,
                "TESTS",
                1,
                100,
                1024L
        );
    }

    private static CodeforcesHandleFirstAcceptedProblemReport.CodeforcesFirstAcceptedProblemItem firstAcceptedProblem(
            String problemKey,
            Integer problemRating,
            String firstAcceptedAtUtcPlus8
    ) {
        String[] problemParts = problemKey.split(":");
        return new CodeforcesHandleFirstAcceptedProblemReport.CodeforcesFirstAcceptedProblemItem(
                problemKey,
                Long.parseLong(problemParts[0]),
                problemParts[1],
                "Problem " + problemParts[1],
                "PROGRAMMING",
                BigDecimal.ZERO,
                problemRating,
                "[]",
                1L,
                LocalDateTime.parse(firstAcceptedAtUtcPlus8),
                LocalDateTime.parse(firstAcceptedAtUtcPlus8).toLocalDate(),
                "Java 21"
        );
    }

    private static CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle firstAcceptedHandle(
            String authorHandle,
            String firstAcceptedAtUtcPlus8
    ) {
        return new CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle(
                authorHandle,
                LocalDateTime.parse(firstAcceptedAtUtcPlus8)
        );
    }
}
