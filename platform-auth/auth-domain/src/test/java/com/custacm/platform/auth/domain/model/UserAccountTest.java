package com.custacm.platform.auth.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {
    private static final Instant NOW = Instant.parse("2026-07-04T12:00:00Z");

    @Test
    void storesAccountFieldsAsGiven() {
        UserAccount account = new UserAccount("230511213黄炳睿", "hash", UserRole.PLAYER, NOW, NOW);

        assertThat(account.studentIdentity()).isEqualTo("230511213黄炳睿");
        assertThat(account.passwordHash()).isEqualTo("hash");
        assertThat(account.role()).isEqualTo(UserRole.PLAYER);
        assertThat(account.createdAt()).isEqualTo(NOW);
        assertThat(account.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void createsUpdatedCopies() {
        UserAccount account = new UserAccount("230511213黄炳睿", "old", UserRole.PLAYER, NOW, NOW);

        UserAccount passwordChanged = account.withPasswordHash("new", NOW.plusSeconds(60));
        UserAccount roleChanged = passwordChanged.withRole(UserRole.DISABLE, NOW.plusSeconds(120));

        assertThat(passwordChanged.passwordHash()).isEqualTo("new");
        assertThat(roleChanged.role()).isEqualTo(UserRole.DISABLE);
    }
}
