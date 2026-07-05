package com.custacm.platform.auth.web;

import com.custacm.platform.auth.app.result.UserOperationResult;
import com.custacm.platform.auth.domain.model.UserAccount;

final class UserResponseMapper {
    private UserResponseMapper() {
    }

    static UserResponse toResponse(UserAccount account) {
        return new UserResponse(
                account.studentIdentity(),
                account.role().value(),
                account.createdAt(),
                account.updatedAt()
        );
    }

    static UserOperationResponse toOperationResponse(UserOperationResult result) {
        UserResponse user = result.account() == null ? null : toResponse(result.account());
        return new UserOperationResponse(
                result.success(),
                result.studentIdentity(),
                user,
                result.plainPassword(),
                result.errorCode(),
                result.message()
        );
    }
}
