package com.custacm.platform.auth.core;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

public class PlatformJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String role = PlatformRoles.requireTokenRole((String) jwt.getClaims().get("role"));
        if (PlatformRoles.ADMIN.equals(role)) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_" + PlatformRoles.ADMIN),
                    new SimpleGrantedAuthority("ROLE_" + PlatformRoles.PLAYER)
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
