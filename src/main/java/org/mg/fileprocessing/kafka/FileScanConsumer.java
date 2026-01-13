package org.mg.fileprocessing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.compression.Compressor;
import org.mg.fileprocessing.entity.ScanStatus;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScanConsumer {
    private final FileService fileService;
    private final Compressor zipCompressor;
    private final ScanFileHttpClient scanFileHttpClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "file.upload.scan", groupId = "test-group-id")
    public void consumeFileScan(String stringPath) {
        log.info("Consumed path: {}", stringPath);
        Path path = Paths.get(stringPath);
        Path compressedFile = zipCompressor.compress(path);

        fileService.handleFileStatusUpdate(path, ScanStatus.IN_PROGRESS);

        ResponseEntity<String> responseEntity = scanFileHttpClient.postFile(compressedFile);

        JsonNode jsonNode = objectMapper.readTree(responseEntity.getBody())
                .path("data")
                .path("attributes")
                .path("last_analysis_stats");

        ScanAnalysisStats scanAnalysisStats = objectMapper.treeToValue(jsonNode, ScanAnalysisStats.class);
        log.info("Read scan analysis: {}", scanAnalysisStats);

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            fileService.handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
        } else if (!isSafe(scanAnalysisStats)) {
            fileService.handleFileStatusUpdate(path, ScanStatus.FAILURE_UNSAFE);
        } else {
            fileService.handleFileStatusUpdate(path, ScanStatus.SUCCESS);
        }
    }

    private boolean isSafe(ScanAnalysisStats scanAnalysisStats) {
        return scanAnalysisStats.harmless() >= 0
                && scanAnalysisStats.malicious() <= 0
                && scanAnalysisStats.suspicious() <= 0;
    }
}
