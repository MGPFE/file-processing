package org.mg.fileprocessing.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RateLimitConfig {
    @Bean
    public ProxyManager<byte[]> proxyManager(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
            RedisClient redisClient = (RedisClient) lettuceConnectionFactory.getNativeClient();

            return LettuceBasedProxyManager.builderFor(redisClient).build();
        }
        throw new IllegalStateException("Expected RedisConnectionFactory to be of type LettuceConnectionFactory");
    }
}
