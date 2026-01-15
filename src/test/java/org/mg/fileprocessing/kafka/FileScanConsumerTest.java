package org.mg.fileprocessing.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mg.fileprocessing.TestUtils;
import org.mg.fileprocessing.compression.Compressor;
import org.mg.fileprocessing.entity.ScanStatus;
import org.mg.fileprocessing.http.ScanFileHttpClient;
import org.mg.fileprocessing.service.FileService;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileScanConsumerTest {
    @Mock private FileService fileService;
    @Mock private Compressor zipCompressor;
    @Mock private ScanFileHttpClient scanFileHttpClient;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    private FileScanConsumer fileScanConsumer;

    @BeforeEach
    void setUp() {
        FileScanConsumerProperties fileScanConsumerProperties = new FileScanConsumerProperties(Duration.ofMinutes(5L), Duration.ofSeconds(1L));

        fileScanConsumer = new FileScanConsumer(fileService, zipCompressor, scanFileHttpClient, objectMapper, fileScanConsumerProperties);
    }

    @Test
    public void shouldProcessCorrectlyWhenFileSafe() {
        // Given
        String stringPath = "test-file.txt";
        Path path = Path.of(stringPath);
        Path compressedPath = Path.of(stringPath.split("\\.")[0] + ".zip");

        given(zipCompressor.compress(path)).willReturn(compressedPath);
        given(scanFileHttpClient.postFile(compressedPath)).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/file-post-response.json"))));
        given(scanFileHttpClient.getAnalysis(anyString())).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/analysis-response.json"))));

        // When
        fileScanConsumer.consumeFileScan(stringPath);

        // Then
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.SUCCESS);
        verify(zipCompressor, times(1)).compress(path);
    }

    @Test
    public void shouldMarkFileAsUnsafeWhenScanApiDetectsSomething() {
        // Given
        String stringPath = "test-file.txt";
        Path path = Path.of(stringPath);
        Path compressedPath = Path.of(stringPath.split("\\.")[0] + ".zip");

        given(zipCompressor.compress(path)).willReturn(compressedPath);
        given(scanFileHttpClient.postFile(compressedPath)).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/file-post-response.json"))));
        given(scanFileHttpClient.getAnalysis(anyString())).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/analysis-response-unsafe.json"))));

        // When
        fileScanConsumer.consumeFileScan(stringPath);

        // Then
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.FAILURE_UNSAFE);
        verify(zipCompressor, times(1)).compress(path);
    }

    @Test
    public void shouldMarkFileForScanRetryWhenStatusCodeIsNot200() {
        // Given
        String stringPath = "test-file.txt";
        Path path = Path.of(stringPath);
        Path compressedPath = Path.of(stringPath.split("\\.")[0] + ".zip");

        given(zipCompressor.compress(path)).willReturn(compressedPath);
        given(scanFileHttpClient.postFile(compressedPath)).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/file-post-response.json"))));
        given(scanFileHttpClient.getAnalysis(anyString())).willReturn(ResponseEntity.internalServerError().build());

        // When
        fileScanConsumer.consumeFileScan(stringPath);

        // Then
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
        verify(zipCompressor, times(1)).compress(path);
    }

    @Test
    public void shouldSetStatusToFailureRetriableIfAnalysisUrlIsMissing() {
        // Given
        String stringPath = "test-file.txt";
        Path path = Path.of(stringPath);
        Path compressedPath = Path.of(stringPath.split("\\.")[0] + ".zip");

        given(zipCompressor.compress(path)).willReturn(compressedPath);
        given(scanFileHttpClient.postFile(compressedPath)).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/file-post-response-no-url.json"))));

        // When
        fileScanConsumer.consumeFileScan(stringPath);

        // Then
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
        verify(zipCompressor, times(1)).compress(path);
    }

    @Test
    public void shouldSetStatusToFailureRetriableIfAnalysisUrlIsBlank() {
        // Given
        String stringPath = "test-file.txt";
        Path path = Path.of(stringPath);
        Path compressedPath = Path.of(stringPath.split("\\.")[0] + ".zip");

        given(zipCompressor.compress(path)).willReturn(compressedPath);
        given(scanFileHttpClient.postFile(compressedPath)).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/file-post-response-blank-url.json"))));

        // When
        fileScanConsumer.consumeFileScan(stringPath);

        // Then
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
        verify(zipCompressor, times(1)).compress(path);
    }

    @Test
    public void shouldMarkFileAsFailureRetriableWhenTimeoutDuringAwait() {
        // Given
        FileScanConsumerProperties fileScanConsumerProperties = new FileScanConsumerProperties(Duration.ofMillis(100L), Duration.ofMillis(50L));
        fileScanConsumer = new FileScanConsumer(fileService, zipCompressor, scanFileHttpClient, objectMapper, fileScanConsumerProperties);

        String stringPath = "test-file.txt";
        Path path = Path.of(stringPath);
        Path compressedPath = Path.of(stringPath.split("\\.")[0] + ".zip");

        given(zipCompressor.compress(path)).willReturn(compressedPath);
        given(scanFileHttpClient.postFile(compressedPath)).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/file-post-response.json"))));
        given(scanFileHttpClient.getAnalysis(anyString())).willReturn(ResponseEntity.ok(TestUtils.getResourceAsString(Path.of("kafka/analysis-response-incomplete.json"))));

        // When
        fileScanConsumer.consumeFileScan(stringPath);

        // Then
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);
        verify(fileService, times(1)).handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
        verify(zipCompressor, times(1)).compress(path);
    }
}