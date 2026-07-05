package com.custacm.platform.auth.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PemRsaKeysTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsInlinePrivateAndPublicKeys() throws Exception {
        KeyPair keyPair = rsaKeyPair();

        RSAPrivateKey privateKey = PemRsaKeys.privateKey(privatePem(keyPair), null);
        RSAPublicKey publicKey = PemRsaKeys.publicKey(publicPem(keyPair), null);

        assertThat(privateKey.getModulus()).isEqualTo(((RSAPrivateKey) keyPair.getPrivate()).getModulus());
        assertThat(publicKey.getModulus()).isEqualTo(((RSAPublicKey) keyPair.getPublic()).getModulus());
    }

    @Test
    void loadsPemFromPathWhenInlineValueIsBlank() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        Path publicKeyPath = tempDir.resolve("public.pem");
        Files.writeString(publicKeyPath, publicPem(keyPair), StandardCharsets.UTF_8);

        RSAPublicKey publicKey = PemRsaKeys.publicKey("  ", publicKeyPath.toString());

        assertThat(publicKey.getModulus()).isEqualTo(((RSAPublicKey) keyPair.getPublic()).getModulus());
    }

    @Test
    void reportsMissingKeyConfiguration() {
        assertThatThrownBy(() -> PemRsaKeys.privateKey(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing RSA private key");
        assertThatThrownBy(() -> PemRsaKeys.publicKey(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing RSA public key");
    }

    @Test
    void reportsUnreadablePemPath() {
        assertThatThrownBy(() -> PemRsaKeys.publicKey(null, tempDir.resolve("missing.pem").toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failed to read RSA public key");
    }

    @Test
    void rejectsEmptyAndInvalidPem() {
        assertThatThrownBy(() -> PemRsaKeys.privateKey("-----BEGIN PRIVATE KEY-----\n-----END PRIVATE KEY-----", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty PEM PRIVATE KEY");

        String invalidPrivateKey = pem("PRIVATE KEY", "not a key".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> PemRsaKeys.privateKey(invalidPrivateKey, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid RSA private key");
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String privatePem(KeyPair keyPair) {
        return pem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
    }

    private static String publicPem(KeyPair keyPair) {
        return pem("PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    private static String pem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }
}
