package org.mg.fileprocessing.compression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mg.fileprocessing.exception.FileHandlingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ZipCompressorTest {
    private static final Instant FIXED_NOW = Instant.parse("2026-05-20T12:00:00Z");
    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

    @TempDir
    private Path compressedPath;
    @TempDir
    private Path uploadedPath;

    private ZipCompressor zipCompressor;

    @BeforeEach
    void setUp() {
        CompressorProperties compressorProperties = new CompressorProperties(compressedPath);

        zipCompressor = new ZipCompressor(compressorProperties, fixedClock);
    }

    @Test
    public void shouldCompressPath() throws IOException {
        // Given
        String filename = "test-file.png";
        String content = "Test content";
        Path filePath = uploadedPath.resolve(filename);
        Files.writeString(filePath, content);

        // When
        Path result = zipCompressor.compress(filePath);

        // Then
        String prefix = String.valueOf(FIXED_NOW.toEpochMilli());
        assertTrue(result.getFileName().toString().startsWith(prefix));
        try (ZipFile zipFile = new ZipFile(result.toFile())) {
            ZipEntry entry = zipFile.getEntry(filename);

            assertNotNull(entry);
            String actualContent = new String(zipFile.getInputStream(entry).readAllBytes());
            assertEquals(content, actualContent);
        }
    }

    @Test
    public void shouldThrowExceptionWhenFailedWhileZippingFiles() {
        // Given
        Path brokenPath = Path.of("file-that-doesnt-exist");

        // When
        // Then
        assertThatThrownBy(() -> zipCompressor.compress(brokenPath))
                .isInstanceOf(FileHandlingException.class)
                .hasRootCauseInstanceOf(IOException.class)
                .hasMessage("Failed while zipping files for scan");
    }
}