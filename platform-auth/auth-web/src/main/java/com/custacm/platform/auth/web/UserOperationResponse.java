package com.custacm.platform.auth.web;

public record UserOperationResponse(
        boolean success,
        String studentIdentity,
        UserResponse user,
        String plainPassword,
        String errorCode,
        String message
) {
}
