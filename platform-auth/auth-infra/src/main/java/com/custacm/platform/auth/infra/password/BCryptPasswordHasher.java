package com.custacm.platform.auth.infra.password;

import com.custacm.platform.auth.app.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BCryptPasswordHasher implements PasswordHasher {
    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasher() {
        this(new BCryptPasswordEncoder());
    }

    BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
