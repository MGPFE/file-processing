package org.mg.fileprocessing.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "file-scan-retry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class FileScanRetryScheduleProperties {
    Integer scanPageSize = 100;
}
