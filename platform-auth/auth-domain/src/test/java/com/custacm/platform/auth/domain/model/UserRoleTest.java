package com.custacm.platform.auth.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRoleTest {
    @Test
    void parsesRolesAndKeepsHierarchy() {
        assertThat(UserRole.ADMIN.value()).isEqualTo("admin");
        assertThat(UserRole.fromValue(" ADMIN ")).isEqualTo(UserRole.ADMIN);
        assertThat(UserRole.fromValue("player")).isEqualTo(UserRole.PLAYER);
        assertThat(UserRole.fromValue("disable")).isEqualTo(UserRole.DISABLE);
        assertThat(UserRole.ADMIN.includes(UserRole.PLAYER)).isTrue();
        assertThat(UserRole.PLAYER.includes(UserRole.ADMIN)).isFalse();
        assertThat(UserRole.PLAYER.includes(UserRole.DISABLE)).isFalse();
    }

    @Test
    void onlyPlayerAndAdminCanAuthenticate() {
        assertThat(UserRole.DISABLE.canAuthenticate()).isFalse();
        assertThat(UserRole.PLAYER.canAuthenticate()).isTrue();
        assertThat(UserRole.ADMIN.canAuthenticate()).isTrue();
    }

    @Test
    void rejectsUnsupportedRoleValues() {
        assertThatThrownBy(() -> UserRole.fromValue("guest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
    }
}
