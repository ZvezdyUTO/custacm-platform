package com.custacm.platform.trainingdata.common.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

class CommonTrainingDataConfigTest {
    @Test
    void collectionJobExecutorRunsDifferentOjJobsConcurrently() throws Exception {
        ExecutorService executor = new CommonTrainingDataConfig().ojSubmissionCollectionJobExecutor();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Runnable blockingJob = () -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        try {
            executor.execute(blockingJob);
            executor.execute(blockingJob);

            assertThat(started.await(2, SECONDS)).isTrue();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, SECONDS)).isTrue();
        }
    }

    @Test
    void scheduledCollectionExecutorRunsDifferentOjsConcurrently() throws Exception {
        ExecutorService executor = new CommonTrainingDataConfig().ojScheduledCollectionExecutor();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Runnable blockingTask = () -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        try {
            executor.execute(blockingTask);
            executor.execute(blockingTask);

            assertThat(started.await(2, SECONDS)).isTrue();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(2, SECONDS)).isTrue();
        }
    }
}
