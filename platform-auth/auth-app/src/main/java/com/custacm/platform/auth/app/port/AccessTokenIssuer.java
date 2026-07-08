package com.custacm.platform.auth.app.port;

import com.custacm.platform.auth.app.result.IssuedToken;
import com.custacm.platform.auth.domain.model.UserRole;

import java.time.Duration;

public interface AccessTokenIssuer {
    default IssuedToken issue(String studentIdentity, UserRole role) {
        return issue(studentIdentity, role, null);
    }

    IssuedToken issue(String studentIdentity, UserRole role, Duration ttl);
}
