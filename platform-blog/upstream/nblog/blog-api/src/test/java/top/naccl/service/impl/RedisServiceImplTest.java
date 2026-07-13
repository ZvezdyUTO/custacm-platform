package top.naccl.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
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
        verify(redisTemplate).expire("home", ttl);
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
