package org.mg.fileprocessing.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "file-scan-consumer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class FileScanConsumerProperties {
    private Duration awaitMaxTime = Duration.ofMinutes(5L);
    private Duration awaitPollInterval = Duration.ofSeconds(20L);
}
