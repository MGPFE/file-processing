package org.mg.fileprocessing.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "storage")
@Component
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileStorageProperties {
    private String strategy = "local";
    private Path path;
}
