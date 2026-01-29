package org.mg.fileprocessing.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.mg.fileprocessing.exception.IdempotencyViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {
    private final RedisTemplate<String, String> redisTemplate;
    private final IdempotencyInterceptorProperties idempotencyInterceptorProperties;

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String PROCESSING_STATUS = "Processing";
    private static final String COMPLETED_STATUS = "Completed";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }

        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            throw new IdempotencyViolationException("%s header is mandatory".formatted(IDEMPOTENCY_KEY_HEADER));
        }

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(key, PROCESSING_STATUS, idempotencyInterceptorProperties.getKeyExpiration());

        if (Boolean.FALSE.equals(isFirstRequest)) {
            String status = redisTemplate.opsForValue().get(key);
            if (PROCESSING_STATUS.equals(status)) {
                response.setStatus(425);
            } else {
                response.setStatus(209);
            }
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key != null) {
            if (ex != null) {
                redisTemplate.delete(key);
            } else {
                redisTemplate.opsForValue().set(key, COMPLETED_STATUS, idempotencyInterceptorProperties.getKeyExpiration());
            }
        }
    }
}
