package org.mg.fileprocessing.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageTest {
    @TempDir
    private Path dummyPath;
    private LocalFileStorage localFileStorage;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        FileStorageProperties fileStorageProperties = new FileStorageProperties("local", dummyPath);

        localFileStorage = new LocalFileStorage(fileStorageProperties);
    }

    @Test
    public void shouldSaveFileToLocalStorage() {
        // Given
        String filename = "filename.jpg";
        String content = "TEST_IMAGE";
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "image/jpg",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // When
        localFileStorage.saveFileToStorage(multipartFile, filename);

        // Then
        assertThat(dummyPath.resolve(filename))
                .exists()
                .hasContent(content);
    }

    @Test
    public void shouldThrowExceptionIfParentDirDoesntMatch() {
        // Given
        String filename = "../malicious.jpg";
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpg",
                "data".getBytes(StandardCharsets.UTF_8)
        );

        // When
        // Then
        assertThatThrownBy(() -> localFileStorage.saveFileToStorage(multipartFile, filename))
                .isInstanceOf(FileHandlingException.class)
                .hasMessage("Cannot store file outside current directory");
    }

    @Test
    public void shouldThrowExceptionWhenInputStreamFails() throws IOException {
        // Given
        MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
        String filename = "test.txt";

        // When
        given(multipartFile.getInputStream()).willThrow(new IOException("Dummy exception"));

        // Then
        assertThatThrownBy(() -> localFileStorage.saveFileToStorage(multipartFile, filename))
                .isInstanceOf(FileHandlingException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessage("Failed to store a file");
    }
}