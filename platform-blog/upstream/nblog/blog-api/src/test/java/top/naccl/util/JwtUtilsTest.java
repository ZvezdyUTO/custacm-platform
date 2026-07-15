package top.naccl.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {
	private static final String VALID_SECRET =
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ab";
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(JwtTestConfiguration.class)
			.withPropertyValues("token.expireTime=60000");

	@Test
	void keepsExistingUsernameAndAuthoritiesTokenShape() {
		JwtUtils jwtUtils = new JwtUtils(VALID_SECRET, 60_000L);
		String token = jwtUtils.generateToken("player1", AuthorityUtils.createAuthorityList("ROLE_player"));
		Claims claims = jwtUtils.getTokenBody(token);

		assertEquals("player1", claims.getSubject());
		assertEquals("ROLE_player,", claims.get("authorities"));
		assertNotNull(claims.getExpiration());
		assertEquals("HS512", Jwts.parser().verifyWith(
				Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8))
		).build().parseSignedClaims(token).getHeader().getAlgorithm());
	}

	@Test
	void missingSecretPreventsContextStartup() {
		contextRunner.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseInstanceOf(IllegalStateException.class)
					.hasRootCauseMessage("token.secretKey must be configured");
		});
	}

	@Test
	void placeholderSecretPreventsContextStartup() {
		contextRunner.withPropertyValues(
				"token.secretKey=change-me-with-at-least-sixty-four-characters-for-hs512-signing-key")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(IllegalStateException.class)
							.hasRootCauseMessage("token.secretKey must not use a placeholder value");
				});
	}

	@Test
	void weakSecretPreventsContextStartup() {
		contextRunner.withPropertyValues("token.secretKey=too-short")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseInstanceOf(IllegalStateException.class)
							.hasRootCauseMessage(
									"token.secretKey must contain at least 64 UTF-8 bytes for HS512");
				});
	}

	@Test
	void validSecretStartsJwtComponent() {
		contextRunner.withPropertyValues("token.secretKey=" + VALID_SECRET)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(JwtUtils.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	@Import(JwtUtils.class)
	static class JwtTestConfiguration {
	}
}
