package top.naccl.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.exception.LoginCooldownException;
import top.naccl.exception.LoginCooldownUnavailableException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 使用 Redis 原子占位为失败登录保留五秒冷却窗口。
 *
 * @author huangbingrui.awa
 */
@Service
public class LoginAttemptLimiter {
	public static final Duration COOLDOWN = Duration.ofSeconds(5);

	private final RedisTemplate<Object, Object> redisTemplate;

	public LoginAttemptLimiter(
			@Qualifier("jsonRedisTemplate") RedisTemplate<Object, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * 登录校验前原子占位。校验失败时保留 key，成功时由调用方释放。
	 */
	public void acquire(String username) {
		String key = cooldownKey(username);
		try {
			Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, 1, COOLDOWN);
			if (acquired == null) {
				throw new LoginCooldownUnavailableException(
						new IllegalStateException("Redis did not return a login cooldown result"));
			}
			if (!acquired) {
				Long remaining = redisTemplate.getExpire(key, TimeUnit.SECONDS);
				throw new LoginCooldownException(normalizeRemainingSeconds(remaining));
			}
		} catch (LoginCooldownException | LoginCooldownUnavailableException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new LoginCooldownUnavailableException(e);
		}
	}

	/**
	 * 正确凭据不应留下冷却窗口。
	 */
	public void release(String username) {
		try {
			redisTemplate.delete(cooldownKey(username));
		} catch (RuntimeException e) {
			throw new LoginCooldownUnavailableException(e);
		}
	}

	static String cooldownKey(String username) {
		String normalized = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(normalized.getBytes(StandardCharsets.UTF_8));
			return RedisKeyConstants.LOGIN_ATTEMPT_COOLDOWN + HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}

	private static int normalizeRemainingSeconds(Long remaining) {
		if (remaining == null || remaining < 1) {
			return 1;
		}
		return (int) Math.min(COOLDOWN.toSeconds(), remaining);
	}
}
