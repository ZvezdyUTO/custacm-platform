package com.custacm.platform.trainingdata.common.app.query;

import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountException;
import com.custacm.platform.trainingdata.common.app.account.OjHandleAccountService;
import com.custacm.platform.trainingdata.common.app.query.result.OjAcceptedSummaryReport;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjDailyRatingAcceptedSummary;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleAccount;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjDifficultyBucketPolicies;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjAcceptedSummaryQueryServiceTest {
    @Test
    void summarizesDwsRowsByRatingInAppLayer() {
        String studentIdentity = "112487张三";
        LocalDate from = LocalDate.parse("2026-07-01");
        LocalDate to = LocalDate.parse("2026-07-03");
        OjAcceptedSummaryCriteria expectedRepositoryQuery = new OjAcceptedSummaryCriteria(
                "tourist",
                from,
                to,
                null,
                null
        );
        OjAcceptedSummaryRepository repository = actualQuery -> {
            assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
            return List.of(
                    row("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)),
                    row("tourist", "2026-07-02", Map.of("800", 3, "UNRATED", 4))
            );
        };
        OjAcceptedSummaryQueryService service = new OjAcceptedSummaryQueryService(
                repository,
                handleAccountService(account(studentIdentity, "tourist", true)),
                OjDifficultyBucketPolicies.defaults()
        );

        OjAcceptedSummaryReport report = service.summarizeStudentAcceptedProblems(
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
    void resolvesRequestedOjHandleFromAccountMap() {
        String studentIdentity = "112487张三";
        OjAcceptedSummaryCriteria expectedRepositoryQuery = new OjAcceptedSummaryCriteria(
                OjNames.ATCODER,
                "tourist_atcoder",
                null,
                null,
                null,
                null
        );
        OjAcceptedSummaryRepository repository = actualQuery -> {
            assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
            return List.of(row("tourist_atcoder", "2026-07-01", Map.of("800", 2)));
        };
        OjAcceptedSummaryQueryService service = new OjAcceptedSummaryQueryService(
                repository,
                handleAccountService(account(
                        studentIdentity,
                        Map.of(OjNames.CODEFORCES, "tourist", OjNames.ATCODER, "tourist_atcoder"),
                        true
                )),
                OjDifficultyBucketPolicies.defaults()
        );

        OjAcceptedSummaryReport report = service.summarizeStudentAcceptedProblems(
                OjNames.ATCODER,
                studentIdentity,
                null,
                null,
                null,
                null
        );

        assertThat(report.studentIdentity()).isEqualTo(studentIdentity);
        assertThat(report.authorHandle()).isEqualTo("tourist_atcoder");
        assertThat(report.totalAcceptedProblemCount()).isEqualTo(2);
        assertThat(report.ratingCounts()).containsExactly(rating("800", 2));
    }

    @Test
    void foldsUnknownDifficultyKeysIntoUnratedWhenRatingBoundsAreBlank() {
        String studentIdentity = "112487张三";
        OjAcceptedSummaryRepository repository = actualQuery -> List.of(
                row("tourist_atcoder", "2026-07-01", Map.of("2800-", 1, "UNRATED", 2))
        );
        OjAcceptedSummaryQueryService service = new OjAcceptedSummaryQueryService(
                repository,
                handleAccountService(account(
                        studentIdentity,
                        Map.of(OjNames.ATCODER, "tourist_atcoder"),
                        true
                )),
                OjDifficultyBucketPolicies.defaults()
        );

        OjAcceptedSummaryReport report = service.summarizeStudentAcceptedProblems(
                OjNames.ATCODER,
                studentIdentity,
                null,
                null,
                null,
                null
        );

        assertThat(report.totalAcceptedProblemCount()).isEqualTo(3);
        assertThat(report.ratingCounts()).containsExactly(rating("UNRATED", 3));
    }

    @Test
    void appliesProblemRatingBoundsToWideDwsRowsInAppLayer() {
        String studentIdentity = "112487张三";
        LocalDate from = LocalDate.parse("2026-07-01");
        LocalDate to = LocalDate.parse("2026-07-03");
        OjAcceptedSummaryCriteria expectedRepositoryQuery = new OjAcceptedSummaryCriteria(
                "tourist",
                from,
                to,
                1200,
                1600
        );
        OjAcceptedSummaryRepository repository = actualQuery -> {
            assertThat(actualQuery).isEqualTo(expectedRepositoryQuery);
            return List.of(
                    row("tourist", "2026-07-01", Map.of("800", 2, "1200", 1)),
                    row("tourist", "2026-07-02", Map.of("1600", 3, "UNRATED", 4))
            );
        };
        OjAcceptedSummaryQueryService service = new OjAcceptedSummaryQueryService(
                repository,
                handleAccountService(account(studentIdentity, "tourist", true)),
                OjDifficultyBucketPolicies.defaults()
        );

        OjAcceptedSummaryReport report = service.summarizeStudentAcceptedProblems(
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
    void rejectsUnboundStudentIdentity() {
        OjAcceptedSummaryRepository repository = query -> {
            throw new UnsupportedOperationException("not used");
        };
        OjAcceptedSummaryQueryService service = new OjAcceptedSummaryQueryService(
                repository,
                handleAccountService(),
                OjDifficultyBucketPolicies.defaults()
        );

        assertThatThrownBy(() -> service.summarizeStudentAcceptedProblems("missing", null, null, null, null))
                .isInstanceOfSatisfying(OjHandleAccountException.class, ex ->
                        assertThat(ex.errorCode()).isEqualTo(
                                OjHandleAccountException.ErrorCode.OJ_HANDLE_ACCOUNT_NOT_FOUND
                        ));
    }

    private static OjDailyRatingAcceptedSummary row(
            String authorHandle,
            String acceptedDateUtcPlus8,
            Map<String, Integer> acceptedProblemCountsByRating
    ) {
        return new OjDailyRatingAcceptedSummary(
                authorHandle,
                LocalDate.parse(acceptedDateUtcPlus8),
                acceptedProblemCountsByRating
        );
    }

    private static OjAcceptedSummaryReport.OjRatingAcceptedCount rating(
            String problemRating,
            int acceptedProblemCount
    ) {
        return new OjAcceptedSummaryReport.OjRatingAcceptedCount(
                problemRating,
                acceptedProblemCount
        );
    }

    private static OjHandleAccountService handleAccountService(OjHandleAccount... accounts) {
        InMemoryOjHandleAccountRepository repository = new InMemoryOjHandleAccountRepository();
        for (OjHandleAccount account : accounts) {
            repository.save(account);
        }
        return new OjHandleAccountService(repository, Clock.fixed(Instant.EPOCH, ZoneOffset.ofHours(8)));
    }

    private static OjHandleAccount account(String studentIdentity, String handle, boolean needCollect) {
        return account(studentIdentity, Map.of(OjNames.CODEFORCES, handle), needCollect);
    }

    private static OjHandleAccount account(
            String studentIdentity,
            Map<String, String> handles,
            boolean needCollect
    ) {
        return new OjHandleAccount(
                studentIdentity,
                handles,
                needCollect,
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
