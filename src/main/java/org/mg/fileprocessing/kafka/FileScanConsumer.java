package org.mg.fileprocessing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.mg.fileprocessing.compression.Compressor;
import org.mg.fileprocessing.entity.ScanStatus;
import org.mg.fileprocessing.http.FilePostResult;
import org.mg.fileprocessing.http.FileScanStatus;
import org.mg.fileprocessing.http.ScanAnalysisStats;
import org.mg.fileprocessing.http.ScanFileHttpClient;
import org.mg.fileprocessing.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScanConsumer {
    private final FileService fileService;
    private final Compressor zipCompressor;
    private final ScanFileHttpClient scanFileHttpClient;
    private final ObjectMapper objectMapper;

    private static final String COMPLETED = "completed";

    @KafkaListener(topics = "file.upload.scan", groupId = "test-group-id")
    public void consumeFileScan(String stringPath) {
        log.info("Consumed path: {}", stringPath);
        Path path = Paths.get(stringPath);
        Path compressedFile = zipCompressor.compress(path);

        fileService.handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);

        ResponseEntity<String> responseEntity = scanFileHttpClient.postFile(compressedFile);

        JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody())
                .path("data")
                .path("links");
        FilePostResult filePostResult = objectMapper.treeToValue(jsonNode, FilePostResult.class);
        log.info("Received file post result {}", filePostResult);

        awaitScanComplete(filePostResult);

        responseEntity = scanFileHttpClient.getAnalysis(filePostResult.self());

        jsonNode = objectMapper.readTree(responseEntity.getBody())
                .path("data")
                .path("attributes")
                .path("stats");
        ScanAnalysisStats scanAnalysisStats = objectMapper.treeToValue(jsonNode, ScanAnalysisStats.class);
        log.info("Received scan analysis stats {}", scanAnalysisStats);

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            fileService.handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
            log.warn("Scan failed, will be retried {}", path);
        } else if (!isSafe(scanAnalysisStats)) {
            fileService.handleFileStatusUpdate(path, ScanStatus.FAILURE_UNSAFE);
            log.warn("Detected unsafe file, will be deleted by cleaner {}", path);
        } else {
            fileService.handleFileStatusUpdate(path, ScanStatus.SUCCESS);
            log.info("File scan successful {}", path);
        }
    }

    private boolean isSafe(ScanAnalysisStats scanAnalysisStats) {
        return scanAnalysisStats.harmless() >= 0
                && scanAnalysisStats.malicious() <= 0
                && scanAnalysisStats.suspicious() <= 0;
    }

    private void awaitScanComplete(FilePostResult filePostResult) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(5L))
                .pollInterval(Duration.ofSeconds(20L))
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
}
