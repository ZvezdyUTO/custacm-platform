package top.naccl.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.naccl.constant.RedisKeyConstants;
import top.naccl.exception.ArticleDownloadRateLimitException;
import top.naccl.exception.ArticleDownloadRateLimitUnavailableException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 使用 Redis 原子占位限制普通用户的文章下载频率。
 *
 * @author huangbingrui.awa
 */
@Service
public class ArticleDownloadRateLimiter {
	static final Duration WINDOW = Duration.ofSeconds(30);

	private final RedisTemplate<Object, Object> redisTemplate;

	public ArticleDownloadRateLimiter(
			@Qualifier("jsonRedisTemplate") RedisTemplate<Object, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void acquire(String username) {
		String key = RedisKeyConstants.ARTICLE_DOWNLOAD_RATE_LIMIT + username;
		try {
			Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, 1, WINDOW);
			if (acquired == null) {
				throw new ArticleDownloadRateLimitUnavailableException(
						new IllegalStateException("Redis did not return a rate-limit result"));
			}
			if (!acquired) {
				Long remaining = redisTemplate.getExpire(key, TimeUnit.SECONDS);
				throw new ArticleDownloadRateLimitException(normalizeRemainingSeconds(remaining));
			}
		} catch (ArticleDownloadRateLimitException | ArticleDownloadRateLimitUnavailableException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ArticleDownloadRateLimitUnavailableException(e);
		}
	}

	private static int normalizeRemainingSeconds(Long remaining) {
		if (remaining == null || remaining < 1) {
			return 1;
		}
		return (int) Math.min(WINDOW.toSeconds(), remaining);
	}
}
