package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesFirstAcceptedProblem;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesFirstAcceptedProblemRepository;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleFirstAcceptedProblemCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemFirstAcceptedHandleCriteria;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcCodeforcesFirstAcceptedProblemRepository implements CodeforcesFirstAcceptedProblemRepository {
    private static final String TABLE_NAME = "dwm_codeforces__handle_problem_first_accepted";
    private static final RowMapper<CodeforcesFirstAcceptedProblem> ROW_MAPPER =
            (rs, rowNum) -> new CodeforcesFirstAcceptedProblem(
                    rs.getString("author_handle"),
                    rs.getString("problem_key"),
                    rs.getLong("problem_contest_id"),
                    rs.getString("problem_index"),
                    rs.getString("problem_name"),
                    rs.getString("problem_type"),
                    rs.getBigDecimal("problem_points"),
                    nullableInteger(rs, "problem_rating"),
                    rs.getString("problem_tags_json"),
                    rs.getLong("first_accepted_submission_id"),
                    rs.getTimestamp("first_accepted_at_utc_plus8").toLocalDateTime(),
                    rs.getDate("first_accepted_date_utc_plus8").toLocalDate(),
                    rs.getString("first_accepted_language")
            );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCodeforcesFirstAcceptedProblemRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CodeforcesFirstAcceptedProblem> findHandleFirstAcceptedProblems(
            CodeforcesHandleFirstAcceptedProblemCriteria query
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("authorHandle", query.authorHandle());
        List<String> predicates = new ArrayList<>();
        predicates.add("author_handle = :authorHandle");
        addFirstAcceptedTimePredicates(
                query.firstAcceptedFromUtcPlus8(),
                query.firstAcceptedToUtcPlus8(),
                predicates,
                params
        );
        addProblemRatingPredicates(query.minProblemRating(), query.maxProblemRating(), predicates, params);
        return query(predicates, params);
    }

    @Override
    public List<CodeforcesFirstAcceptedProblem> findProblemFirstAcceptedHandles(
            CodeforcesProblemFirstAcceptedHandleCriteria query
    ) {
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
        return query(predicates, params);
    }

    private List<CodeforcesFirstAcceptedProblem> query(
            List<String> predicates,
            MapSqlParameterSource params
    ) {
        String sql = """
                select
                    author_handle,
                    problem_key,
                    problem_contest_id,
                    problem_index,
                    problem_name,
                    problem_type,
                    problem_points,
                    problem_rating,
                    problem_tags_json,
                    first_accepted_submission_id,
                    first_accepted_at_utc_plus8,
                    first_accepted_date_utc_plus8,
                    first_accepted_language
                from %s
                where %s
                order by first_accepted_at_utc_plus8 asc, author_handle asc, problem_key asc
                """.formatted(TABLE_NAME, String.join(" and ", predicates));

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
            params.addValue("firstAcceptedFromUtcPlus8", Timestamp.valueOf(firstAcceptedFromUtcPlus8));
        }
        if (firstAcceptedToUtcPlus8 != null) {
            predicates.add("first_accepted_at_utc_plus8 <= :firstAcceptedToUtcPlus8");
            params.addValue("firstAcceptedToUtcPlus8", Timestamp.valueOf(firstAcceptedToUtcPlus8));
        }
    }

    private static void addProblemRatingPredicates(
            Integer minProblemRating,
            Integer maxProblemRating,
            List<String> predicates,
            MapSqlParameterSource params
    ) {
        if (minProblemRating != null) {
            predicates.add("problem_rating >= :minProblemRating");
            params.addValue("minProblemRating", minProblemRating);
        }
        if (maxProblemRating != null) {
            predicates.add("problem_rating <= :maxProblemRating");
            params.addValue("maxProblemRating", maxProblemRating);
        }
    }

    private static Integer nullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
