package com.custacm.platform.auth.core;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public final class KeycloakJwtAuthenticationConverters {
    private KeycloakJwtAuthenticationConverters() {
    }

    public static Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("student_identity");
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtAuthoritiesConverter());
        return converter;
    }
}
