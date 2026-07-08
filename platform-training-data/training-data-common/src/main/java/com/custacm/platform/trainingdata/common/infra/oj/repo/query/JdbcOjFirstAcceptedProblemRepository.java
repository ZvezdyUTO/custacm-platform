package com.custacm.platform.trainingdata.common.infra.oj.repo.query;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemFirstAcceptedHandleCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjFirstAcceptedProblem;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjProblemRatingBuckets;
import com.custacm.platform.trainingdata.common.infra.oj.repo.warehouse.OjWarehouseTableNames;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcOjFirstAcceptedProblemRepository implements OjFirstAcceptedProblemRepository {
    private static final RowMapper<OjFirstAcceptedProblem> ROW_MAPPER =
            (rs, rowNum) -> new OjFirstAcceptedProblem(
                    rs.getString("handle"),
                    rs.getString("problem_key"),
                    rs.getString("problem_index"),
                    rs.getString("problem_name"),
                    rs.getString("difficulty"),
                    rs.getString("first_accepted_submission_id"),
                    rs.getObject("first_accepted_at_utc_plus8", LocalDateTime.class),
                    rs.getDate("first_accepted_date_utc_plus8").toLocalDate(),
                    rs.getString("first_accepted_language"),
                    rs.getString("first_accepted_source_url")
            );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcOjFirstAcceptedProblemRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OjFirstAcceptedProblem> findHandleFirstAcceptedProblems(
            OjHandleFirstAcceptedProblemCriteria query
    ) {
        String tableName = OjWarehouseTableNames.dwmHandleProblemFirstAccepted(query.ojName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("handle", query.authorHandle());
        List<String> predicates = new ArrayList<>();
        predicates.add("handle = :handle");
        addFirstAcceptedTimePredicates(
                query.firstAcceptedFromUtcPlus8(),
                query.firstAcceptedToUtcPlus8(),
                predicates,
                params
        );
        addProblemRatingPredicates(query.minProblemRating(), query.maxProblemRating(), predicates, params);
        return query(tableName, predicates, params);
    }

    @Override
    public List<OjFirstAcceptedProblem> findProblemFirstAcceptedHandles(
            OjProblemFirstAcceptedHandleCriteria query
    ) {
        String tableName = OjWarehouseTableNames.dwmHandleProblemFirstAccepted(query.ojName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("problemKey", query.problemKey());
        List<String> predicates = new ArrayList<>();
        predicates.add("problem_key = :problemKey");
        addFirstAcceptedTimePredicates(
                query.firstAcceptedFromUtcPlus8(),
                query.firstAcceptedToUtcPlus8(),
                predicates,
                params
        );
        return query(tableName, predicates, params);
    }

    private List<OjFirstAcceptedProblem> query(
            String tableName,
            List<String> predicates,
            MapSqlParameterSource params
    ) {
        String sql = """
                select
                    handle,
                    problem_key,
                    problem_index,
                    problem_name,
                    difficulty,
                    first_accepted_submission_id,
                    first_accepted_at_utc_plus8,
                    first_accepted_date_utc_plus8,
                    first_accepted_language,
                    first_accepted_source_url
                from %s
                where %s
                order by first_accepted_at_utc_plus8 asc, handle asc, problem_key asc
                """.formatted(tableName, String.join(" and ", predicates));

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    private static void addFirstAcceptedTimePredicates(
            LocalDateTime firstAcceptedFromUtcPlus8,
            LocalDateTime firstAcceptedToUtcPlus8,
            List<String> predicates,
            MapSqlParameterSource params
    ) {
        if (firstAcceptedFromUtcPlus8 != null) {
            predicates.add("first_accepted_at_utc_plus8 >= :firstAcceptedFromUtcPlus8");
            params.addValue("firstAcceptedFromUtcPlus8", firstAcceptedFromUtcPlus8);
        }
        if (firstAcceptedToUtcPlus8 != null) {
            predicates.add("first_accepted_at_utc_plus8 <= :firstAcceptedToUtcPlus8");
            params.addValue("firstAcceptedToUtcPlus8", firstAcceptedToUtcPlus8);
        }
    }

    private static void addProblemRatingPredicates(
            Integer minProblemRating,
            Integer maxProblemRating,
            List<String> predicates,
            MapSqlParameterSource params
    ) {
        List<String> difficulties = difficultyValuesInRange(minProblemRating, maxProblemRating);
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

    private static List<String> difficultyValuesInRange(Integer minProblemRating, Integer maxProblemRating) {
        if (minProblemRating == null && maxProblemRating == null) {
            return null;
        }
        return OjProblemRatingBuckets.RATINGS.stream()
                .filter(rating -> minProblemRating == null || rating >= minProblemRating)
                .filter(rating -> maxProblemRating == null || rating <= maxProblemRating)
                .map(String::valueOf)
                .toList();
    }
}
