package org.mg.fileprocessing.interceptors;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final ProxyManager<String> buckets;
    private final RateLimitInterceptorProperties rateLimitInterceptorProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = request.getRemoteAddr();

        BucketConfiguration bucketConfiguration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(rateLimitInterceptorProperties.getMaxTransactions())
                        .refillGreedy(rateLimitInterceptorProperties.getMaxTransactions(), rateLimitInterceptorProperties.getPerTimeframe()))
                .build();

        Bucket bucket = buckets.builder().build(key, () -> bucketConfiguration);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return true;
        } else {
            throw new RateLimitExceededException(Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
    }
}
