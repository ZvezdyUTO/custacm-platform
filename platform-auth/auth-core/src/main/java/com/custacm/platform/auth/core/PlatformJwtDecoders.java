package com.custacm.platform.auth.core;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;

public final class PlatformJwtDecoders {
    private PlatformJwtDecoders() {
    }

    public static JwtDecoder rsa(RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
