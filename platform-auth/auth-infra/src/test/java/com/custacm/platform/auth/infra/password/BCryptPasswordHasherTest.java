package com.custacm.platform.auth.infra.password;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {
    @Test
    void hashesAndVerifiesPassword() {
        BCryptPasswordHasher hasher = new BCryptPasswordHasher();

        String hash = hasher.hash("secret123");

        assertThat(hash).isNotEqualTo("secret123");
        assertThat(hasher.matches("secret123", hash)).isTrue();
        assertThat(hasher.matches("bad-password", hash)).isFalse();
    }
}
