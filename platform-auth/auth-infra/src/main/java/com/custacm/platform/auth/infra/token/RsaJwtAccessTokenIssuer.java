package com.custacm.platform.auth.infra.token;

import com.custacm.platform.auth.app.port.AccessTokenIssuer;
import com.custacm.platform.auth.app.result.IssuedToken;
import com.custacm.platform.auth.domain.model.UserRole;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class RsaJwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtEncoder jwtEncoder;
    private final Duration ttl;
    private final Clock clock;

    public RsaJwtAccessTokenIssuer(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey,
            Duration ttl,
            Clock clock
    ) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(String studentIdentity, UserRole role) {
        return issue(studentIdentity, role, ttl);
    }

    @Override
    public IssuedToken issue(String studentIdentity, UserRole role, Duration requestedTtl) {
        Duration effectiveTtl = requestedTtl == null ? ttl : requestedTtl;
        if (effectiveTtl.isZero() || effectiveTtl.isNegative()) {
            throw new IllegalArgumentException("access token ttl must be positive");
        }
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(effectiveTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(studentIdentity)
                .claim("role", role.value())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, effectiveTtl.toSeconds());
    }
}
