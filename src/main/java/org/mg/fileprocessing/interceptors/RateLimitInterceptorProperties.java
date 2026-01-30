package org.mg.fileprocessing.interceptors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "ratelimit")
@Component
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitInterceptorProperties {
    private Long maxTransactions = 10L;
    private Duration perTimeframe = Duration.ofMinutes(10L);
}
