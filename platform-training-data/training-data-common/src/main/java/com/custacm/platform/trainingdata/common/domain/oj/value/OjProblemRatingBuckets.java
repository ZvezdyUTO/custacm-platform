package com.custacm.platform.trainingdata.common.domain.oj.value;

import java.util.List;
import java.util.stream.IntStream;

public final class OjProblemRatingBuckets {
    public static final String UNRATED_KEY = "UNRATED";
    public static final List<Integer> RATINGS = IntStream.iterate(800, rating -> rating <= 3500, rating -> rating + 100)
            .boxed()
            .toList();

    private OjProblemRatingBuckets() {
    }

    public static String keyForRating(int rating) {
        return Integer.toString(rating);
    }
}
