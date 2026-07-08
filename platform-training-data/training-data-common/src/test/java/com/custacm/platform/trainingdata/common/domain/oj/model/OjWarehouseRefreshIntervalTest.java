package com.custacm.platform.trainingdata.common.domain.oj.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjWarehouseRefreshIntervalTest {
    @Test
    void acceptsInclusiveDateInterval() {
        OjWarehouseRefreshInterval interval = new OjWarehouseRefreshInterval(
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-03")
        );

        assertThat(interval.fromDateUtcPlus8()).isEqualTo(LocalDate.parse("2026-07-01"));
        assertThat(interval.toDateUtcPlus8()).isEqualTo(LocalDate.parse("2026-07-03"));
    }

    @Test
    void rejectsNullFromDate() {
        assertThatThrownBy(() -> new OjWarehouseRefreshInterval(null, LocalDate.parse("2026-07-03")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fromDateUtcPlus8 must not be null");
    }

    @Test
    void rejectsNullToDate() {
        assertThatThrownBy(() -> new OjWarehouseRefreshInterval(LocalDate.parse("2026-07-01"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("toDateUtcPlus8 must not be null");
    }

    @Test
    void rejectsInvertedDateInterval() {
        assertThatThrownBy(() -> new OjWarehouseRefreshInterval(
                LocalDate.parse("2026-07-03"),
                LocalDate.parse("2026-07-01")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("toDateUtcPlus8 must not be before fromDateUtcPlus8");
    }
}
