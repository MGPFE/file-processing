package org.mg.fileprocessing.checksum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "checksum")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Configuration
public class ChecksumConfig {
    private String algorithm;
}
