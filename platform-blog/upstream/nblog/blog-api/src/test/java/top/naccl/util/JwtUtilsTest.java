package top.naccl.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtUtilsTest {
	@Test
	void keepsExistingUsernameAndAuthoritiesTokenShape() {
		JwtUtils jwtUtils = new JwtUtils();
		jwtUtils.setSecretKey("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ab");
		jwtUtils.setExpireTime(60_000L);
		String token = JwtUtils.generateToken("player1", AuthorityUtils.createAuthorityList("ROLE_player"));
		Claims claims = JwtUtils.getTokenBody(token);

		assertEquals("player1", claims.getSubject());
		assertEquals("ROLE_player,", claims.get("authorities"));
		assertNotNull(claims.getExpiration());
		assertEquals("HS512", Jwts.parser().verifyWith(
				Keys.hmacShaKeyFor("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ab".getBytes(StandardCharsets.UTF_8))
		).build().parseSignedClaims(token).getHeader().getAlgorithm());
	}
}
