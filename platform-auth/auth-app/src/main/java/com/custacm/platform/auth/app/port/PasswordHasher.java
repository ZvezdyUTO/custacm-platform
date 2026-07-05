package com.custacm.platform.auth.app.port;

public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
