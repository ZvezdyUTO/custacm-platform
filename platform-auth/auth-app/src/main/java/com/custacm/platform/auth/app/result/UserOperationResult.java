package com.custacm.platform.auth.app.result;

import com.custacm.platform.auth.domain.model.UserAccount;

public record UserOperationResult(
        boolean success,
        String studentIdentity,
        UserAccount account,
        String plainPassword,
        String errorCode,
        String message
) {
    public static UserOperationResult success(UserAccount account, String plainPassword, String message) {
        return new UserOperationResult(true, account.studentIdentity(), account, plainPassword, null, message);
    }

    public static UserOperationResult failure(String studentIdentity, String errorCode, String message) {
        return new UserOperationResult(false, studentIdentity, null, null, errorCode, message);
    }
}
