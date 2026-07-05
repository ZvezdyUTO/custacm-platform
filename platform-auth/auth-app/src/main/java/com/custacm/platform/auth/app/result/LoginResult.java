package com.custacm.platform.auth.app.result;

import com.custacm.platform.auth.domain.model.UserAccount;

public record LoginResult(UserAccount user, IssuedToken token) {
}
