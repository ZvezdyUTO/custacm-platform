package com.custacm.platform.auth.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakRolesTest {
    @Test
    void readsRealmAndClientRoles() {
        Map<String, Object> claims = Map.of(
                "realm_access", Map.of("roles", List.of("student")),
                "resource_access", Map.of(
                        "custacm-web", Map.of("roles", List.of("admin"))
                )
        );

        assertThat(KeycloakRoles.fromClaims(claims)).containsExactly("student", "admin");
    }

    @Test
    void choosesAdminBeforeStudent() {
        assertThat(KeycloakRoles.platformRoleFrom(Set.of("student", "admin")))
                .contains("admin");
    }

    @Test
    void choosesStudentWhenAdminIsAbsent() {
        assertThat(KeycloakRoles.platformRoleFrom(Set.of("student")))
                .contains("student");
    }

    @Test
    void rejectsTokensWithoutPlatformRole() {
        assertThatThrownBy(() -> KeycloakRoles.platformRoleFromClaims(Map.of(
                "realm_access", Map.of("roles", List.of("offline_access"))
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin or student");
    }
}
