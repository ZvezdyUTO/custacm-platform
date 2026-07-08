package com.custacm.platform.trainingdata.common.domain.oj.value;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class OjNames {
    public static final String CODEFORCES = "CODEFORCES";
    public static final String ATCODER = "ATCODER";

    private static final Set<String> SUPPORTED_NAMES = new LinkedHashSet<>(Set.of(CODEFORCES, ATCODER));

    private OjNames() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("oj name must not be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_NAMES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported oj name: " + value);
        }
        return normalized;
    }

    public static Set<String> supportedNames() {
        return Set.copyOf(SUPPORTED_NAMES);
    }
}
