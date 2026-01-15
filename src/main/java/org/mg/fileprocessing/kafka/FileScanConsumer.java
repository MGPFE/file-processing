package org.mg.fileprocessing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.mg.fileprocessing.compression.Compressor;
import org.mg.fileprocessing.entity.ScanStatus;
import org.mg.fileprocessing.http.*;
import org.mg.fileprocessing.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScanConsumer {
    private final FileService fileService;
    private final Compressor zipCompressor;
    private final ScanFileHttpClient scanFileHttpClient;
    private final ObjectMapper objectMapper;
    private final FileScanConsumerProperties fileScanConsumerProperties;

    private static final String COMPLETED = "completed";

    @KafkaListener(topics = "file.upload.scan", groupId = "test-group-id")
    public void consumeFileScan(String stringPath) {
        log.info("Consumed path: {}", stringPath);
        Path path = Paths.get(stringPath);
        try {
            Path compressedFile = zipCompressor.compress(path);

            fileService.handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);

            FilePostResult filePostResult = getFilePostResult(compressedFile);
            if (filePostResult == null || filePostResult.self() == null || filePostResult.self().isBlank()) {
                throw new IllegalStateException("Received illegal analysis url");
            }

            awaitScanComplete(filePostResult);
            ScanAnalysisResponse scanAnalysisResponse = getScanAnalysisResponse(filePostResult);

            processAnalysisResult(path, scanAnalysisResponse);
        } catch (ConditionTimeoutException e) {
            log.error("Scan timeout for file {}: {}", path, e.getMessage());
            fileService.handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
        } catch (Exception e) {
            log.error("Unexpected error while processing file {}: {}", path, e.getMessage());
            fileService.handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
        }
    }

    private boolean isSafe(ScanAnalysisStats scanAnalysisStats) {
        return scanAnalysisStats.harmless() >= 0
                && scanAnalysisStats.malicious() <= 0
                && scanAnalysisStats.suspicious() <= 0;
    }

    private FilePostResult getFilePostResult(Path compressedFile) {
        ResponseEntity<String> responseEntity = scanFileHttpClient.postFile(compressedFile);

        JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody())
                .path("data")
                .path("links");
        FilePostResult filePostResult = objectMapper.treeToValue(jsonNode, FilePostResult.class);
        log.info("Received file post result {}", filePostResult);
        return filePostResult;
    }

    private ScanAnalysisResponse getScanAnalysisResponse(FilePostResult filePostResult) {
        ResponseEntity<String> responseEntity = scanFileHttpClient.getAnalysis(filePostResult.self());

        JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody())
                .path("data")
                .path("attributes")
                .path("stats");
        ScanAnalysisStats scanAnalysisStats = objectMapper.treeToValue(jsonNode, ScanAnalysisStats.class);
        log.info("Received scan analysis stats {}", scanAnalysisStats);
        return new ScanAnalysisResponse(scanAnalysisStats, responseEntity.getStatusCode());
    }

    private void awaitScanComplete(FilePostResult filePostResult) {
        Awaitility.await()
                .atMost(fileScanConsumerProperties.getAwaitMaxTime())
                .pollInterval(fileScanConsumerProperties.getAwaitPollInterval())
                .until(() -> {
                    ResponseEntity<String> completeEntity = scanFileHttpClient.getAnalysis(filePostResult.self());
                    JsonNode completeJsonNode = objectMapper.readTree(completeEntity.getBody())
                            .path("data")
                            .path("attributes");
                    FileScanStatus fileScanStatus = objectMapper.treeToValue(completeJsonNode, FileScanStatus.class);
                    log.info("AWAITING SCAN COMPLETE: {}", fileScanStatus.status());
                    return COMPLETED.equals(fileScanStatus.status());
                });
    }

    private void processAnalysisResult(Path path, ScanAnalysisResponse scanAnalysisResponse) {
        if (!isSafe(scanAnalysisResponse.scanAnalysisStats())) {
            fileService.handleFileStatusUpdate(path, ScanStatus.FAILURE_UNSAFE);
            log.warn("Detected unsafe file, will be deleted by cleaner {}", path);
        } else {
            fileService.handleFileStatusUpdate(path, ScanStatus.SUCCESS);
            log.info("File scan successful {}", path);
        }
    }
}
