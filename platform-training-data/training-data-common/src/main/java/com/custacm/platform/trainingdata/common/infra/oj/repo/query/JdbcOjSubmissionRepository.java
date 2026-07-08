package com.custacm.platform.trainingdata.common.infra.oj.repo.query;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjHandleSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjSubmission;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjSubmissionRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjProblemRatingBuckets;
import com.custacm.platform.trainingdata.common.infra.oj.repo.warehouse.OjWarehouseTableNames;
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

public class JdbcOjSubmissionRepository implements OjSubmissionRepository {
    private static final RowMapper<OjSubmission> ROW_MAPPER = (rs, rowNum) -> new OjSubmission(
            rs.getString("submission_id"),
            rs.getString("handle"),
            nullableDateTime(rs, "submitted_at_utc_plus8"),
            nullableDate(rs, "submitted_date_utc_plus8"),
            rs.getString("problem_key"),
            rs.getString("problem_index"),
            rs.getString("problem_name"),
            rs.getString("difficulty"),
            rs.getString("language"),
            rs.getString("verdict"),
            rs.getBoolean("is_accepted"),
            nullableInteger(rs, "time_consumed_millis"),
            rs.getString("source_url")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcOjSubmissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countHandleSubmissions(OjHandleSubmissionCriteria query) {
        String tableName = OjWarehouseTableNames.dwdSubmission(query.ojName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("handle", query.authorHandle());
        List<String> predicates = new ArrayList<>();
        predicates.add("handle = :handle");
        addSubmittedTimePredicates(query.submittedFromUtcPlus8(), query.submittedToUtcPlus8(), predicates, params);
        addProblemRatingPredicates(query.minProblemRating(), query.maxProblemRating(), predicates, params);
        return count(tableName, predicates, params);
    }

    @Override
    public List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria query) {
        String tableName = OjWarehouseTableNames.dwdSubmission(query.ojName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("handle", query.authorHandle());
        List<String> predicates = new ArrayList<>();
        predicates.add("handle = :handle");
        addSubmittedTimePredicates(query.submittedFromUtcPlus8(), query.submittedToUtcPlus8(), predicates, params);
        addProblemRatingPredicates(query.minProblemRating(), query.maxProblemRating(), predicates, params);
        return query(tableName, predicates, params, query.limit(), query.offset());
    }

    @Override
    public long countProblemSubmissions(OjProblemSubmissionCriteria query) {
        String tableName = OjWarehouseTableNames.dwdSubmission(query.ojName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("problemKey", query.problemKey());
        List<String> predicates = new ArrayList<>();
        predicates.add("problem_key = :problemKey");
        addSubmittedTimePredicates(query.submittedFromUtcPlus8(), query.submittedToUtcPlus8(), predicates, params);
        return count(tableName, predicates, params);
    }

    @Override
    public List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria query) {
        String tableName = OjWarehouseTableNames.dwdSubmission(query.ojName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("problemKey", query.problemKey());
        List<String> predicates = new ArrayList<>();
        predicates.add("problem_key = :problemKey");
        addSubmittedTimePredicates(query.submittedFromUtcPlus8(), query.submittedToUtcPlus8(), predicates, params);
        return query(tableName, predicates, params, query.limit(), query.offset());
    }

    private long count(String tableName, List<String> predicates, MapSqlParameterSource params) {
        String sql = """
                select count(*)
                from %s
                where %s
                """.formatted(tableName, String.join(" and ", predicates));

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0 : count;
    }

    private List<OjSubmission> query(
            String tableName,
            List<String> predicates,
            MapSqlParameterSource params,
            int limit,
            long offset
    ) {
        params.addValue("limit", limit);
        params.addValue("offset", offset);
        String sql = """
                select
                    submission_id,
                    handle,
                    submitted_at_utc_plus8,
                    submitted_date_utc_plus8,
                    problem_key,
                    problem_index,
                    problem_name,
                    difficulty,
                    language,
                    verdict,
                    is_accepted,
                    time_consumed_millis,
                    source_url
                from %s
                where %s
                order by submitted_at_utc_plus8 desc, length(submission_id) desc, submission_id desc
                limit :limit offset :offset
                """.formatted(tableName, String.join(" and ", predicates));

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
}
