package com.custacm.platform.trainingdata.common.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "platform.training-data.collector")
public record OjCollectorSchedulingProperties(
        List<Schedule> schedules,
        Duration jobItemInterval
) {
    private static final Duration DEFAULT_JOB_ITEM_INTERVAL = Duration.ofSeconds(4);

    public OjCollectorSchedulingProperties {
        schedules = schedules == null ? List.of() : schedules.stream()
                .map(Schedule::normalized)
                .toList();
        if (jobItemInterval == null || jobItemInterval.isNegative()) {
            jobItemInterval = DEFAULT_JOB_ITEM_INTERVAL;
        }
    }

    public List<Schedule> enabledSchedules() {
        return schedules.stream()
                .filter(schedule -> Boolean.TRUE.equals(schedule.enabled()))
                .toList();
    }

    public record Schedule(
            String name,
            String ojName,
            Boolean enabled,
            String cron,
            String zone,
            Duration lookback
    ) {
        private static final String DEFAULT_NAME = "daily-recent-submissions";
        private static final String DEFAULT_OJ_NAME = "CODEFORCES";
        private static final String DEFAULT_CRON = "0 0 12 * * *";
        private static final String DEFAULT_ZONE = "Asia/Shanghai";
        private static final Duration DEFAULT_LOOKBACK = Duration.ofHours(120);

        private static Schedule normalized(Schedule schedule) {
            if (schedule == null) {
                return new Schedule(
                        DEFAULT_NAME,
                        DEFAULT_OJ_NAME,
                        true,
                        DEFAULT_CRON,
                        DEFAULT_ZONE,
                        DEFAULT_LOOKBACK
                );
            }
            return schedule;
        }

        public Schedule {
            if (name == null || name.isBlank()) {
                name = DEFAULT_NAME;
            } else {
                name = name.trim();
            }
            if (enabled == null) {
                enabled = true;
            }
            ojName = ojName == null || ojName.isBlank()
                    ? DEFAULT_OJ_NAME
                    : normalizeOjName(ojName);
            if (cron == null || cron.isBlank()) {
                cron = DEFAULT_CRON;
            } else {
                cron = cron.trim();
            }
            if (zone == null || zone.isBlank()) {
                zone = DEFAULT_ZONE;
            } else {
                zone = zone.trim();
            }
            if (lookback == null || lookback.isNegative() || lookback.isZero()) {
                lookback = DEFAULT_LOOKBACK;
            }
        }

        private static String normalizeOjName(String value) {
            return value.trim().toUpperCase(Locale.ROOT);
        }
    }
}
