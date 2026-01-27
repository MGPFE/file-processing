package org.mg.fileprocessing.compression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "compression")
@NoArgsConstructor
@AllArgsConstructor
@Component
@Getter
@Setter
public class CompressorProperties {
    private Path path = Path.of("compressed");
}
