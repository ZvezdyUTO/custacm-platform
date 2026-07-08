package com.custacm.platform.auth.infra.token;

import com.custacm.platform.auth.core.PlatformJwtDecoders;
import com.custacm.platform.auth.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RsaJwtAccessTokenIssuerTest {
    @Test
    void issuesRs256JwtWithSubjectRoleAndExpiry() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        Clock clock = Clock.fixed(Instant.parse("2030-07-04T12:00:00Z"), ZoneOffset.UTC);
        RsaJwtAccessTokenIssuer issuer = new RsaJwtAccessTokenIssuer(
                (RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate(),
                Duration.ofHours(2),
                clock
        );

        var issued = issuer.issue("230511213黄炳睿", UserRole.PLAYER);
        Jwt jwt = PlatformJwtDecoders.rsa((RSAPublicKey) keyPair.getPublic()).decode(issued.tokenValue());

        assertThat(issued.expiresInSeconds()).isEqualTo(7200);
        assertThat(jwt.getSubject()).isEqualTo("230511213黄炳睿");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("player");
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2030-07-04T14:00:00Z"));
    }

    @Test
    void issuesTokenWithRequestedTtl() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        Clock clock = Clock.fixed(Instant.parse("2030-07-04T12:00:00Z"), ZoneOffset.UTC);
        RsaJwtAccessTokenIssuer issuer = new RsaJwtAccessTokenIssuer(
                (RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate(),
                Duration.ofHours(2),
                clock
        );

        var issued = issuer.issue("230511213黄炳睿", UserRole.PLAYER, Duration.ofDays(30));
        Jwt jwt = PlatformJwtDecoders.rsa((RSAPublicKey) keyPair.getPublic()).decode(issued.tokenValue());

        assertThat(issued.expiresInSeconds()).isEqualTo(Duration.ofDays(30).toSeconds());
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2030-08-03T12:00:00Z"));
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
