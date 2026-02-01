package org.mg.fileprocessing.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FileResourceLoaderTest {
    @TempDir
    Path tempDir;

    private FileResourceLoader fileResourceLoader;

    @BeforeEach
    void setUp() {
        fileResourceLoader = new FileResourceLoader();
    }

    @Test
    public void shouldLoadFileAsResource() throws IOException {
        // Given
        String content = "Test";
        Path tempFile = tempDir.resolve("test-file.jpg");
        Files.writeString(tempFile, content);

        // When
        Resource result = fileResourceLoader.loadPathAsResource(tempFile);

        // Then
        assertNotNull(result);
        assertThat(result.getContentAsByteArray()).isEqualTo(content.getBytes(StandardCharsets.UTF_8));
    }
}