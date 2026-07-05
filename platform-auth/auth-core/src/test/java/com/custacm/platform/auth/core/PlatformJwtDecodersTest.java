package com.custacm.platform.auth.core;

import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformJwtDecodersTest {
    @Test
    void buildsRsaJwtDecoder() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        assertThat(PlatformJwtDecoders.rsa((RSAPublicKey) generator.generateKeyPair().getPublic()))
                .isNotNull();
    }
}
