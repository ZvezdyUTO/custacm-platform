package com.custacm.platform.trainingdata.common.support;

import java.util.Objects;
import java.util.function.Function;

public final class Texts {
    private Texts() {
    }

    public static String requireText(String value, String fieldName) {
        return requireText(value, fieldName, IllegalArgumentException::new);
    }

    public static String requireText(
            String value,
            String fieldName,
            Function<String, ? extends RuntimeException> exceptionFactory
    ) {
        Objects.requireNonNull(exceptionFactory, "exceptionFactory must not be null");
        if (value == null || value.isBlank()) {
            throw exceptionFactory.apply(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
