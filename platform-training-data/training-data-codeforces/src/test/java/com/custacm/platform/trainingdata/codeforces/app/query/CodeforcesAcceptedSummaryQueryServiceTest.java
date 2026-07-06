package com.custacm.platform.trainingdata.codeforces.app.query;

import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountException;
import com.custacm.platform.trainingdata.codeforces.app.account.CodeforcesHandleAccountService;
import com.custacm.platform.trainingdata.codeforces.app.query.result.CodeforcesAcceptedSummaryReport;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesDailyRatingAcceptedSummary;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesHandleAccount;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesHandleAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeforcesAcceptedSummaryQueryServiceTest {
    @Test
    void summarizesDwsRowsByRatingInAppLayer() {
        String studentIdentity = "112487张三";
        LocalDate from = LocalDate.parse("2026-07-01");
        LocalDate to = LocalDate.parse("2026-07-03");
        CodeforcesAcceptedSummaryCriteria expectedRepositoryQuery = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                from,
                to,
                null,
                null
        );
        CodeforcesAcceptedSummaryRepository repository = actualQuery -> {
            assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
            return List.of(
                    row("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)),
                    row("tourist", "2026-07-02", Map.of("800", 3, "UNRATED", 4))
            );
        };
        CodeforcesAcceptedSummaryQueryService service = new CodeforcesAcceptedSummaryQueryService(
                repository,
                handleAccountService(account(studentIdentity, "tourist"))
        );

        CodeforcesAcceptedSummaryReport report = service.summarizeStudentAcceptedProblems(
                studentIdentity,
                from,
                to,
                null,
                null
        );

        assertThat(report.studentIdentity()).isEqualTo(studentIdentity);
        assertThat(report.authorHandle()).isEqualTo("tourist");
        assertThat(report.totalAcceptedProblemCount()).isEqualTo(10);
        assertThat(report.ratingCounts()).containsExactly(
                rating("800", 5),
                rating("1200", 1),
                rating("UNRATED", 4)
        );
    }

    @Test
    void appliesProblemRatingBoundsToWideDwsRowsInAppLayer() {
        String studentIdentity = "112487张三";
        LocalDate from = LocalDate.parse("2026-07-01");
        LocalDate to = LocalDate.parse("2026-07-03");
        CodeforcesAcceptedSummaryCriteria expectedRepositoryQuery = new CodeforcesAcceptedSummaryCriteria(
                "tourist",
                from,
                to,
                1200,
                1600
        );
        CodeforcesAcceptedSummaryRepository repository = actualQuery -> {
            assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
            return List.of(
                    row("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)),
                    row("tourist", "2026-07-02", Map.of("1600", 3, "UNRATED", 4))
            );
        };
        CodeforcesAcceptedSummaryQueryService service = new CodeforcesAcceptedSummaryQueryService(
                repository,
                handleAccountService(account(studentIdentity, "tourist"))
        );

        CodeforcesAcceptedSummaryReport report = service.summarizeStudentAcceptedProblems(
                studentIdentity,
                from,
                to,
                1200,
                1600
        );

        assertThat(report.studentIdentity()).isEqualTo(studentIdentity);
        assertThat(report.authorHandle()).isEqualTo("tourist");
        assertThat(report.totalAcceptedProblemCount()).isEqualTo(4);
        assertThat(report.ratingCounts()).containsExactly(
                rating("1200", 1),
                rating("1600", 3)
        );
    }

    @Test
    void summarizesNeedCollectAccountsAndSortsByTotalDescending() {
        LocalDate from = LocalDate.parse("2026-07-01");
        LocalDate to = LocalDate.parse("2026-07-03");
        List<String> queriedHandles = new ArrayList<>();
        CodeforcesAcceptedSummaryRepository repository = actualQuery -> {
            assertThat(actualQuery.acceptedFromDateUtcPlus8()).isEqualTo(from);
            assertThat(actualQuery.acceptedToDateUtcPlus8()).isEqualTo(to);
            assertThat(actualQuery.minProblemRating()).isEqualTo(800);
            assertThat(actualQuery.maxProblemRating()).isEqualTo(1600);
            queriedHandles.add(actualQuery.authorHandle());
            return switch (actualQuery.authorHandle()) {
                case "tourist" -> List.of(row("tourist", "2026-07-01", Map.of("800", 1)));
                case "Benq" -> List.of(row("Benq", "2026-07-02", Map.of("1200", 3)));
                default -> throw new AssertionError("unexpected handle " + actualQuery.authorHandle());
            };
        };
        CodeforcesAcceptedSummaryQueryService service = new CodeforcesAcceptedSummaryQueryService(
                repository,
                handleAccountService(
                        account("112487张三", "tourist", true),
                        account("112488李四", "disabledHandle", false),
                        account("112489王五", "Benq", true)
                )
        );

        List<CodeforcesAcceptedSummaryReport> reports = service.summarizeAutoCollectAcceptedProblems(
                from,
                to,
                800,
                1600
        );

        assertThat(queriedHandles).containsExactly("tourist", "Benq");
        assertThat(reports)
                .extracting("studentIdentity", "authorHandle", "totalAcceptedProblemCount")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("112489王五", "Benq", 3),
                        org.assertj.core.groups.Tuple.tuple("112487张三", "tourist", 1)
                );
    }

    @Test
    void rejectsUnboundStudentIdentity() {
        CodeforcesAcceptedSummaryRepository repository = query -> {
            throw new UnsupportedOperationException("not used");
        };
        CodeforcesAcceptedSummaryQueryService service = new CodeforcesAcceptedSummaryQueryService(
                repository,
                handleAccountService()
        );

        assertThatThrownBy(() -> service.summarizeStudentAcceptedProblems("missing", null, null, null, null))
                .isInstanceOfSatisfying(CodeforcesHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                CodeforcesHandleAccountException.ErrorCode.CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND
                        ));
    }

    private static CodeforcesDailyRatingAcceptedSummary row(
            String authorHandle,
            String acceptedDateUtcPlus8,
            Map<String, Integer> acceptedProblemCountsByRating
    ) {
        return new CodeforcesDailyRatingAcceptedSummary(
                authorHandle,
                LocalDate.parse(acceptedDateUtcPlus8),
                acceptedProblemCountsByRating
        );
    }

    private static CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount rating(
            String problemRating,
            int acceptedProblemCount
    ) {
        return new CodeforcesAcceptedSummaryReport.CodeforcesRatingAcceptedCount(
                problemRating,
                acceptedProblemCount
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

    private static CodeforcesHandleAccount account(String studentIdentity, String handle, boolean needCollect) {
        return new CodeforcesHandleAccount(studentIdentity, handle, needCollect, Instant.EPOCH, Instant.EPOCH);
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
