package com.custacm.platform.trainingdata.codeforces.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.training-data.codeforces.collector")
public record CodeforcesCollectorProperties(
        String baseUrl,
        int pageSize,
        Duration connectTimeout,
        Duration readTimeout,
        Duration requestInterval,
        int maxRequestAttempts
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
    }
}
