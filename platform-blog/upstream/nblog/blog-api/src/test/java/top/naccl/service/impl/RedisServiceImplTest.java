package top.naccl.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ExpirationOptions;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.naccl.entity.Category;
import top.naccl.entity.Tag;
import top.naccl.model.vo.BlogInfo;
import top.naccl.model.vo.PageResult;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Author: huangbingrui.awa
class RedisServiceImplTest {
    private final RedisTemplate<Object, Object> redisTemplate = mock(RedisTemplate.class);
    private final HashOperations<Object, Object, Object> hashOperations = mock(HashOperations.class);
    private final ValueOperations<Object, Object> valueOperations = mock(ValueOperations.class);
    private final Duration ttl = Duration.ofMinutes(10);
    private final RedisServiceImpl service = new RedisServiceImpl(redisTemplate, ttl);

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void readsHashWithOneRedisCommand() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("home", 1)).thenReturn(null);

        assertNull(service.getBlogInfoPageResultByHash("home", 1));

        verify(hashOperations).get("home", 1);
        verify(hashOperations, never()).hasKey("home", 1);
    }

    @Test
    void appliesTtlToValueAndHashCaches() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        service.saveListToValue("categories", List.of("algorithm"));
        service.saveKVToHash("home", 1, "page");

        verify(valueOperations).set("categories", List.of("algorithm"), ttl);
        verify(hashOperations).put("home", 1, "page");
        verify(redisTemplate).expire(eq("home"),
                argThat(expiration -> expiration.getExpirationTimeInMilliseconds() == ttl.toMillis()),
                eq(ExpirationOptions.builder().nx().build()));
        verify(redisTemplate, never()).expire("home", ttl);
    }

    @Test
    void restoresTypedArticlesAndNestedTaxonomyAfterARealJsonRoundTrip() {
        BlogInfo article = new BlogInfo();
        article.setId(42L);
        article.setCreateTime(new Date(1_700_000_000_000L));
        Category category = new Category();
        category.setName("题解");
        article.setCategory(category);
        Tag tag = new Tag();
        tag.setName("动态规划");
        article.setTags(List.of(tag));
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        Object cached = serializer.deserialize(serializer.serialize(new PageResult<>(3, List.of(article))));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("home", 1)).thenReturn(cached);

        PageResult<BlogInfo> result = service.getBlogInfoPageResultByHash("home", 1);

        assertEquals(3, result.getTotalPage());
        assertEquals(42L, result.getList().getFirst().getId());
        assertEquals(article.getCreateTime(), result.getList().getFirst().getCreateTime());
        assertEquals("题解", result.getList().getFirst().getCategory().getName());
        assertEquals("动态规划", result.getList().getFirst().getTags().getFirst().getName());
    }

    @Test
    void malformedPageDataDegradesToACacheMiss() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("home", 1)).thenReturn("invalid page");
        when(hashOperations.get("home", 2)).thenReturn(Map.of("totalPage", 3));

        assertNull(service.getBlogInfoPageResultByHash("home", 1));
        assertNull(service.getBlogInfoPageResultByHash("home", 2));
    }

    @Test
    void wrongValueShapesDegradeToACacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("categories")).thenReturn(Map.of("unexpected", true));
        when(valueOperations.get("site")).thenReturn(List.of("unexpected"));

        assertNull(service.getListByValue("categories"));
        assertNull(service.getMapByValue("site"));
    }

    @Test
    void invalidatesOnlyAfterTheDatabaseTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        service.deleteCacheByKey("categories");

        verify(redisTemplate, never()).delete("categories");
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(redisTemplate).delete("categories");
    }

    @Test
    void degradesToCacheMissWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));

        assertNull(service.getListByValue("categories"));
    }
}
