package top.naccl.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * @Description: JWT工具类
 * @Author: Naccl
 * @Date: 2020-09-02
 */
@Component
public class JwtUtils {
	private static final int HS512_MINIMUM_KEY_BYTES = 64;
	private static final String SECRET_PLACEHOLDER_MARKER = "change-me";

	private final long expireTime;
	private final SecretKey signingKey;

	public JwtUtils(
			@Value("${token.secretKey:}") String secretKey,
			@Value("${token.expireTime}") long expireTime) {
		this.signingKey = validateAndCreateSigningKey(secretKey);
		if (expireTime <= 0) {
			throw new IllegalStateException("token.expireTime must be greater than zero");
		}
		this.expireTime = expireTime;
	}

	/**
	 * 生成token
	 *
	 * @param subject
	 * @return
	 */
	public String generateToken(String subject) {
		return Jwts.builder()
				.subject(subject)
				.expiration(new Date(System.currentTimeMillis() + expireTime))
				.signWith(signingKey, Jwts.SIG.HS512)
				.compact();
	}

	/**
	 * 生成带角色权限的token
	 *
	 * @param subject
	 * @param authorities
	 * @return
	 */
	public String generateToken(String subject, Collection<? extends GrantedAuthority> authorities) {
		StringBuilder sb = new StringBuilder();
		for (GrantedAuthority authority : authorities) {
			sb.append(authority.getAuthority()).append(",");
		}
		return Jwts.builder()
				.subject(subject)
				.claim("authorities", sb.toString())
				.expiration(new Date(System.currentTimeMillis() + expireTime))
				.signWith(signingKey, Jwts.SIG.HS512)
				.compact();
	}

	/**
	 * 生成自定义过期时间token
	 *
	 * @param subject
	 * @param expireTime
	 * @return
	 */
	public String generateToken(String subject, long expireTime) {
		return Jwts.builder()
				.subject(subject)
				.expiration(new Date(System.currentTimeMillis() + expireTime))
				.signWith(signingKey, Jwts.SIG.HS512)
				.compact();
	}


	/**
	 * 获取tokenBody同时校验token是否有效（无效则会抛出异常）
	 *
	 * @param token
	 * @return
	 */
	public Claims getTokenBody(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private static SecretKey validateAndCreateSigningKey(String secretKey) {
		if (secretKey == null || secretKey.isBlank()) {
			throw new IllegalStateException("token.secretKey must be configured");
		}
		if (secretKey.toLowerCase(Locale.ROOT).contains(SECRET_PLACEHOLDER_MARKER)) {
			throw new IllegalStateException("token.secretKey must not use a placeholder value");
		}
		byte[] secretBytes = secretKey.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < HS512_MINIMUM_KEY_BYTES) {
			throw new IllegalStateException("token.secretKey must contain at least 64 UTF-8 bytes for HS512");
		}
		return Keys.hmacShaKeyFor(secretBytes);
	}
}
