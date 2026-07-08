package com.custacm.platform.trainingdata.common.domain.oj.model;

import java.time.LocalDate;

public record OjWarehouseRefreshInterval(
        LocalDate fromDateUtcPlus8,
        LocalDate toDateUtcPlus8
) {
    public OjWarehouseRefreshInterval {
        if (fromDateUtcPlus8 == null) {
            throw new IllegalArgumentException("fromDateUtcPlus8 must not be null");
        }
        if (toDateUtcPlus8 == null) {
            throw new IllegalArgumentException("toDateUtcPlus8 must not be null");
        }
        if (toDateUtcPlus8.isBefore(fromDateUtcPlus8)) {
            throw new IllegalArgumentException("toDateUtcPlus8 must not be before fromDateUtcPlus8");
        }
    }
}
