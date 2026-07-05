package com.custacm.platform.auth.app.port;

import com.custacm.platform.auth.app.result.IssuedToken;
import com.custacm.platform.auth.domain.model.UserRole;

public interface AccessTokenIssuer {
    IssuedToken issue(String studentIdentity, UserRole role);
}
