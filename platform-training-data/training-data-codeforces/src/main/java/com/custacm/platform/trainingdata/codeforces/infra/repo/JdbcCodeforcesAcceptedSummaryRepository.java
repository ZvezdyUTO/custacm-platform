package com.custacm.platform.trainingdata.codeforces.infra.repo;

import com.custacm.platform.trainingdata.codeforces.domain.criteria.CodeforcesAcceptedSummaryCriteria;
import com.custacm.platform.trainingdata.codeforces.domain.repo.CodeforcesAcceptedSummaryRepository;
import com.custacm.platform.trainingdata.codeforces.domain.model.CodeforcesDailyRatingAcceptedSummary;
import com.custacm.platform.trainingdata.codeforces.domain.value.CodeforcesProblemRatingBuckets;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcCodeforcesAcceptedSummaryRepository implements CodeforcesAcceptedSummaryRepository {
    private static final String TABLE_NAME = "dws_codeforces__handle_daily_rating_accepted_summary";
    private static final String UNRATED_COLUMN = "unrated_accepted_problem_count";
    private static final String RATING_COUNT_COLUMNS = ratingCountColumns();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcCodeforcesAcceptedSummaryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CodeforcesDailyRatingAcceptedSummary> findDailyRatingAcceptedSummaries(
            CodeforcesAcceptedSummaryCriteria query
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("authorHandle", query.authorHandle());
        List<String> predicates = new ArrayList<>();
        predicates.add("author_handle = :authorHandle");

        if (query.acceptedFromDateUtcPlus8() != null) {
            predicates.add("accepted_date_utc_plus8 >= :acceptedFromDateUtcPlus8");
            params.addValue("acceptedFromDateUtcPlus8", Date.valueOf(query.acceptedFromDateUtcPlus8()));
        }
        if (query.acceptedToDateUtcPlus8() != null) {
            predicates.add("accepted_date_utc_plus8 <= :acceptedToDateUtcPlus8");
            params.addValue("acceptedToDateUtcPlus8", Date.valueOf(query.acceptedToDateUtcPlus8()));
        }
        addProblemRatingPredicates(query.minProblemRating(), query.maxProblemRating(), predicates);

        String sql = """
                select
                    author_handle,
                    accepted_date_utc_plus8,
                    %s,
                    %s
                from %s
                where %s
                order by accepted_date_utc_plus8 asc
                """.formatted(RATING_COUNT_COLUMNS, UNRATED_COLUMN, TABLE_NAME, String.join(" and ", predicates));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Map<String, Integer> acceptedProblemCountsByRating = new HashMap<>();
            for (Integer rating : CodeforcesProblemRatingBuckets.RATINGS) {
                acceptedProblemCountsByRating.put(
                        CodeforcesProblemRatingBuckets.keyForRating(rating),
                        rs.getInt(ratingColumn(rating))
                );
            }
            acceptedProblemCountsByRating.put(
                    CodeforcesProblemRatingBuckets.UNRATED_KEY,
                    rs.getInt(UNRATED_COLUMN)
            );
            return new CodeforcesDailyRatingAcceptedSummary(
                    rs.getString("author_handle"),
                    rs.getDate("accepted_date_utc_plus8").toLocalDate(),
                    acceptedProblemCountsByRating
            );
        });
    }

    private static void addProblemRatingPredicates(
            Integer minProblemRating,
            Integer maxProblemRating,
            List<String> predicates
    ) {
        if (minProblemRating == null && maxProblemRating == null) {
            return;
        }

        List<String> ratingPredicates = new ArrayList<>();
        for (Integer rating : CodeforcesProblemRatingBuckets.RATINGS) {
            if (withinProblemRatingBounds(rating, minProblemRating, maxProblemRating)) {
                ratingPredicates.add(ratingColumn(rating) + " > 0");
            }
        }
        if (ratingPredicates.isEmpty()) {
            predicates.add("1 = 0");
        } else {
            predicates.add("(" + String.join(" or ", ratingPredicates) + ")");
        }
    }

    private static boolean withinProblemRatingBounds(
            int problemRating,
            Integer minProblemRating,
            Integer maxProblemRating
    ) {
        if (minProblemRating != null && problemRating < minProblemRating) {
            return false;
        }
        return maxProblemRating == null || problemRating <= maxProblemRating;
    }

    private static String ratingColumn(int rating) {
        return "rating_" + rating + "_accepted_problem_count";
    }

    private static String ratingCountColumns() {
        return CodeforcesProblemRatingBuckets.RATINGS.stream()
                .map(JdbcCodeforcesAcceptedSummaryRepository::ratingColumn)
                .reduce((left, right) -> left + ",\n                    " + right)
                .orElseThrow();
    }
}
