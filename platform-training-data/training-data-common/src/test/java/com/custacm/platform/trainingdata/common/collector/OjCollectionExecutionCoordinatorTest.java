package com.custacm.platform.trainingdata.common.collector;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

class OjCollectionExecutionCoordinatorTest {
    @Test
    void allowsDifferentOjsToRunConcurrently() throws Exception {
        OjCollectionExecutionCoordinator coordinator = new OjCollectionExecutionCoordinator();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try {
            executor.execute(() -> coordinator.runExclusive("CODEFORCES", () -> awaitRelease(started, release)));
            executor.execute(() -> coordinator.runExclusive("ATCODER", () -> awaitRelease(started, release)));

            assertThat(started.await(2, SECONDS)).isTrue();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, SECONDS)).isTrue();
        }
    }

    @Test
    void rejectsOverlappingTryRunForSameOj() throws Exception {
        OjCollectionExecutionCoordinator coordinator = new OjCollectionExecutionCoordinator();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try {
            executor.execute(() -> coordinator.runExclusive("CODEFORCES", () -> awaitRelease(started, release)));
            assertThat(started.await(2, SECONDS)).isTrue();

            assertThat(coordinator.tryRunExclusive("CODEFORCES", () -> {
                throw new AssertionError("overlapping action must not run");
            })).isFalse();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, SECONDS)).isTrue();
        }
    }

    private static void awaitRelease(CountDownLatch started, CountDownLatch release) {
        started.countDown();
        try {
            release.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
