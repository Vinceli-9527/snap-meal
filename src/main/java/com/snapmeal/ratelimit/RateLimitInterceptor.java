package com.snapmeal.ratelimit;

import com.snapmeal.auth.AuthInterceptor;
import com.snapmeal.common.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌桶限流（Bucket4j）：保护下单接口，按用户隔离，本地内存实现。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final long IDLE_TIMEOUT_MILLIS = 10 * 60 * 1000;
    private static final int MAX_BUCKETS = 10_000;

    private final Bandwidth orderBandwidth;
    private final ConcurrentHashMap<String, Bucketed> buckets = new ConcurrentHashMap<>();

    public RateLimitInterceptor(@Value("${sky.rate-limit.orders-per-minute:10}") int ordersPerMinute) {
        this.orderBandwidth = Bandwidth.classic(ordersPerMinute, Refill.greedy(ordersPerMinute, Duration.ofMinutes(1)));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        Bucketed entry = bucket(key(request));
        if (!entry.bucket.tryConsume(1)) throw new TooManyRequestsException("下单过于频繁，请稍后再试");
        return true;
    }

    private Bucketed bucket(String key) {
        long now = System.currentTimeMillis();
        if (buckets.size() > MAX_BUCKETS) evictIdle(now);
        Bucketed entry = buckets.computeIfAbsent(key, k -> new Bucketed(Bucket4j.builder().addLimit(orderBandwidth).build(), now));
        entry.lastAccess = now;
        return entry;
    }

    private void evictIdle(long now) {
        buckets.entrySet().removeIf(e -> now - e.getValue().lastAccess > IDLE_TIMEOUT_MILLIS);
    }

    private String key(HttpServletRequest request) {
        Object uid = request.getAttribute(AuthInterceptor.SUBJECT_ID);
        return uid != null ? "user:" + uid : "ip:" + request.getRemoteAddr();
    }

    private static class Bucketed {
        final Bucket bucket;
        volatile long lastAccess;

        Bucketed(Bucket bucket, long lastAccess) {
            this.bucket = bucket;
            this.lastAccess = lastAccess;
        }
    }
}
