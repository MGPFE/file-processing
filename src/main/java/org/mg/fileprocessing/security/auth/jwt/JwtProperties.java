package org.mg.fileprocessing.security.auth.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
@Component
@NoArgsConstructor
@AllArgsConstructor
public class JwtProperties {
    private String secret;
    private Duration expiration = Duration.ofHours(1L);
}
