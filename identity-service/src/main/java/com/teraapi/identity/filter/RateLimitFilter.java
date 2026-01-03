package com.teraapi.identity.filter;

import com.teraapi.identity.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that enforces rate limiting on HTTP requests.
 * Uses client IP address as the rate limiting key.
 * 
 * Returns HTTP 429 (Too Many Requests) when rate limit is exceeded,
 * with Retry-After header indicating when to retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIP(request);
        String path = request.getRequestURI();
        
        Bucket bucket = resolveBucket(clientIp, path);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Request allowed - add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write(
                String.format("{\"error\": \"Rate limit exceeded\", \"retryAfter\": %d}", waitForRefill)
            );
            
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
        }
    }

    private Bucket resolveBucket(String clientIp, String path) {
        if (path.startsWith("/auth/login") || path.startsWith("/auth/register")) {
            return rateLimitConfig.resolveAuthBucket(clientIp);
        } else if (path.startsWith("/admin")) {
            return rateLimitConfig.resolveAdminBucket(clientIp);
        } else {
            return rateLimitConfig.resolveApiBucket(clientIp);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
