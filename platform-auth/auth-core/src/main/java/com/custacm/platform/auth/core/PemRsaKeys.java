package com.custacm.platform.auth.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class PemRsaKeys {
    private PemRsaKeys() {
    }

    public static RSAPrivateKey privateKey(String inlinePem, String pemPath) {
        String pem = readPem(inlinePem, pemPath, "private key");
        byte[] der = decodePem(pem, "PRIVATE KEY");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("invalid RSA private key", ex);
        }
    }

    public static RSAPublicKey publicKey(String inlinePem, String pemPath) {
        String pem = readPem(inlinePem, pemPath, "public key");
        byte[] der = decodePem(pem, "PUBLIC KEY");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("invalid RSA public key", ex);
        }
    }

    private static String readPem(String inlinePem, String pemPath, String keyName) {
        if (inlinePem != null && !inlinePem.isBlank()) {
            return inlinePem;
        }
        if (pemPath != null && !pemPath.isBlank()) {
            try {
                return Files.readString(Path.of(pemPath), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new IllegalArgumentException("failed to read RSA " + keyName + " from " + pemPath, ex);
            }
        }
        throw new IllegalArgumentException("missing RSA " + keyName);
    }

    private static byte[] decodePem(String pem, String type) {
        String normalized = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("empty PEM " + type);
        }
        return Base64.getDecoder().decode(normalized);
    }
}
