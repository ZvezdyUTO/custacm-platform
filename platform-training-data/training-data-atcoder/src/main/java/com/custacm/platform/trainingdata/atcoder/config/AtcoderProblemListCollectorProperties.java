package com.custacm.platform.trainingdata.atcoder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.training-data.atcoder.problem-list-collector")
public record AtcoderProblemListCollectorProperties(
        Boolean enabled,
        Boolean bootstrapOnStartup,
        Boolean bootstrapOnlyWhenEmpty,
        String cron,
        String zone
) {
    private static final String DEFAULT_CRON = "0 30 3 1/3 * ?";
    private static final String DEFAULT_ZONE = "Asia/Shanghai";

    public AtcoderProblemListCollectorProperties {
        if (enabled == null) {
            enabled = true;
        }
        if (bootstrapOnStartup == null) {
            bootstrapOnStartup = true;
        }
        if (bootstrapOnlyWhenEmpty == null) {
            bootstrapOnlyWhenEmpty = true;
        }
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
    }
}
