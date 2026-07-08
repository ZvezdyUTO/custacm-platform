package com.custacm.platform.trainingdata.common.collector;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjEpochSecondsTest {
    @Test
    void keepsExactEpochSecondBoundary() {
        Instant instant = Instant.parse("2026-07-05T04:00:00Z");

        assertThat(OjEpochSeconds.ceilingEpochSecond(instant))
                .isEqualTo(instant.getEpochSecond());
    }

    @Test
    void roundsSubSecondInstantUpToNextEpochSecond() {
        Instant instant = Instant.parse("2026-07-05T04:00:00.001Z");

        assertThat(OjEpochSeconds.ceilingEpochSecond(instant))
                .isEqualTo(instant.getEpochSecond() + 1L);
    }

    @Test
    void rejectsNullInstant() {
        assertThatThrownBy(() -> OjEpochSeconds.ceilingEpochSecond(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("instant must not be null");
    }
}
