package org.mg.fileprocessing.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mg.fileprocessing.repository.FileRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageCleanerTest {
    @TempDir private Path dummyPath;
    @Mock private FileRepository fileRepository;
    private LocalFileStorageCleaner localFileStorageCleaner;

    private static final Instant FIXED_NOW = Instant.parse("2026-05-20T12:00:00Z");
    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        FileStorageProperties fileStorageProperties = new FileStorageProperties("local", dummyPath);

        localFileStorageCleaner = new LocalFileStorageCleaner(fileRepository, fileStorageProperties, fixedClock);
    }

    @Test
    public void shouldCleanOrphanedFiles() throws IOException {
        // Given
        String oldOrphanFilename = "old-orphan.txt";
        String regularFileFilename = "regular-file.txt";
        String newOrphanFilename = "new-orphan.txt";

        Path oldOrphan = createFile(dummyPath.resolve(oldOrphanFilename), FIXED_NOW.minus(Duration.ofDays(25)));
        Path regularFile = createFile(dummyPath.resolve(regularFileFilename), FIXED_NOW.minus(Duration.ofMinutes(10)));
        Path newOrphan = createFile(dummyPath.resolve(newOrphanFilename), FIXED_NOW.minus(Duration.ofHours(2)));

        given(fileRepository.existsByFileStorageName(oldOrphanFilename)).willReturn(false);

        // When
        localFileStorageCleaner.cleanOrphanedFiles();

        // Then
        assertAll(
                () -> assertFalse(Files.exists(oldOrphan), "Old orphan exists"),
                () -> assertTrue(Files.exists(regularFile), "Regular file exists"),
                () -> assertTrue(Files.exists(newOrphan), "New orphan exists")
        );
    }

    @Test
    public void shouldNotCleanOldFileIfNotOrphan() throws IOException {
        // Given
        String regularFileFilename = "regular-old-file.txt";
        Path regularOldFile = createFile(dummyPath.resolve(regularFileFilename), FIXED_NOW.minus(Duration.ofDays(10)));

        given(fileRepository.existsByFileStorageName(regularFileFilename)).willReturn(true);

        // When
        localFileStorageCleaner.cleanOrphanedFiles();

        // Then
        assertTrue(Files.exists(regularOldFile));
    }

    private Path createFile(Path path, Instant lastModified) throws IOException {
        Files.createFile(path);
        Files.setLastModifiedTime(path, FileTime.from(lastModified));
        return path;
    }
}