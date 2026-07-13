package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import top.naccl.exception.LoginCooldownException;
import top.naccl.exception.LoginCooldownUnavailableException;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author huangbingrui.awa
 */
class LoginAttemptLimiterTest {
	private final RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);
	private final ValueOperations<Object, Object> valueOperations = mock(ValueOperations.class);
	private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(redisTemplate);

	@Test
	void acquiresOneFiveSecondWindowWithoutExposingTheUsername() {
		String key = LoginAttemptLimiter.cooldownKey("Player1");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(key, 1, LoginAttemptLimiter.COOLDOWN)).thenReturn(true);

		assertDoesNotThrow(() -> limiter.acquire("Player1"));

		verify(valueOperations).setIfAbsent(key, 1, LoginAttemptLimiter.COOLDOWN);
		assertEquals(LoginAttemptLimiter.cooldownKey(" player1 "), key);
	}

	@Test
	void rejectsARepeatedAttemptWithTheRemainingCooldown() {
		String key = LoginAttemptLimiter.cooldownKey("player1");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(key, 1, LoginAttemptLimiter.COOLDOWN)).thenReturn(false);
		when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(4L);

		LoginCooldownException exception = assertThrows(
				LoginCooldownException.class, () -> limiter.acquire("player1"));

		assertEquals(4, exception.retryAfterSeconds());
	}

	@Test
	void releasesTheReservationAfterCorrectCredentials() {
		String key = LoginAttemptLimiter.cooldownKey("player1");

		limiter.release("player1");

		verify(redisTemplate).delete(key);
	}

	@Test
	void failsClosedWhenRedisCannotCheckTheCooldown() {
		when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));

		assertThrows(LoginCooldownUnavailableException.class, () -> limiter.acquire("player1"));
	}
}
