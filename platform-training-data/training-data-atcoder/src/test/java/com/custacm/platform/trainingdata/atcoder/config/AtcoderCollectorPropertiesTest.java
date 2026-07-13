package com.custacm.platform.trainingdata.atcoder.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AtcoderCollectorPropertiesTest {
    @Test
    void appliesCollectorDefaults() {
        AtcoderCollectorProperties properties = new AtcoderCollectorProperties(
                null,
                0,
                null,
                null,
                null,
                0
        );

        assertThat(properties.baseUrl()).isEqualTo("https://kenkoooo.com");
        assertThat(properties.pageSize()).isEqualTo(500);
        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.requestInterval()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.maxRequestAttempts()).isEqualTo(3);
    }

    @Test
    void appliesProblemListScheduleDefaults() {
        AtcoderProblemListCollectorProperties properties =
                new AtcoderProblemListCollectorProperties(null, null, null, null, null);

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.bootstrapOnStartup()).isFalse();
        assertThat(properties.bootstrapOnlyWhenEmpty()).isTrue();
        assertThat(properties.cron()).isEqualTo("0 30 3 1/3 * ?");
        assertThat(properties.zone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    void allowsDisablingProblemListScheduleExplicitly() {
        AtcoderProblemListCollectorProperties properties =
                new AtcoderProblemListCollectorProperties(false, false, false, null, null);

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.bootstrapOnStartup()).isFalse();
        assertThat(properties.bootstrapOnlyWhenEmpty()).isFalse();
    }
}
