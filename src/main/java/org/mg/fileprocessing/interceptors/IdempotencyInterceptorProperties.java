package org.mg.fileprocessing.interceptors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "idempotency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class IdempotencyInterceptorProperties {
    private Duration keyExpiration = Duration.ofHours(24L);
}
