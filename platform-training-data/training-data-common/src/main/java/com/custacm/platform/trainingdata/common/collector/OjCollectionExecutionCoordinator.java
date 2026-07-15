package com.custacm.platform.trainingdata.common.collector;

import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OjCollectionExecutionCoordinator {
    private final Map<String, Semaphore> permits = new ConcurrentHashMap<>();

    public void runExclusive(String ojName, Runnable action) {
        Runnable nonNullAction = Objects.requireNonNull(action, "action must not be null");
        try (Permit ignored = acquire(ojName)) {
            nonNullAction.run();
        }
    }

    public boolean tryRunExclusive(String ojName, Runnable action) {
        Runnable nonNullAction = Objects.requireNonNull(action, "action must not be null");
        Optional<Permit> acquired = tryAcquire(ojName);
        if (acquired.isEmpty()) {
            return false;
        }
        try (Permit ignored = acquired.orElseThrow()) {
            nonNullAction.run();
            return true;
        }
    }

    public Permit acquire(String ojName) {
        Semaphore semaphore = semaphore(ojName);
        semaphore.acquireUninterruptibly();
        return new Permit(semaphore);
    }

    public Optional<Permit> tryAcquire(String ojName) {
        Semaphore semaphore = semaphore(ojName);
        try {
            return semaphore.tryAcquire(0L, TimeUnit.NANOSECONDS)
                    ? Optional.of(new Permit(semaphore))
                    : Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Semaphore semaphore(String ojName) {
        String normalizedOjName = OjNames.normalize(ojName);
        return permits.computeIfAbsent(normalizedOjName, ignored -> new Semaphore(1, true));
    }

    public static final class Permit implements AutoCloseable {
        private final Semaphore semaphore;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private Permit(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}
