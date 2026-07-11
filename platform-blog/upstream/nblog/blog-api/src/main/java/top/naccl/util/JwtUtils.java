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

/**
 * @Description: JWT工具类
 * @Author: Naccl
 * @Date: 2020-09-02
 */
@Component
public class JwtUtils {
	private static long expireTime;
	private static String secretKey;

	@Value("${token.secretKey}")
	public void setSecretKey(String secretKey) {
		JwtUtils.secretKey = secretKey;
	}

	@Value("${token.expireTime}")
	public void setExpireTime(long expireTime) {
		JwtUtils.expireTime = expireTime;
	}

	/**
	 * 判断token是否存在
	 *
	 * @param token
	 * @return
	 */
	public static boolean judgeTokenIsExist(String token) {
		return token != null && !"".equals(token) && !"null".equals(token);
	}

	/**
	 * 生成token
	 *
	 * @param subject
	 * @return
	 */
	public static String generateToken(String subject) {
		String jwt = Jwts.builder()
				.subject(subject)
				.expiration(new Date(System.currentTimeMillis() + expireTime))
				.signWith(getSigningKey(), Jwts.SIG.HS512)
				.compact();
		return jwt;
	}

	/**
	 * 生成带角色权限的token
	 *
	 * @param subject
	 * @param authorities
	 * @return
	 */
	public static String generateToken(String subject, Collection<? extends GrantedAuthority> authorities) {
		StringBuilder sb = new StringBuilder();
		for (GrantedAuthority authority : authorities) {
			sb.append(authority.getAuthority()).append(",");
		}
		String jwt = Jwts.builder()
				.subject(subject)
				.claim("authorities", sb.toString())
				.expiration(new Date(System.currentTimeMillis() + expireTime))
				.signWith(getSigningKey(), Jwts.SIG.HS512)
				.compact();
		return jwt;
	}

	/**
	 * 生成自定义过期时间token
	 *
	 * @param subject
	 * @param expireTime
	 * @return
	 */
	public static String generateToken(String subject, long expireTime) {
		String jwt = Jwts.builder()
				.subject(subject)
				.expiration(new Date(System.currentTimeMillis() + expireTime))
				.signWith(getSigningKey(), Jwts.SIG.HS512)
				.compact();
		return jwt;
	}


	/**
	 * 获取tokenBody同时校验token是否有效（无效则会抛出异常）
	 *
	 * @param token
	 * @return
	 */
	public static Claims getTokenBody(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token.replace("Bearer", "").trim())
				.getPayload();
		return claims;
	}

	private static SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}
}
