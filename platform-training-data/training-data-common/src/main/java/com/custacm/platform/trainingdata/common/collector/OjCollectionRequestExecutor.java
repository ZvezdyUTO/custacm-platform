package com.custacm.platform.trainingdata.common.collector;

import java.time.Duration;

public final class OjCollectionRequestExecutor {
    private final int maxRequestAttempts;
    private final Duration requestInterval;
    private final SleepStrategy sleepStrategy;
    private boolean requestSent;

    public OjCollectionRequestExecutor(
            int maxRequestAttempts,
            Duration requestInterval,
            SleepStrategy sleepStrategy
    ) {
        if (maxRequestAttempts <= 0) {
            throw new IllegalArgumentException("maxRequestAttempts must be positive");
        }
        this.maxRequestAttempts = maxRequestAttempts;
        this.requestInterval = requestInterval == null || requestInterval.isNegative()
                ? Duration.ZERO
                : requestInterval;
        this.sleepStrategy = sleepStrategy;
    }

    public <T> T execute(RequestOperation<T> operation) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxRequestAttempts; attempt++) {
            beforeRequest();
            try {
                return operation.execute();
            } catch (RuntimeException ex) {
                lastFailure = ex;
            }
        }
        throw lastFailure;
    }

    private void beforeRequest() {
        if (requestSent && !requestInterval.isZero()) {
            try {
                sleepStrategy.sleep(requestInterval);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while rate limiting OJ collection", ex);
            }
        }
        requestSent = true;
    }

    @FunctionalInterface
    public interface RequestOperation<T> {
        T execute();
    }

    @FunctionalInterface
    public interface SleepStrategy {
        void sleep(Duration duration) throws InterruptedException;
    }
}
