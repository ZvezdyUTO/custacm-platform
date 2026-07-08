package com.custacm.platform.auth.web;

public record LoginRequest(String studentIdentity, String password, Boolean rememberMe) {
    boolean rememberMeEnabled() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
