package org.mg.fileprocessing.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StorageConfig {
    private final FileStorageProperties fileStorageProperties;

    @PostConstruct
    public void init() {
        try {
            Path path = fileStorageProperties.getPath();
            if (Files.notExists(path)) {
                Files.createDirectories(path);
                log.info("Successfully created file storage directory: {}", path);
            }
        } catch (IOException e) {
            log.error("Couldn't create file storage directory");
            throw new RuntimeException("Couldn't create file storage directory", e);
        }
    }
}
