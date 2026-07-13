package top.naccl.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.exception.ArticleDownloadRateLimitException;
import top.naccl.exception.ArticleDownloadRateLimitUnavailableException;

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
class ArticleDownloadRateLimiterTest {
	private final RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);
	private final ValueOperations<Object, Object> valueOperations = mock(ValueOperations.class);
	private final ArticleDownloadRateLimiter rateLimiter = new ArticleDownloadRateLimiter(redisTemplate);

	@Test
	void acquiresOneThirtySecondWindowAtomically() {
		String key = RedisKeyConstants.ARTICLE_DOWNLOAD_RATE_LIMIT + "player1";
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(key, 1, ArticleDownloadRateLimiter.WINDOW)).thenReturn(true);

		assertDoesNotThrow(() -> rateLimiter.acquire("player1"));

		verify(valueOperations).setIfAbsent(key, 1, ArticleDownloadRateLimiter.WINDOW);
	}

	@Test
	void returnsTheRemainingCooldownForARepeatedDownload() {
		String key = RedisKeyConstants.ARTICLE_DOWNLOAD_RATE_LIMIT + "player1";
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(key, 1, ArticleDownloadRateLimiter.WINDOW)).thenReturn(false);
		when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(17L);

		ArticleDownloadRateLimitException exception = assertThrows(
				ArticleDownloadRateLimitException.class, () -> rateLimiter.acquire("player1"));

		assertEquals(17, exception.retryAfterSeconds());
	}

	@Test
	void failsClosedWhenRedisCannotCheckTheLimit() {
		when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));

		assertThrows(ArticleDownloadRateLimitUnavailableException.class,
				() -> rateLimiter.acquire("player1"));
	}
}
