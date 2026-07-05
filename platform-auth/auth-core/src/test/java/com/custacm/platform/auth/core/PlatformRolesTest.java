package com.custacm.platform.auth.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformRolesTest {
    @Test
    void normalizesTokenRoles() {
        assertThat(PlatformRoles.requireTokenRole(" ADMIN ")).isEqualTo("admin");
        assertThat(PlatformRoles.requireTokenRole("Player")).isEqualTo("player");
    }

    @Test
    void rejectsNonTokenRolesInJwt() {
        assertThatThrownBy(() -> PlatformRoles.requireTokenRole("guest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
        assertThatThrownBy(() -> PlatformRoles.requireTokenRole("disable"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported");
    }
}
