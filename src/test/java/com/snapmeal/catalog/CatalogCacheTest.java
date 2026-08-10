package com.snapmeal.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogCacheTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void memoryModeLoadsOnceThenServesFromCache() {
        CatalogCache cache = new CatalogCache("memory", provider(null), mapper);
        AtomicInteger loads = new AtomicInteger();
        List<Map<String, Object>> value = rows(2);

        assertEquals(2, cache.get("k", loader(loads, value)).size());
        assertEquals(2, cache.get("k", loader(loads, value)).size());
        assertEquals(1, loads.get(), "第二次应命中缓存，不再加载");
    }

    @Test
    void evictByPrefixInvalidatesOnlyMatchingKeys() {
        CatalogCache cache = new CatalogCache("memory", provider(null), mapper);
        AtomicInteger loadsA = new AtomicInteger();
        AtomicInteger loadsB = new AtomicInteger();
        List<Map<String, Object>> a = rows(1);
        List<Map<String, Object>> b = rows(3);

        cache.get("snap-meal:catalog:dish:1", loader(loadsA, a));
        cache.get("snap-meal:catalog:dish:1", loader(loadsA, a));
        cache.get("other:key", loader(loadsB, b));
        cache.get("other:key", loader(loadsB, b));

        cache.evictByPrefix("snap-meal:catalog:dish:");

        cache.get("snap-meal:catalog:dish:1", loader(loadsA, a));
        cache.get("other:key", loader(loadsB, b));
        assertEquals(2, loadsA.get(), "失效后应重新加载");
        assertEquals(1, loadsB.get(), "其他前缀不应受影响");
    }

    @Test
    void redisModeStoresAndReadsThroughRedis() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("k")).thenReturn(null);

        CatalogCache cache = new CatalogCache("redis", provider(redis), mapper);
        cache.get("k", () -> rows(1));
        verify(ops).set(eq("k"), anyString(), any(Duration.class));

        when(ops.get("k")).thenReturn("[{\"id\":99}]");
        List<Map<String, Object>> cached = cache.get("k", () -> {
            throw new AssertionError("Redis 命中不应再走 loader");
        });
        assertEquals(99, ((Number) cached.get(0).get("id")).intValue());
    }

    @Test
    void redisModeEvictScansAndDeletesByPrefix() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));

        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("snap-meal:catalog:dish:1", "snap-meal:catalog:dish:all");
        when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);

        CatalogCache cache = new CatalogCache("redis", provider(redis), mapper);
        cache.evictByPrefix("snap-meal:catalog:dish:");
        verify(redis).scan(any(ScanOptions.class));
        verify(redis).delete(Arrays.asList("snap-meal:catalog:dish:1", "snap-meal:catalog:dish:all"));
    }

    private static List<Map<String, Object>> rows(int n) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", i);
            m.put("name", "菜品" + i);
            list.add(m);
        }
        return list;
    }

    private static Supplier<List<Map<String, Object>>> loader(AtomicInteger counter, List<Map<String, Object>> value) {
        return () -> {
            counter.incrementAndGet();
            return value;
        };
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<RedisTemplate<String, String>> provider(RedisTemplate<String, String> value) {
        ObjectProvider<RedisTemplate<String, String>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
