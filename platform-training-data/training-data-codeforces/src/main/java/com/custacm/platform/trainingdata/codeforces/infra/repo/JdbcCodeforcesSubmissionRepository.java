package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesSubmission;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesSubmissionRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcCodeforcesSubmissionRepository implements CodeforcesSubmissionRepository {
    private static final String TABLE_NAME = "dwd_codeforces__submission";
    private static final RowMapper<CodeforcesSubmission> ROW_MAPPER = (rs, rowNum) -> new CodeforcesSubmission(
            rs.getLong("codeforces_submission_id"),
            rs.getString("author_handle"),
            nullableLong(rs, "contest_id"),
            nullableDateTime(rs, "submitted_at_utc_plus8"),
            nullableDate(rs, "submitted_date_utc_plus8"),
            nullableInteger(rs, "relative_time_seconds"),
            rs.getString("problem_key"),
            nullableLong(rs, "problem_contest_id"),
            rs.getString("problem_index"),
            rs.getString("problem_name"),
            rs.getString("problem_type"),
            rs.getBigDecimal("problem_points"),
            nullableInteger(rs, "problem_rating"),
            rs.getString("problem_tags_json"),
            rs.getString("author_participant_type"),
            rs.getString("programming_language"),
            rs.getString("verdict"),
            rs.getBoolean("is_accepted"),
            rs.getString("testset"),
            nullableInteger(rs, "passed_test_count"),
            nullableInteger(rs, "time_consumed_millis"),
            nullableLong(rs, "memory_consumed_bytes")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCodeforcesSubmissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("authorHandle", query.authorHandle());
        List<String> predicates = new ArrayList<>();
        predicates.add("author_handle = :authorHandle");
        addSubmittedTimePredicates(query.submittedFromUtcPlus8(), query.submittedToUtcPlus8(), predicates, params);
        addProblemRatingPredicates(query.minProblemRating(), query.maxProblemRating(), predicates, params);
        return count(predicates, params);
    }

    @Override
    public List<CodeforcesSubmission> findHandleSubmissions(CodeforcesHandleSubmissionCriteria query) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("authorHandle", query.authorHandle());
        List<String> predicates = new ArrayList<>();
        predicates.add("author_handle = :authorHandle");
        addSubmittedTimePredicates(query.submittedFromUtcPlus8(), query.submittedToUtcPlus8(), predicates, params);
        addProblemRatingPredicates(query.minProblemRating(), query.maxProblemRating(), predicates, params);
        return query(predicates, params, query.limit(), query.offset());
    }

    @Override
    public long countProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("problemKey", query.problemKey());
        List<String> predicates = new ArrayList<>();
        predicates.add("problem_key = :problemKey");
        addSubmittedTimePredicates(query.submittedFromUtcPlus8(), query.submittedToUtcPlus8(), predicates, params);
        return count(predicates, params);
    }

    @Override
    public List<CodeforcesSubmission> findProblemSubmissions(CodeforcesProblemSubmissionCriteria query) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("problemKey", query.problemKey());
        List<String> predicates = new ArrayList<>();
        predicates.add("problem_key = :problemKey");
        addSubmittedTimePredicates(query.submittedFromUtcPlus8(), query.submittedToUtcPlus8(), predicates, params);
        return query(predicates, params, query.limit(), query.offset());
    }

    private long count(List<String> predicates, MapSqlParameterSource params) {
        String sql = """
                select count(*)
                from %s
                where %s
                """.formatted(TABLE_NAME, String.join(" and ", predicates));

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0 : count;
    }

    private List<CodeforcesSubmission> query(
            List<String> predicates,
            MapSqlParameterSource params,
            int limit,
            long offset
    ) {
        params.addValue("limit", limit);
        params.addValue("offset", offset);
        String sql = """
                select
                    codeforces_submission_id,
                    author_handle,
                    contest_id,
                    submitted_at_utc_plus8,
                    submitted_date_utc_plus8,
                    relative_time_seconds,
                    problem_key,
                    problem_contest_id,
                    problem_index,
                    problem_name,
                    problem_type,
                    problem_points,
                    problem_rating,
                    problem_tags_json,
                    author_participant_type,
                    programming_language,
                    verdict,
                    is_accepted,
                    testset,
                    passed_test_count,
                    time_consumed_millis,
                    memory_consumed_bytes
                from %s
                where %s
                order by submitted_at_utc_plus8 desc, codeforces_submission_id desc
                limit :limit offset :offset
                """.formatted(TABLE_NAME, String.join(" and ", predicates));

        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    private static void addSubmittedTimePredicates(
            LocalDateTime submittedFromUtcPlus8,
            LocalDateTime submittedToUtcPlus8,
            List<String> predicates,
            MapSqlParameterSource params
    ) {
        if (submittedFromUtcPlus8 != null) {
            predicates.add("submitted_at_utc_plus8 >= :submittedFromUtcPlus8");
            params.addValue("submittedFromUtcPlus8", submittedFromUtcPlus8);
        }
        if (submittedToUtcPlus8 != null) {
            predicates.add("submitted_at_utc_plus8 <= :submittedToUtcPlus8");
            params.addValue("submittedToUtcPlus8", submittedToUtcPlus8);
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

    private static LocalDateTime nullableDateTime(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, LocalDateTime.class);
    }

    private static LocalDate nullableDate(ResultSet rs, String columnName) throws SQLException {
        Date date = rs.getDate(columnName);
        return date == null ? null : date.toLocalDate();
    }

    private static Integer nullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private static Long nullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
