package com.custacm.platform.trainingdata.common.collector;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public final class OjSubmissionWindowFilter {
    private OjSubmissionWindowFilter() {
    }

    public static WindowedSubmissionPage filterSortedPage(
            JsonNode page,
            Instant windowStartInclusive,
            Instant windowEndExclusive,
            EpochSecondExtractor submittedEpochSecondExtractor
    ) {
        requireWindow(windowStartInclusive, windowEndExclusive);
        if (page == null || !page.isArray()) {
            throw new IllegalArgumentException("OJ submission page must be an array");
        }
        long startEpochSecond = OjEpochSeconds.ceilingEpochSecond(windowStartInclusive);
        long endEpochSecond = OjEpochSeconds.ceilingEpochSecond(windowEndExclusive);
        List<JsonNode> matchedSubmissions = new ArrayList<>();
        boolean allSubmissionsAreOlderThanWindow = !page.isEmpty();
        OptionalLong maxEpochSecond = OptionalLong.empty();
        for (JsonNode submission : page) {
            OptionalLong submittedEpochSecond = submittedEpochSecondExtractor.extract(submission);
            if (submittedEpochSecond.isEmpty()) {
                allSubmissionsAreOlderThanWindow = false;
                continue;
            }
            long epochSecond = submittedEpochSecond.getAsLong();
            if (maxEpochSecond.isEmpty() || epochSecond > maxEpochSecond.getAsLong()) {
                maxEpochSecond = OptionalLong.of(epochSecond);
            }
            if (epochSecond >= startEpochSecond && epochSecond < endEpochSecond) {
                matchedSubmissions.add(submission.deepCopy());
            }
            if (epochSecond >= startEpochSecond) {
                allSubmissionsAreOlderThanWindow = false;
            }
        }
        return new WindowedSubmissionPage(matchedSubmissions, allSubmissionsAreOlderThanWindow, maxEpochSecond);
    }

    private static void requireWindow(Instant windowStartInclusive, Instant windowEndExclusive) {
        if (windowStartInclusive == null) {
            throw new IllegalArgumentException("windowStartInclusive must not be null");
        }
        if (windowEndExclusive == null) {
            throw new IllegalArgumentException("windowEndExclusive must not be null");
        }
        if (!windowStartInclusive.isBefore(windowEndExclusive)) {
            throw new IllegalArgumentException("windowStartInclusive must be before windowEndExclusive");
        }
    }

    @FunctionalInterface
    public interface EpochSecondExtractor {
        OptionalLong extract(JsonNode submission);
    }

    public record WindowedSubmissionPage(
            List<JsonNode> matchedSubmissions,
            boolean allSubmissionsAreOlderThanWindow,
            OptionalLong maxEpochSecond
    ) {
        public WindowedSubmissionPage {
            matchedSubmissions = matchedSubmissions == null ? List.of() : List.copyOf(matchedSubmissions);
            maxEpochSecond = maxEpochSecond == null ? OptionalLong.empty() : maxEpochSecond;
        }
    }
}
