package org.mg.fileprocessing;

import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.interceptors.IdempotencyInterceptor;
import org.mg.fileprocessing.interceptors.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final IdempotencyInterceptor idempotencyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/");

        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/files/**");
    }
}
