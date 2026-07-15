package com.custacm.platform.trainingdata.common.infra.oj.repo.query;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjRatingAcceptedSummary;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjDifficultyBucketPolicies;
import com.custacm.platform.trainingdata.common.infra.oj.repo.warehouse.OjWarehouseTableNames;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class JdbcOjAcceptedSummaryRepository implements OjAcceptedSummaryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OjDifficultyBucketPolicies bucketPolicies;

    public JdbcOjAcceptedSummaryRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            OjDifficultyBucketPolicies bucketPolicies
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.bucketPolicies = bucketPolicies;
    }

    @Override
    public List<OjRatingAcceptedSummary> summarizeAcceptedProblemsByRating(
            OjAcceptedSummaryCriteria query
    ) {
        return summarizeAcceptedProblemsByRating(List.of(query));
    }

    @Override
    public List<OjRatingAcceptedSummary> summarizeAcceptedProblemsByRating(
            List<OjAcceptedSummaryCriteria> queries
    ) {
        if (queries == null || queries.isEmpty()) {
            return List.of();
        }
        OjAcceptedSummaryCriteria query = queries.getFirst();
        validateSameFilters(queries, query);
        String tableName = OjWarehouseTableNames.dwsHandleDailyRatingAcceptedSummary(query.ojName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("handles", queries.stream()
                        .map(OjAcceptedSummaryCriteria::authorHandle)
                        .distinct()
                        .toList());
        List<String> predicates = new ArrayList<>();
        predicates.add("handle in (:handles)");

        if (query.acceptedFromDateUtcPlus8() != null) {
            predicates.add("accepted_date_utc_plus8 >= :acceptedFromDateUtcPlus8");
            params.addValue("acceptedFromDateUtcPlus8", Date.valueOf(query.acceptedFromDateUtcPlus8()));
        }
        if (query.acceptedToDateUtcPlus8() != null) {
            predicates.add("accepted_date_utc_plus8 <= :acceptedToDateUtcPlus8");
            params.addValue("acceptedToDateUtcPlus8", Date.valueOf(query.acceptedToDateUtcPlus8()));
        }
        addProblemRatingPredicates(query, predicates, params);

        String sql = """
                select
                    handle,
                    difficulty,
                    sum(accepted_problem_count) as accepted_problem_count
                from %s
                where %s
                group by handle, difficulty
                order by handle asc, difficulty asc
                """.formatted(tableName, String.join(" and ", predicates));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new OjRatingAcceptedSummary(
                rs.getString("handle"),
                rs.getString("difficulty"),
                Math.toIntExact(rs.getLong("accepted_problem_count"))
        ));
    }

    private static void validateSameFilters(
            List<OjAcceptedSummaryCriteria> queries,
            OjAcceptedSummaryCriteria expected
    ) {
        boolean mismatched = queries.stream().anyMatch(query ->
                !expected.ojName().equals(query.ojName())
                        || !java.util.Objects.equals(expected.acceptedFromDateUtcPlus8(), query.acceptedFromDateUtcPlus8())
                        || !java.util.Objects.equals(expected.acceptedToDateUtcPlus8(), query.acceptedToDateUtcPlus8())
                        || !java.util.Objects.equals(expected.minProblemRating(), query.minProblemRating())
                        || !java.util.Objects.equals(expected.maxProblemRating(), query.maxProblemRating()));
        if (mismatched) {
            throw new IllegalArgumentException("batch accepted-summary queries must use identical filters");
        }
    }

    private void addProblemRatingPredicates(
            OjAcceptedSummaryCriteria query,
            List<String> predicates,
            MapSqlParameterSource params
    ) {
        List<String> difficulties = bucketPolicies.policyFor(query.ojName())
                .bucketKeysInRange(query.minProblemRating(), query.maxProblemRating());
        if (difficulties == null) {
            return;
        }
        if (difficulties.isEmpty()) {
            predicates.add("1 = 0");
            return;
        }
        predicates.add("difficulty in (:difficulties)");
        params.addValue("difficulties", difficulties);
    }
}
