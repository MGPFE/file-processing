package org.mg.fileprocessing.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.repository.FileRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalFileStorageCleaner implements FileStorageCleaner {
    private final FileRepository fileRepository;
    private final FileStorageProperties fileStorageProperties;

    @Override
    @Scheduled(cron = "0 * * * * *")
    public void cleanOrphanedFiles() {
        log.info("Cleaning up orphaned files...");
        Instant hourAgo = Instant.now().minus(Duration.ofDays(1));

        try (Stream<Path> diskFiles = Files.list(fileStorageProperties.getPath())) {
            diskFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> isOlderThan(path, hourAgo))
                    .forEach(this::deleteIfOrphan);
        } catch (IOException e) {
            log.error("FS Cleaner: failed while scanning files in path");
        }
    }

    private boolean isOlderThan(Path path, Instant timestamp) {
        FileTime fileTime;
        try {
            fileTime = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            log.error("FS Cleaner: Failed while reading attributes of file: {}", path.getFileName().toString());
            return false;
        }

        Instant lastModified = fileTime.toInstant();

        return lastModified.isBefore(timestamp);
    }

    private void deleteIfOrphan(Path path) {
        String filename = path.getFileName().toString();

        if (!fileRepository.existsByFileStorageName(filename)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                log.error("FS Cleaner: Failed while deleting oprhan file: {}", path.getFileName().toString());
            }
        }
    }
}
