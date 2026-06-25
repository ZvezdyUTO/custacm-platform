package com.custacm.platform.auth.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthModuleControllerTest {
    private final AuthModuleController controller = new AuthModuleController();

    @Test
    void healthReturnsServiceStatus() {
        assertThat(controller.health())
                .containsEntry("status", "UP")
                .containsEntry("service", "auth-web");
    }

    @Test
    void moduleInfoReturnsAuthMetadata() {
        assertThat(controller.moduleInfo())
                .containsEntry("module", "platform-auth")
                .containsEntry("service", "auth-web");
    }
}
