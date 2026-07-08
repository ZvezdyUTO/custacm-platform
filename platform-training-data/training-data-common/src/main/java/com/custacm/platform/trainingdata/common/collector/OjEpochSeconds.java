package com.custacm.platform.trainingdata.common.collector;

import java.time.Instant;

public final class OjEpochSeconds {
    private OjEpochSeconds() {
    }

    public static long ceilingEpochSecond(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("instant must not be null");
        }
        long epochSecond = instant.getEpochSecond();
        return instant.getNano() == 0 ? epochSecond : Math.addExact(epochSecond, 1L);
    }
}
