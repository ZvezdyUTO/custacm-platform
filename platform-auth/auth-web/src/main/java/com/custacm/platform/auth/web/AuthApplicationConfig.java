package com.custacm.platform.auth.web;

import com.custacm.platform.auth.app.port.AccessTokenIssuer;
import com.custacm.platform.auth.app.port.PasswordHasher;
import com.custacm.platform.auth.app.service.AdminUserService;
import com.custacm.platform.auth.app.service.AuthAccountService;
import com.custacm.platform.auth.core.PemRsaKeys;
import com.custacm.platform.auth.core.PlatformJwtDecoders;
import com.custacm.platform.auth.domain.model.UserRole;
import com.custacm.platform.auth.domain.repo.UserAccountRepository;
import com.custacm.platform.auth.infra.jdbc.JdbcUserAccountRepository;
import com.custacm.platform.auth.infra.password.BCryptPasswordHasher;
import com.custacm.platform.auth.infra.token.RsaJwtAccessTokenIssuer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthApplicationConfig {
    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    UserAccountRepository userAccountRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new JdbcUserAccountRepository(jdbcTemplate);
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    RSAPrivateKey authJwtPrivateKey(AuthProperties properties) {
        AuthProperties.Jwt jwt = properties.jwt();
        return PemRsaKeys.privateKey(jwt.privateKey(), jwt.privateKeyPath());
    }

    @Bean
    RSAPublicKey authJwtPublicKey(AuthProperties properties) {
        AuthProperties.Jwt jwt = properties.jwt();
        return PemRsaKeys.publicKey(jwt.publicKey(), jwt.publicKeyPath());
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey authJwtPublicKey) {
        return PlatformJwtDecoders.rsa(authJwtPublicKey);
    }

    @Bean
    AccessTokenIssuer accessTokenIssuer(
            RSAPublicKey authJwtPublicKey,
            RSAPrivateKey authJwtPrivateKey,
            AuthProperties properties,
            Clock authClock
    ) {
        return new RsaJwtAccessTokenIssuer(
                authJwtPublicKey,
                authJwtPrivateKey,
                properties.jwt().resolvedAccessTokenTtl(),
                authClock
        );
    }

    @Bean
    AuthAccountService authAccountService(
            UserAccountRepository userAccountRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            Clock authClock
    ) {
        return new AuthAccountService(
                userAccountRepository,
                passwordHasher,
                accessTokenIssuer,
                authClock
        );
    }

    @Bean
    AdminUserService adminUserService(
            UserAccountRepository userAccountRepository,
            PasswordHasher passwordHasher,
            Clock authClock
    ) {
        return new AdminUserService(userAccountRepository, passwordHasher, authClock);
    }

    @Bean
    ApplicationRunner bootstrapAdmin(AuthProperties properties, UserAccountRepository userAccounts, AdminUserService adminUsers) {
        return args -> {
            AuthProperties.BootstrapAdmin bootstrap = properties.bootstrapAdmin();
            if (bootstrap == null || !bootstrap.configured()) {
                return;
            }
            if (!bootstrap.complete()) {
                throw new IllegalStateException("bootstrap admin student identity and password must be configured together");
            }
            if (userAccounts.findByStudentIdentity(bootstrap.studentIdentity().trim()).isEmpty()) {
                adminUsers.createUser(bootstrap.studentIdentity(), bootstrap.password(), UserRole.ADMIN);
            }
        };
    }
}
