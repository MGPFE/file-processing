package org.mg.fileprocessing.compression;

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
public class CompressorConfig {
    private final CompressorProperties compressorProperties;

    @PostConstruct
    public void init() {
        try {
            Path path = compressorProperties.getPath();
            if (Files.notExists(path)) {
                Files.createDirectories(path);
                log.info("Successfully created file compressor directory: {}", path);
            }
        } catch (IOException e) {
            log.error("Couldn't create file compressor directory");
            throw new RuntimeException("Couldn't create file compressor directory", e);
        }
    }
}
