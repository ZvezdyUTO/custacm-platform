package com.custacm.platform.trainingdata.codeforces.app.query;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountService;
import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesHandleSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesProblemSubmissionReport;
import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesSubmissionItem;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesFirstAcceptedProblem;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesHandleAccountRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesSubmissionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeforcesWarehouseQueryServiceTest {
    @Test
    void returnsStudentSubmissionDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-01T23:59:59");
        String studentIdentity = "112487张三";
        CodeforcesHandleSubmissionCriteria expectedRepositoryQuery = new CodeforcesHandleSubmissionCriteria(
                "tourist",
                from,
                to,
                null,
                null,
                2,
                2
        );
        CodeforcesSubmissionRepository repository = new CodeforcesSubmissionRepository() {
            @Override
            public long countHandleSubmissions(CodeforcesHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return 4;
            }

            @Override
            public List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return List.of(
                        submission(3, "tourist", 1200, true),
                        submission(4, "tourist", null, false)
                );
            }

            @Override
            public long countProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }
        };
        CodeforcesHandleAccountService handleAccountService = handleAccountService(
                account(studentIdentity, "tourist")
        );
        CodeforcesSubmissionQueryService service = new CodeforcesSubmissionQueryService(
                repository,
                handleAccountService
        );

        CodeforcesHandleSubmissionReport report = service.listStudentSubmissions(
                studentIdentity,
                from,
                to,
                null,
                null,
                2,
                2
        );

        assertThat(report.studentIdentity()).isEqualTo(studentIdentity);
        assertThat(report.authorHandle()).isEqualTo("tourist");
        assertThat(report.page()).isEqualTo(2);
        assertThat(report.limit()).isEqualTo(2);
        assertThat(report.total()).isEqualTo(4);
        assertThat(report.totalPages()).isEqualTo(2);
        assertThat(report.hasMore()).isFalse();
        assertThat(report.submissions()).containsExactly(
                submissionItem(3, studentIdentity, "tourist", 1200, true),
                submissionItem(4, studentIdentity, "tourist", null, false)
        );
    }

    @Test
    void returnsProblemSubmissionDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-02T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-02T23:59:59");
        CodeforcesProblemSubmissionCriteria query = new CodeforcesProblemSubmissionCriteria("1000:A", from, to, 2, 0);
        CodeforcesSubmissionRepository repository = new CodeforcesSubmissionRepository() {
            @Override
            public long countHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemSubmissions(CodeforcesProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return 3;
            }

            @Override
            public List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(
                        submission(1, "alice", 800, false),
                        submission(2, "alice", 800, true)
                );
            }
        };
        CodeforcesHandleAccountService handleAccountService = handleAccountService(
                account("112487张三", "alice"),
                account("112488李四", "bob")
        );
        CodeforcesSubmissionQueryService service = new CodeforcesSubmissionQueryService(
                repository,
                handleAccountService
        );

        CodeforcesProblemSubmissionReport report = service.listProblemSubmissions(query);

        assertThat(report.problemKey()).isEqualTo("1000:A");
        assertThat(report.page()).isEqualTo(1);
        assertThat(report.limit()).isEqualTo(2);
        assertThat(report.total()).isEqualTo(3);
        assertThat(report.totalPages()).isEqualTo(2);
        assertThat(report.hasMore()).isTrue();
        assertThat(report.submissions()).containsExactly(
                submissionItem(1, "112487张三", "alice", 800, false),
                submissionItem(2, "112487张三", "alice", 800, true)
        );
    }

    @Test
    void returnsEmptySubmissionPageWhenOffsetIsPastTotal() {
        LocalDateTime from = LocalDateTime.parse("2026-07-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-01T23:59:59");
        String studentIdentity = "112487张三";
        CodeforcesHandleSubmissionCriteria expectedRepositoryQuery = new CodeforcesHandleSubmissionCriteria(
                "tourist",
                from,
                to,
                null,
                null,
                1,
                1
        );
        CodeforcesSubmissionRepository repository = new CodeforcesSubmissionRepository() {
            @Override
            public long countHandleSubmissions(CodeforcesHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return 1;
            }

            @Override
            public List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
                throw new AssertionError("page rows should not be queried when offset is past total");
            }

            @Override
            public long countProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }
        };
        CodeforcesSubmissionQueryService service = new CodeforcesSubmissionQueryService(
                repository,
                handleAccountService(account(studentIdentity, "tourist"))
        );

        CodeforcesHandleSubmissionReport report = service.listStudentSubmissions(
                studentIdentity,
                from,
                to,
                null,
                null,
                2,
                1
        );

        assertThat(report.page()).isEqualTo(2);
        assertThat(report.limit()).isEqualTo(1);
        assertThat(report.total()).isEqualTo(1);
        assertThat(report.totalPages()).isEqualTo(1);
        assertThat(report.hasMore()).isFalse();
        assertThat(report.submissions()).isEmpty();
    }

    @Test
    void rejectsUnboundStudentIdentityForSubmissions() {
        CodeforcesSubmissionRepository repository = new CodeforcesSubmissionRepository() {
            @Override
            public long countHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }
        };
        CodeforcesSubmissionQueryService service = new CodeforcesSubmissionQueryService(
                repository,
                handleAccountService()
        );

        assertThatThrownBy(() -> service.listStudentSubmissions("missing", null, null, null, null, 1, 100))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND
                        ));
    }

    @Test
    void rejectsUnboundProblemSubmissionHandle() {
        CodeforcesProblemSubmissionCriteria query = new CodeforcesProblemSubmissionCriteria("1000:A", null, null, 100, 0);
        CodeforcesSubmissionRepository repository = new CodeforcesSubmissionRepository() {
            @Override
            public long countHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemSubmissions(CodeforcesProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return 1;
            }

            @Override
            public List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(submission(1, "unbound", 800, true));
            }
        };
        CodeforcesSubmissionQueryService service = new CodeforcesSubmissionQueryService(
                repository,
                handleAccountService()
        );

        assertThatThrownBy(() -> service.listProblemSubmissions(query))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND
                        ));
    }

    @Test
    void returnsStudentFirstAcceptedProblemDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-03T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-03T23:59:59");
        String studentIdentity = "112487张三";
        CodeforcesHandleFirstAcceptedProblemCriteria expectedRepositoryQuery =
                new CodeforcesHandleFirstAcceptedProblemCriteria(
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
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
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
        CodeforcesHandleAccountService handleAccountService = handleAccountService(
                account(studentIdentity, "tourist")
        );
        CodeforcesFirstAcceptedProblemQueryService service =
                new CodeforcesFirstAcceptedProblemQueryService(repository, handleAccountService);

        var report = service.summarizeStudentFirstAcceptedProblems(
                studentIdentity,
                from,
                to,
                null,
                null
        );

        assertThat(report.studentIdentity()).isEqualTo(studentIdentity);
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
        CodeforcesHandleAccountService handleAccountService = handleAccountService(
                account("112488李四", "bob"),
                account("112487张三", "alice")
        );
        CodeforcesFirstAcceptedProblemQueryService service =
                new CodeforcesFirstAcceptedProblemQueryService(repository, handleAccountService);

        CodeforcesProblemFirstAcceptedHandleReport report = service.summarizeProblemFirstAcceptedHandles(query);

        assertThat(report.problemKey()).isEqualTo("1000:A");
        assertThat(report.acceptedHandleCount()).isEqualTo(2);
        assertThat(report.acceptedHandles()).containsExactly(
                firstAcceptedHandle("112487张三", "alice", "2026-07-04T09:00:00"),
                firstAcceptedHandle("112488李四", "bob", "2026-07-04T10:00:00")
        );
    }

    @Test
    void rejectsUnboundProblemFirstAcceptedHandle() {
        CodeforcesProblemFirstAcceptedHandleCriteria query =
                new CodeforcesProblemFirstAcceptedHandleCriteria("1000:A", null, null);
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
                return List.of(firstAccepted("unbound", "1000:A", 800, "2026-07-04T10:00:00"));
            }
        };
        CodeforcesFirstAcceptedProblemQueryService service =
                new CodeforcesFirstAcceptedProblemQueryService(repository, handleAccountService());

        assertThatThrownBy(() -> service.summarizeProblemFirstAcceptedHandles(query))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND
                        ));
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
            String studentIdentity,
            String authorHandle,
            Integer problemRating,
            boolean accepted
    ) {
        return new CodeforcesSubmissionItem(
                codeforcesSubmissionId,
                studentIdentity,
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
            String studentIdentity,
            String authorHandle,
            String firstAcceptedAtUtcPlus8
    ) {
        return new CodeforcesProblemFirstAcceptedHandleReport.CodeforcesFirstAcceptedHandle(
                studentIdentity,
                authorHandle,
                LocalDateTime.parse(firstAcceptedAtUtcPlus8)
        );
    }

    private static CodeforcesHandleAccountService handleAccountService(CodeforcesHandleAccount... accounts) {
        InMemoryCodeforcesHandleAccountRepository repository = new InMemoryCodeforcesHandleAccountRepository();
        for (CodeforcesHandleAccount account : accounts) {
            repository.save(account);
        }
        return new CodeforcesHandleAccountService(repository);
    }

    private static CodeforcesHandleAccount account(String studentIdentity, String handle) {
        return new CodeforcesHandleAccount(studentIdentity, handle, Instant.EPOCH, Instant.EPOCH);
    }

    private static final class InMemoryCodeforcesHandleAccountRepository implements CodeforcesHandleAccountRepository {
        private final Map<String, CodeforcesHandleAccount> accountsByIdentity = new LinkedHashMap<>();

        @Override
        public List<CodeforcesHandleAccount> findAll() {
            return List.copyOf(accountsByIdentity.values());
        }

        @Override
        public Optional<CodeforcesHandleAccount> findByStudentIdentity(String studentIdentity) {
            return Optional.ofNullable(accountsByIdentity.get(studentIdentity));
        }

        @Override
        public Optional<CodeforcesHandleAccount> findByHandle(String handle) {
            return accountsByIdentity.values().stream()
                    .filter(account -> account.handle().equals(handle))
                    .findFirst();
        }

        @Override
        public CodeforcesHandleAccount save(CodeforcesHandleAccount account) {
            accountsByIdentity.put(account.studentIdentity(), account);
            return account;
        }

        @Override
        public CodeforcesHandleAccount updateStudentIdentityAndNeedCollect(
                String oldStudentIdentity,
                String newStudentIdentity,
                boolean needCollect,
                Instant updatedAt
        ) {
            throw new UnsupportedOperationException("not used");
        }
    }
}
