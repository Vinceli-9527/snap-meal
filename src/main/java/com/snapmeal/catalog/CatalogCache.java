package com.snapmeal.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 菜品列表缓存：Redis 优先（redis-mode=redis），未启用/连不上时本地内存兜底（带 TTL）。
 */
@Component
public class CatalogCache {
    private static final long TTL_MILLIS = 5 * 60 * 1000;

    private final RedisTemplate<String, String> redis;
    private final boolean redisMode;
    private final ObjectMapper mapper;
    private final ConcurrentHashMap<String, Entry> memory = new ConcurrentHashMap<>();

    public CatalogCache(@Value("${sky.integrations.redis-mode:memory}") String redisMode,
                        ObjectProvider<RedisTemplate<String, String>> redis,
                        ObjectMapper mapper) {
        this.redisMode = "redis".equalsIgnoreCase(redisMode);
        this.redis = redis.getIfAvailable();
        this.mapper = mapper;
    }

    public List<Map<String, Object>> get(String key, Supplier<List<Map<String, Object>>> loader) {
        String json = read(key);
        if (json != null) return parse(json);
        List<Map<String, Object>> value = loader.get();
        if (value != null) store(key, value);
        return value;
    }

    public void evictByPrefix(String prefix) {
        memory.keySet().removeIf(k -> k.startsWith(prefix));
        if (!canUseRedis()) return;
        try {
            List<String> keys = new ArrayList<>();
            try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(prefix + "*").count(200).build())) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) redis.delete(keys);
        } catch (Exception ignored) {
            // Redis 不可用时仅失效本地缓存，TTL 兜底过期
        }
    }

    private String read(String key) {
        if (canUseRedis()) {
            try {
                String v = redis.opsForValue().get(key);
                if (v != null && !v.isEmpty()) return v;
            } catch (Exception ignored) {
            }
        }
        Entry e = memory.get(key);
        if (e != null && e.expiresAt > System.currentTimeMillis()) return e.json;
        memory.remove(key);
        return null;
    }

    private void store(String key, List<Map<String, Object>> value) {
        String json;
        try {
            json = mapper.writeValueAsString(value);
        } catch (Exception e) {
            return;
        }
        memory.put(key, new Entry(json, System.currentTimeMillis() + TTL_MILLIS));
        if (!canUseRedis()) return;
        try {
            redis.opsForValue().set(key, json, Duration.ofMillis(TTL_MILLIS));
        } catch (Exception ignored) {
        }
    }

    private List<Map<String, Object>> parse(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private boolean canUseRedis() {
        return redisMode && redis != null;
    }

    private static class Entry {
        final String json;
        final long expiresAt;

        Entry(String json, long expiresAt) {
            this.json = json;
            this.expiresAt = expiresAt;
        }
    }
}
