package com.teraapi.identity.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j token bucket algorithm.
 * 
 * Provides different rate limits for different endpoint types:
 * - Authentication endpoints: 5 requests per minute (prevent brute force)
 * - API endpoints: 100 requests per minute (normal usage)
 * - Admin endpoints: 50 requests per minute
 */
@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Get or create bucket for authentication endpoints
     * Limit: 5 requests per minute
     */
    public Bucket resolveAuthBucket(String key) {
        return cache.computeIfAbsent(key, k -> createAuthBucket());
    }

    /**
     * Get or create bucket for API endpoints
     * Limit: 100 requests per minute
     */
    public Bucket resolveApiBucket(String key) {
        return cache.computeIfAbsent(key, k -> createApiBucket());
    }

    /**
     * Get or create bucket for admin endpoints
     * Limit: 50 requests per minute
     */
    public Bucket resolveAdminBucket(String key) {
        return cache.computeIfAbsent(key, k -> createAdminBucket());
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createAdminBucket() {
        Bandwidth limit = Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
