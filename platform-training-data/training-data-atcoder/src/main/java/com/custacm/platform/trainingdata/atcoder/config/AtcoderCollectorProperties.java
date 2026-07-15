package com.custacm.platform.trainingdata.atcoder.config;

import com.custacm.platform.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.training-data.atcoder.collector")
public record AtcoderCollectorProperties(
        String baseUrl,
        int pageSize,
        Duration connectTimeout,
        Duration readTimeout,
        Duration requestInterval,
        int maxRequestAttempts
) {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_REQUEST_INTERVAL = Duration.ofSeconds(2);
    private static final int DEFAULT_MAX_REQUEST_ATTEMPTS = 3;

    public AtcoderCollectorProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://kenkoooo.com";
        }
        if (pageSize <= 0) {
            pageSize = AtcoderSubmissionSourceClient.USER_SUBMISSIONS_PAGE_LIMIT;
        } else if (pageSize > AtcoderSubmissionSourceClient.USER_SUBMISSIONS_PAGE_LIMIT) {
            throw new IllegalArgumentException(
                    "pageSize must not exceed the AtCoder user submissions limit of "
                            + AtcoderSubmissionSourceClient.USER_SUBMISSIONS_PAGE_LIMIT
            );
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
