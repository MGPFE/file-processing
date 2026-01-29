package org.mg.fileprocessing.config;

import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.StringCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RateLimitConfig {
    @Bean
    public ProxyManager<String> proxyManager(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
            RedisClient redisClient = (RedisClient) lettuceConnectionFactory.getNativeClient();

            return LettuceBasedProxyManager.builderFor(redisClient)
                    .build(StringCodec.UTF8);
        }
        throw new IllegalStateException("Expected RedisConnectionFactory to be type of LettuceConnectionFactory");
    }
}
