package com.custacm.platform.trainingdata.common.infra.oj.repo.query;

import com.custacm.platform.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.common.domain.oj.model.OjDailyRatingAcceptedSummary;
import com.custacm.platform.trainingdata.common.domain.oj.repo.OjAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjProblemRatingBuckets;
import com.custacm.platform.trainingdata.common.infra.oj.repo.warehouse.OjWarehouseTableNames;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcOjAcceptedSummaryRepository implements OjAcceptedSummaryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcOjAcceptedSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OjDailyRatingAcceptedSummary> findDailyRatingAcceptedSummaries(
            OjAcceptedSummaryCriteria query
    ) {
        String tableName = OjWarehouseTableNames.dwsHandleDailyRatingAcceptedSummary(query.ojName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("handle", query.authorHandle());
        List<String> predicates = new ArrayList<>();
        predicates.add("handle = :handle");

        if (query.acceptedFromDateUtcPlus8() != null) {
            predicates.add("accepted_date_utc_plus8 >= :acceptedFromDateUtcPlus8");
            params.addValue("acceptedFromDateUtcPlus8", Date.valueOf(query.acceptedFromDateUtcPlus8()));
        }
        if (query.acceptedToDateUtcPlus8() != null) {
            predicates.add("accepted_date_utc_plus8 <= :acceptedToDateUtcPlus8");
            params.addValue("acceptedToDateUtcPlus8", Date.valueOf(query.acceptedToDateUtcPlus8()));
        }
        addProblemRatingPredicates(query.minProblemRating(), query.maxProblemRating(), predicates, params);

        String sql = """
                select
                    handle,
                    accepted_date_utc_plus8,
                    difficulty,
                    accepted_problem_count
                from %s
                where %s
                order by accepted_date_utc_plus8 asc, difficulty asc
                """.formatted(tableName, String.join(" and ", predicates));

        Map<String, OjDailyRatingAcceptedSummaryBuilder> builders = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            String handle = rs.getString("handle");
            Date acceptedDate = rs.getDate("accepted_date_utc_plus8");
            String key = handle + "\n" + acceptedDate;
            builders.computeIfAbsent(key, ignored -> new OjDailyRatingAcceptedSummaryBuilder(
                    handle,
                    acceptedDate.toLocalDate()
            )).put(rs.getString("difficulty"), rs.getInt("accepted_problem_count"));
        });
        return builders.values().stream()
                .map(OjDailyRatingAcceptedSummaryBuilder::build)
                .toList();
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

    private static final class OjDailyRatingAcceptedSummaryBuilder {
        private final String handle;
        private final java.time.LocalDate acceptedDateUtcPlus8;
        private final Map<String, Integer> counts = new LinkedHashMap<>();

        private OjDailyRatingAcceptedSummaryBuilder(String handle, java.time.LocalDate acceptedDateUtcPlus8) {
            this.handle = handle;
            this.acceptedDateUtcPlus8 = acceptedDateUtcPlus8;
        }

        private void put(String difficulty, int count) {
            counts.put(difficulty, count);
        }

        private OjDailyRatingAcceptedSummary build() {
            return new OjDailyRatingAcceptedSummary(handle, acceptedDateUtcPlus8, counts);
        }
    }
}
