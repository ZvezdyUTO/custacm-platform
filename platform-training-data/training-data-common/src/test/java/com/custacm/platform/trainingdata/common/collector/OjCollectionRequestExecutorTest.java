package com.custacm.platform.trainingdata.common.collector;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OjCollectionRequestExecutorTest {
    @Test
    void retriesFailedRequestsAndSleepsBeforeRequestsAfterTheFirstOne() {
        List<String> events = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger();
        OjCollectionRequestExecutor executor = new OjCollectionRequestExecutor(
                3,
                Duration.ofSeconds(4),
                duration -> events.add("sleep:" + duration)
        );

        String result = executor.execute(() -> {
            events.add("request");
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("temporary source failure");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(events).containsExactly(
                "request",
                "sleep:PT4S",
                "request",
                "sleep:PT4S",
                "request"
        );
    }

    @Test
    void throwsLastFailureWhenRetriesAreExhausted() {
        OjCollectionRequestExecutor executor = new OjCollectionRequestExecutor(
                2,
                Duration.ZERO,
                duration -> {
                }
        );

        assertThatThrownBy(() -> executor.execute(() -> {
            throw new IllegalStateException("source still unavailable");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("source still unavailable");
    }
}
