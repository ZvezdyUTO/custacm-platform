package top.naccl.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ExpirationOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.naccl.model.vo.BlogInfo;
import top.naccl.model.vo.PageResult;
import top.naccl.service.RedisService;
import top.naccl.util.JacksonUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @Description: 读写Redis相关操作
 * @Author: Naccl
 * @Date: 2020-09-27
 */
@Service
public class RedisServiceImpl implements RedisService {
	private static final Logger log = LoggerFactory.getLogger(RedisServiceImpl.class);
	private static final TypeReference<PageResult<BlogInfo>> BLOG_PAGE_TYPE = new TypeReference<>() {};
	private static final ExpirationOptions INITIAL_EXPIRATION = ExpirationOptions.builder().nx().build();

	private final RedisTemplate<Object, Object> redisTemplate;
	private final Duration ttl;

	public RedisServiceImpl(
			@Qualifier("jsonRedisTemplate") RedisTemplate<Object, Object> redisTemplate,
			@Value("${blog.cache.ttl:10m}") Duration ttl
	) {
		this.redisTemplate = redisTemplate;
		this.ttl = ttl;
	}

	@Override
	public PageResult<BlogInfo> getBlogInfoPageResultByHash(String hash, Integer pageNum) {
		return read(() -> {
			Object redisResult = redisTemplate.opsForHash().get(hash, pageNum);
			if (redisResult == null) {
				return null;
			}
			PageResult<BlogInfo> page = JacksonUtils.convertValue(redisResult, BLOG_PAGE_TYPE);
			if (page.getTotalPage() == null || page.getTotalPage() < 0 || page.getList() == null) {
				throw new IllegalArgumentException("Invalid cached blog page");
			}
			return page;
		});
	}

	@Override
	public void saveKVToHash(String hash, Object key, Object value) {
		write(() -> {
			redisTemplate.opsForHash().put(hash, key, value);
			//只给新 hash 设置过期时间，其他分页写入不能持续延长老页缓存寿命。
			redisTemplate.expire(hash, Expiration.from(ttl), INITIAL_EXPIRATION);
		});
	}

	@Override
	public <T> List<T> getListByValue(String key) {
		return read(() -> (List<T>) redisTemplate.opsForValue().get(key));
	}

	@Override
	public <T> void saveListToValue(String key, List<T> list) {
		write(() -> redisTemplate.opsForValue().set(key, list, ttl));
	}

	@Override
	public <T> Map<String, T> getMapByValue(String key) {
		return read(() -> (Map<String, T>) redisTemplate.opsForValue().get(key));
	}

	@Override
	public <T> void saveMapToValue(String key, Map<String, T> map) {
		write(() -> redisTemplate.opsForValue().set(key, map, ttl));
	}

	@Override
	public void deleteCacheByKey(String key) {
		if (TransactionSynchronizationManager.isActualTransactionActive()
				&& TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					deleteNow(key);
				}
			});
			return;
		}
		deleteNow(key);
	}

	private void deleteNow(String key) {
		write(() -> redisTemplate.delete(key));
	}

	private <T> T read(CacheRead<T> operation) {
		try {
			return operation.get();
		} catch (RuntimeException ex) {
			log.error("Cache read failed, errorCode=CACHE_READ_FAILED", ex);
			return null;
		}
	}

	private void write(Runnable operation) {
		try {
			operation.run();
		} catch (RuntimeException ex) {
			log.error("Cache write failed, errorCode=CACHE_WRITE_FAILED", ex);
		}
	}

	@FunctionalInterface
	private interface CacheRead<T> {
		T get();
	}

}
