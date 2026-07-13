package com.custacm.platform.trainingdata.common.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjSubmissionWindowFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void filtersSubmissionsInsideWindowAndDetectsOlderSortedPages() {
        ArrayNode page = objectMapper.createArrayNode()
                .add(submission(101, "2026-07-02T00:00:00Z"))
                .add(submission(102, "2026-06-30T04:00:00Z"))
                .add(submission(103, "2026-06-29T23:59:59Z"));

        var result = OjSubmissionWindowFilter.filterSortedPage(
                page,
                Instant.parse("2026-06-30T04:00:00Z"),
                Instant.parse("2026-07-05T04:00:00Z"),
                node -> OptionalLong.of(node.path("submittedAt").asLong())
        );

        assertThat(result.matchedSubmissions())
                .extracting(node -> node.path("id").asLong())
                .containsExactly(101L, 102L);
        assertThat(result.allSubmissionsAreOlderThanWindow()).isFalse();
        assertThat(result.maxEpochSecond())
                .hasValue(Instant.parse("2026-07-02T00:00:00Z").getEpochSecond());
    }

    @Test
    void marksPageOlderThanWindowWhenEverySubmissionPrecedesStart() {
        ArrayNode page = objectMapper.createArrayNode()
                .add(submission(201, "2026-06-29T00:00:00Z"))
                .add(submission(202, "2026-06-28T00:00:00Z"));

        var result = OjSubmissionWindowFilter.filterSortedPage(
                page,
                Instant.parse("2026-06-30T04:00:00Z"),
                Instant.parse("2026-07-05T04:00:00Z"),
                node -> OptionalLong.of(node.path("submittedAt").asLong())
        );

        assertThat(result.matchedSubmissions()).isEmpty();
        assertThat(result.allSubmissionsAreOlderThanWindow()).isTrue();
        assertThat(result.maxEpochSecond())
                .hasValue(Instant.parse("2026-06-29T00:00:00Z").getEpochSecond());
    }

    @Test
    void usesCeilingEpochSecondsForSubSecondWindowBounds() {
        ArrayNode page = objectMapper.createArrayNode()
                .add(submission(301, 100L))
                .add(submission(302, 101L))
                .add(submission(303, 105L))
                .add(submission(304, 106L));

        var result = OjSubmissionWindowFilter.filterSortedPage(
                page,
                Instant.ofEpochSecond(100L, 1L),
                Instant.ofEpochSecond(105L, 1L),
                node -> OptionalLong.of(node.path("submittedAt").asLong())
        );

        assertThat(result.matchedSubmissions())
                .extracting(node -> node.path("id").asLong())
                .containsExactly(302L, 303L);
        assertThat(result.maxEpochSecond()).hasValue(106L);
    }

    @Test
    void rejectsNonArrayPage() {
        assertThatThrownBy(() -> OjSubmissionWindowFilter.filterSortedPage(
                objectMapper.createObjectNode(),
                Instant.parse("2026-06-30T04:00:00Z"),
                Instant.parse("2026-07-05T04:00:00Z"),
                node -> OptionalLong.empty()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OJ submission page must be an array");
    }

    @Test
    void acceptsAnEmptyWindowForZeroLookback() {
        Instant cursor = Instant.parse("2026-07-05T04:00:00Z");

        var result = OjSubmissionWindowFilter.filterSortedPage(
                objectMapper.createArrayNode().add(submission(401, cursor.minusSeconds(1).getEpochSecond())),
                cursor,
                cursor,
                node -> OptionalLong.of(node.path("submittedAt").asLong())
        );

        assertThat(result.matchedSubmissions()).isEmpty();
        assertThat(result.allSubmissionsAreOlderThanWindow()).isTrue();
    }

    private com.fasterxml.jackson.databind.node.ObjectNode submission(long id, String submittedAt) {
        return submission(id, Instant.parse(submittedAt).getEpochSecond());
    }

    private com.fasterxml.jackson.databind.node.ObjectNode submission(long id, long submittedAt) {
        return objectMapper.createObjectNode()
                .put("id", id)
                .put("submittedAt", submittedAt);
    }
}
