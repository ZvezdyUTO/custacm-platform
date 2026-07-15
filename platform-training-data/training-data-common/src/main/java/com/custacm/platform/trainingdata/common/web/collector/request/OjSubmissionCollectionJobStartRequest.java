package com.custacm.platform.trainingdata.common.web.collector.request;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.time.Duration;
import java.util.List;

import static com.custacm.platform.trainingdata.common.support.Texts.requireText;

public record OjSubmissionCollectionJobStartRequest(
        List<String> usernames,
        Long lookbackHours,
        Boolean refreshWarehouse,
        String ojName
) {
    public List<String> requireUsernames() {
        if (usernames == null || usernames.isEmpty()) {
            throw new IllegalArgumentException("usernames must not be empty");
        }
        return usernames.stream()
                .map(username -> requireText(username, "username"))
                .distinct()
                .toList();
    }

    public Duration requireLookbackDuration() {
        if (lookbackHours == null || lookbackHours <= 0) {
            throw new IllegalArgumentException("lookbackHours must be positive");
        }
        try {
            return Duration.ofHours(lookbackHours);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("lookbackHours is too large", ex);
        }
    }

    public boolean refreshWarehouseOrDefault() {
        return Boolean.TRUE.equals(refreshWarehouse);
    }

    public String optionalOjName() {
        return ojName == null || ojName.isBlank() ? null : OjNames.normalize(ojName);
    }
}
