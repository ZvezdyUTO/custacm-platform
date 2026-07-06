package com.custacm.platform.trainingdata.codeforces.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "platform.training-data.codeforces.collector")
public record CodeforcesCollectorProperties(
        String baseUrl,
        int pageSize,
        Duration connectTimeout,
        Duration readTimeout,
        Duration requestInterval,
        int maxRequestAttempts,
        List<Schedule> schedules
) {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_REQUEST_INTERVAL = Duration.ofSeconds(4);
    private static final int DEFAULT_MAX_REQUEST_ATTEMPTS = 3;

    public CodeforcesCollectorProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://codeforces.com";
        }
        if (pageSize <= 0) {
            pageSize = 1000;
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            readTimeout = DEFAULT_READ_TIMEOUT;
        }
        if (requestInterval == null || requestInterval.isNegative()) {
            requestInterval = DEFAULT_REQUEST_INTERVAL;
        }
        if (maxRequestAttempts <= 0) {
            maxRequestAttempts = DEFAULT_MAX_REQUEST_ATTEMPTS;
        }
        schedules = schedules == null ? List.of() : schedules.stream()
                .map(Schedule::normalized)
                .toList();
    }

    public List<Schedule> enabledSchedules() {
        return schedules.stream()
                .filter(Schedule::enabled)
                .toList();
    }

    public record Schedule(
            String name,
            boolean enabled,
            String cron,
            String zone,
            Duration lookback
    ) {
        private static final String DEFAULT_NAME = "daily-recent-submissions";
        private static final String DEFAULT_CRON = "0 0 12 * * *";
        private static final String DEFAULT_ZONE = "Asia/Shanghai";
        private static final Duration DEFAULT_LOOKBACK = Duration.ofHours(120);

        private static Schedule normalized(Schedule schedule) {
            if (schedule == null) {
                return new Schedule(DEFAULT_NAME, false, DEFAULT_CRON, DEFAULT_ZONE, DEFAULT_LOOKBACK);
            }
            return schedule;
        }

        public Schedule {
            if (name == null || name.isBlank()) {
                name = DEFAULT_NAME;
            }
            if (cron == null || cron.isBlank()) {
                cron = DEFAULT_CRON;
            }
            if (zone == null || zone.isBlank()) {
                zone = DEFAULT_ZONE;
            }
            if (lookback == null || lookback.isNegative() || lookback.isZero()) {
                lookback = DEFAULT_LOOKBACK;
            }
        }
    }
}
