package com.custacm.platform.trainingdata.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.auth.jwt")
public record TrainingDataJwtProperties(String publicKey, String publicKeyPath) {
}
