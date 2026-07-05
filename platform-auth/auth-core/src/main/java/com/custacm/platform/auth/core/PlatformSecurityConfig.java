package com.custacm.platform.auth.core;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PlatformSecurityConfig {
    public static final int PROTECTED_CHAIN_ORDER = 1;
    public static final int GUEST_CHAIN_ORDER = 2;

    private PlatformSecurityConfig() {
    }

    public static JwtConfig statelessJwt(HttpSecurity http, JwtDecoder jwtDecoder) {
        return new JwtConfig(http, jwtDecoder);
    }

    public static SecurityFilterChain guest(HttpSecurity http) throws Exception {
        return commonStateless(http)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    private static HttpSecurity commonStateless(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
    }

    public static final class JwtConfig {
        private final HttpSecurity http;
        private final JwtDecoder jwtDecoder;
        private final List<String> adminPaths = new ArrayList<>();
        private final List<String> playerPaths = new ArrayList<>();

        private JwtConfig(HttpSecurity http, JwtDecoder jwtDecoder) {
            this.http = http;
            this.jwtDecoder = jwtDecoder;
        }

        public JwtConfig admin(String... paths) {
            addPaths(adminPaths, paths);
            return this;
        }

        public JwtConfig player(String... paths) {
            addPaths(playerPaths, paths);
            return this;
        }

        public SecurityFilterChain build() throws Exception {
            String[] protectedPaths = protectedPaths();
            if (protectedPaths.length == 0) {
                throw new IllegalStateException("at least one platform protected path is required");
            }
            return commonStateless(http)
                    .securityMatcher(protectedPaths)
                    .authorizeHttpRequests(authorize -> {
                        for (String adminPath : adminPaths) {
                            authorize.requestMatchers(adminPath).hasRole(PlatformRoles.ADMIN);
                        }
                        for (String playerPath : playerPaths) {
                            authorize.requestMatchers(playerPath).hasRole(PlatformRoles.PLAYER);
                        }
                        authorize.anyRequest().denyAll();
                    })
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                            .decoder(jwtDecoder)
                            .jwtAuthenticationConverter(
                                    PlatformJwtAuthenticationConverters.jwtAuthenticationConverter()
                            )))
                    .build();
        }

        private String[] protectedPaths() {
            List<String> paths = new ArrayList<>(adminPaths.size() + playerPaths.size());
            paths.addAll(adminPaths);
            paths.addAll(playerPaths);
            return paths.stream().distinct().toArray(String[]::new);
        }

        private static void addPaths(List<String> target, String... paths) {
            if (paths == null) {
                return;
            }
            Arrays.stream(paths)
                    .filter(path -> path != null && !path.isBlank())
                    .forEach(target::add);
        }
    }
}
