package org.mg.fileprocessing.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ConfigurationProperties(prefix = "scan")
public class ScanFileHttpClientProperties {
    private String url;
    private String apiKey;
    private Long bigScanThreshold;
    private String bigScanSuffix;
}
