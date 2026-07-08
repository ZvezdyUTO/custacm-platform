package com.custacm.platform.trainingdata.common.app.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleFirstAcceptedProblemReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjHandleSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemFirstAcceptedHandleReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjProblemSubmissionReport;
import com.custacm.platform.trainingdata.common.app.query.result.OjSubmissionItem;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjFirstAcceptedProblem;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjSubmission;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjSubmissionRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjWarehouseQueryServiceTest {
    @Test
    void returnsStudentSubmissionDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-01T23:59:59");
        String studentIdentity = "112487张三";
        OjHandleSubmissionCriteria expectedRepositoryQuery = new OjHandleSubmissionCriteria(
                "tourist",
                from,
                to,
                null,
                null,
                2,
                2
        );
        OjSubmissionRepository repository = new OjSubmissionRepository() {
            @Override
            public long countHandleSubmissions(OjHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return 4;
            }

            @Override
            public List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return List.of(
                        submission(3, "tourist", 1200, true),
                        submission(4, "tourist", null, false)
                );
            }

            @Override
            public long countProblemSubmissions(OjProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }
        };
        OjHandleAccountService handleAccountService = handleAccountService(
                account(studentIdentity, "tourist")
        );
        OjSubmissionQueryService service = new OjSubmissionQueryService(
                repository,
                handleAccountService
        );

        OjHandleSubmissionReport report = service.listStudentSubmissions(
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
    void returnsStudentSubmissionDetailsForRequestedOjHandle() {
        String studentIdentity = "112487张三";
        OjHandleSubmissionCriteria expectedRepositoryQuery = new OjHandleSubmissionCriteria(
                OjNames.ATCODER,
                "tourist_atcoder",
                null,
                null,
                null,
                null,
                100,
                0
        );
        OjSubmissionRepository repository = new OjSubmissionRepository() {
            @Override
            public long countHandleSubmissions(OjHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return 1;
            }

            @Override
            public List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return List.of(submission(1, "tourist_atcoder", 800, true));
            }

            @Override
            public long countProblemSubmissions(OjProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }
        };
        OjSubmissionQueryService service = new OjSubmissionQueryService(
                repository,
                handleAccountService(account(
                        studentIdentity,
                        Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder")
                ))
        );

        OjHandleSubmissionReport report = service.listStudentSubmissions(
                OjNames.ATCODER,
                studentIdentity,
                null,
                null,
                null,
                null,
                1,
                100
        );

        assertThat(report.studentIdentity()).isEqualTo(studentIdentity);
        assertThat(report.authorHandle()).isEqualTo("tourist_atcoder");
        assertThat(report.submissions()).containsExactly(
                submissionItem(1, studentIdentity, "tourist_atcoder", 800, true)
        );
    }

    @Test
    void returnsProblemSubmissionDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-02T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-02T23:59:59");
        OjProblemSubmissionCriteria query = new OjProblemSubmissionCriteria("1000:A", from, to, 2, 0);
        OjSubmissionRepository repository = new OjSubmissionRepository() {
            @Override
            public long countHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemSubmissions(OjProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return 3;
            }

            @Override
            public List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(
                        submission(1, "alice", 800, false),
                        submission(2, "alice", 800, true)
                );
            }
        };
        OjHandleAccountService handleAccountService = handleAccountService(
                account("112487张三", "alice"),
                account("112488李四", "bob")
        );
        OjSubmissionQueryService service = new OjSubmissionQueryService(
                repository,
                handleAccountService
        );

        OjProblemSubmissionReport report = service.listProblemSubmissions(query);

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
    void mapsProblemSubmissionHandlesBackByRequestedOj() {
        OjProblemSubmissionCriteria query =
                new OjProblemSubmissionCriteria(OjNames.ATCODER, "abc100:A", null, null, 100, 0);
        OjSubmissionRepository repository = new OjSubmissionRepository() {
            @Override
            public long countHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemSubmissions(OjProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return 1;
            }

            @Override
            public List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(submission(1, "alice_atcoder", 800, true));
            }
        };
        OjSubmissionQueryService service = new OjSubmissionQueryService(
                repository,
                handleAccountService(account(
                        "112487张三",
                        Map.of(OjNames.CODEFORCES, "alice", OjNames.ATCODER, "alice_atcoder")
                ))
        );

        OjProblemSubmissionReport report = service.listProblemSubmissions(query);

        assertThat(report.submissions()).containsExactly(
                submissionItem(1, "112487张三", "alice_atcoder", 800, true)
        );
    }

    @Test
    void returnsEmptySubmissionPageWhenOffsetIsPastTotal() {
        LocalDateTime from = LocalDateTime.parse("2026-07-01T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-01T23:59:59");
        String studentIdentity = "112487张三";
        OjHandleSubmissionCriteria expectedRepositoryQuery = new OjHandleSubmissionCriteria(
                "tourist",
                from,
                to,
                null,
                null,
                1,
                1
        );
        OjSubmissionRepository repository = new OjSubmissionRepository() {
            @Override
            public long countHandleSubmissions(OjHandleSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return 1;
            }

            @Override
            public List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new AssertionError("page rows should not be queried when offset is past total");
            }

            @Override
            public long countProblemSubmissions(OjProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }
        };
        OjSubmissionQueryService service = new OjSubmissionQueryService(
                repository,
                handleAccountService(account(studentIdentity, "tourist"))
        );

        OjHandleSubmissionReport report = service.listStudentSubmissions(
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
        OjSubmissionRepository repository = new OjSubmissionRepository() {
            @Override
            public long countHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemSubmissions(OjProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }
        };
        OjSubmissionQueryService service = new OjSubmissionQueryService(
                repository,
                handleAccountService()
        );

        assertThatThrownBy(() -> service.listStudentSubmissions("missing", null, null, null, null, 1, 100))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND
                        ));
    }

    @Test
    void rejectsUnboundProblemSubmissionHandle() {
        OjProblemSubmissionCriteria query = new OjProblemSubmissionCriteria("1000:A", null, null, 100, 0);
        OjSubmissionRepository repository = new OjSubmissionRepository() {
            @Override
            public long countHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemSubmissions(OjProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return 1;
            }

            @Override
            public List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(submission(1, "unbound", 800, true));
            }
        };
        OjSubmissionQueryService service = new OjSubmissionQueryService(
                repository,
                handleAccountService()
        );

        assertThatThrownBy(() -> service.listProblemSubmissions(query))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND
                        ));
    }

    @Test
    void returnsStudentFirstAcceptedProblemDetails() {
        LocalDateTime from = LocalDateTime.parse("2026-07-03T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-03T23:59:59");
        String studentIdentity = "112487张三";
        OjHandleFirstAcceptedProblemCriteria expectedRepositoryQuery =
                new OjHandleFirstAcceptedProblemCriteria(
                        "tourist",
                        from,
                        to,
                        null,
                        null,
                        2,
                        2
                );
        OjFirstAcceptedProblemRepository repository = new OjFirstAcceptedProblemRepository() {
            @Override
            public long countHandleFirstAcceptedProblems(OjHandleFirstAcceptedProblemCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return 4;
            }

            @Override
            public List<OjFirstAcceptedProblem> findHandleFirstAcceptedProblems(
                    OjHandleFirstAcceptedProblemCriteria actualQuery
            ) {
                assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
                return List.of(
                        firstAccepted("tourist", "1000:C", 1200, "2026-07-03T11:00:00"),
                        firstAccepted("tourist", "1000:D", null, "2026-07-03T12:00:00")
                );
            }

            @Override
            public long countProblemFirstAcceptedHandles(OjProblemFirstAcceptedHandleCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjFirstAcceptedProblem> findProblemFirstAcceptedHandles(
                    OjProblemFirstAcceptedHandleCriteria query
            ) {
                throw new UnsupportedOperationException("not used");
            }
        };
        OjHandleAccountService handleAccountService = handleAccountService(
                account(studentIdentity, "tourist")
        );
        OjFirstAcceptedProblemQueryService service =
                new OjFirstAcceptedProblemQueryService(repository, handleAccountService);

        var report = service.summarizeStudentFirstAcceptedProblems(
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
        assertThat(report.totalAcceptedProblemCount()).isEqualTo(4);
        assertThat(report.page()).isEqualTo(2);
        assertThat(report.limit()).isEqualTo(2);
        assertThat(report.total()).isEqualTo(4);
        assertThat(report.totalPages()).isEqualTo(2);
        assertThat(report.hasMore()).isFalse();
        assertThat(report.problems()).containsExactly(
                firstAcceptedProblem("1000:C", 1200, "2026-07-03T11:00:00"),
                firstAcceptedProblem("1000:D", null, "2026-07-03T12:00:00")
        );
    }

    @Test
    void summarizesProblemFirstAcceptedHandles() {
        LocalDateTime from = LocalDateTime.parse("2026-07-04T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2026-07-04T23:59:59");
        OjProblemFirstAcceptedHandleCriteria query =
                new OjProblemFirstAcceptedHandleCriteria("1000:A", from, to);
        OjFirstAcceptedProblemRepository repository = new OjFirstAcceptedProblemRepository() {
            @Override
            public long countHandleFirstAcceptedProblems(OjHandleFirstAcceptedProblemCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjFirstAcceptedProblem> findHandleFirstAcceptedProblems(
                    OjHandleFirstAcceptedProblemCriteria query
            ) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemFirstAcceptedHandles(OjProblemFirstAcceptedHandleCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return 2;
            }

            @Override
            public List<OjFirstAcceptedProblem> findProblemFirstAcceptedHandles(
                    OjProblemFirstAcceptedHandleCriteria actualQuery
            ) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(
                        firstAccepted("bob", "1000:A", 800, "2026-07-04T10:00:00"),
                        firstAccepted("alice", "1000:A", 800, "2026-07-04T09:00:00")
                );
            }
        };
        OjHandleAccountService handleAccountService = handleAccountService(
                account("112488李四", "bob"),
                account("112487张三", "alice")
        );
        OjFirstAcceptedProblemQueryService service =
                new OjFirstAcceptedProblemQueryService(repository, handleAccountService);

        OjProblemFirstAcceptedHandleReport report = service.summarizeProblemFirstAcceptedHandles(query);

        assertThat(report.problemKey()).isEqualTo("1000:A");
        assertThat(report.acceptedHandleCount()).isEqualTo(2);
        assertThat(report.page()).isEqualTo(1);
        assertThat(report.limit()).isEqualTo(Integer.MAX_VALUE);
        assertThat(report.total()).isEqualTo(2);
        assertThat(report.totalPages()).isEqualTo(1);
        assertThat(report.hasMore()).isFalse();
        assertThat(report.acceptedHandles()).containsExactly(
                firstAcceptedHandle("112488李四", "bob", "2026-07-04T10:00:00"),
                firstAcceptedHandle("112487张三", "alice", "2026-07-04T09:00:00")
        );
    }

    @Test
    void rejectsUnboundProblemFirstAcceptedHandle() {
        OjProblemFirstAcceptedHandleCriteria query =
                new OjProblemFirstAcceptedHandleCriteria("1000:A", null, null);
        OjFirstAcceptedProblemRepository repository = new OjFirstAcceptedProblemRepository() {
            @Override
            public long countHandleFirstAcceptedProblems(OjHandleFirstAcceptedProblemCriteria query) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public List<OjFirstAcceptedProblem> findHandleFirstAcceptedProblems(
                    OjHandleFirstAcceptedProblemCriteria query
            ) {
                throw new UnsupportedOperationException("not used");
            }

            @Override
            public long countProblemFirstAcceptedHandles(OjProblemFirstAcceptedHandleCriteria actualQuery) {
                assertThat(actualQuery).isEqualTo(query);
                return 1;
            }

            @Override
            public List<OjFirstAcceptedProblem> findProblemFirstAcceptedHandles(
                    OjProblemFirstAcceptedHandleCriteria actualQuery
            ) {
                assertThat(actualQuery).isEqualTo(query);
                return List.of(firstAccepted("unbound", "1000:A", 800, "2026-07-04T10:00:00"));
            }
        };
        OjFirstAcceptedProblemQueryService service =
                new OjFirstAcceptedProblemQueryService(repository, handleAccountService());

        assertThatThrownBy(() -> service.summarizeProblemFirstAcceptedHandles(query))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND
                        ));
    }

    private static OjSubmission submission(
            long codeforcesSubmissionId,
            String authorHandle,
            Integer problemRating,
            boolean accepted
    ) {
        return new OjSubmission(
                String.valueOf(codeforcesSubmissionId),
                authorHandle,
                LocalDateTime.parse("2026-07-01T12:00:00"),
                LocalDate.parse("2026-07-01"),
                "1000:A",
                "A",
                "A problem",
                problemRating == null ? null : String.valueOf(problemRating),
                "Java 21",
                accepted ? "OK" : "WRONG_ANSWER",
                accepted,
                100,
                "https://codeforces.com/contest/1000/submission/" + codeforcesSubmissionId
        );
    }

    private static OjFirstAcceptedProblem firstAccepted(
            String authorHandle,
            String problemKey,
            Integer problemRating,
            String firstAcceptedAtUtcPlus8
    ) {
        String[] problemParts = problemKey.split(":");
        return new OjFirstAcceptedProblem(
                authorHandle,
                problemKey,
                problemParts[1],
                "Problem " + problemParts[1],
                problemRating == null ? null : String.valueOf(problemRating),
                "1",
                LocalDateTime.parse(firstAcceptedAtUtcPlus8),
                LocalDateTime.parse(firstAcceptedAtUtcPlus8).toLocalDate(),
                "Java 21",
                "https://codeforces.com/contest/" + problemParts[0] + "/submission/1"
        );
    }

    private static OjSubmissionItem submissionItem(
            long codeforcesSubmissionId,
            String studentIdentity,
            String authorHandle,
            Integer problemRating,
            boolean accepted
    ) {
        return new OjSubmissionItem(
                String.valueOf(codeforcesSubmissionId),
                studentIdentity,
                authorHandle,
                LocalDateTime.parse("2026-07-01T12:00:00"),
                LocalDate.parse("2026-07-01"),
                "1000:A",
                "A",
                "A problem",
                problemRating == null ? null : String.valueOf(problemRating),
                "Java 21",
                accepted ? "OK" : "WRONG_ANSWER",
                accepted,
                100,
                "https://codeforces.com/contest/1000/submission/" + codeforcesSubmissionId
        );
    }

    private static OjHandleFirstAcceptedProblemReport.OjFirstAcceptedProblemItem firstAcceptedProblem(
            String problemKey,
            Integer problemRating,
            String firstAcceptedAtUtcPlus8
    ) {
        String[] problemParts = problemKey.split(":");
        return new OjHandleFirstAcceptedProblemReport.OjFirstAcceptedProblemItem(
                problemKey,
                problemParts[1],
                "Problem " + problemParts[1],
                problemRating == null ? null : String.valueOf(problemRating),
                "1",
                LocalDateTime.parse(firstAcceptedAtUtcPlus8),
                LocalDateTime.parse(firstAcceptedAtUtcPlus8).toLocalDate(),
                "Java 21",
                "https://codeforces.com/contest/" + problemParts[0] + "/submission/1"
        );
    }

    private static OjProblemFirstAcceptedHandleReport.OjFirstAcceptedHandle firstAcceptedHandle(
            String studentIdentity,
            String authorHandle,
            String firstAcceptedAtUtcPlus8
    ) {
        return new OjProblemFirstAcceptedHandleReport.OjFirstAcceptedHandle(
                studentIdentity,
                authorHandle,
                LocalDateTime.parse(firstAcceptedAtUtcPlus8)
        );
    }

    private static OjHandleAccountService handleAccountService(OjHandleAccount... accounts) {
        InMemoryOjHandleAccountRepository repository = new InMemoryOjHandleAccountRepository();
        for (OjHandleAccount account : accounts) {
            repository.save(account);
        }
        return new OjHandleAccountService(repository, Clock.fixed(Instant.EPOCH, ZoneOffset.ofHours(8)));
    }

    private static OjHandleAccount account(String studentIdentity, String handle) {
        return account(studentIdentity, Map.of(OjNames.CODEFORCES, handle));
    }

    private static OjHandleAccount account(String studentIdentity, Map<String, String> handles) {
        return new OjHandleAccount(
                studentIdentity,
                handles,
                true,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private static final class InMemoryOjHandleAccountRepository implements OjHandleAccountRepository {
        private final Map<String, OjHandleAccount> accountsByIdentity = new LinkedHashMap<>();

        @Override
        public List<OjHandleAccount> findAll() {
            return List.copyOf(accountsByIdentity.values());
        }

        @Override
        public java.util.Optional<OjHandleAccount> findByStudentIdentity(String studentIdentity) {
            return java.util.Optional.ofNullable(accountsByIdentity.get(studentIdentity));
        }

        @Override
        public java.util.Optional<OjHandleAccount> findByHandle(String ojName, String handle) {
            return accountsByIdentity.values().stream()
                    .filter(account -> handle.equals(account.handles().get(OjNames.normalize(ojName))))
                    .findFirst();
        }

        @Override
        public OjHandleAccount save(OjHandleAccount account) {
            accountsByIdentity.put(account.studentIdentity(), account);
            return account;
        }

        @Override
        public OjHandleAccount updateStudentIdentityAndNeedCollect(
                String oldStudentIdentity,
                String newStudentIdentity,
                Map<String, String> handles,
                boolean needCollect,
                Map<String, OjHandleCollectionState> collectionStates,
                Instant updatedAt
        ) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public OjHandleAccount updateCollectionStates(
                String studentIdentity,
                Map<String, OjHandleCollectionState> collectionStates,
                Instant updatedAt
        ) {
            throw new UnsupportedOperationException("not used");
        }
    }
}
