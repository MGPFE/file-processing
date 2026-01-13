package org.mg.fileprocessing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.compression.Compressor;
import org.mg.fileprocessing.http.ScanFileHttpClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScanConsumer {
    private final Compressor zipCompressor;
    private final ScanFileHttpClient scanFileHttpClient;

    @KafkaListener(topics = "file.upload.scan", groupId = "test-group-id")
    public void consumeFileScan(String stringPath) {
        log.info("Consumed path: {}", stringPath);
        Path compressedFile = zipCompressor.compress(Paths.get(stringPath));
        scanFileHttpClient.postFile(compressedFile);
    }
}
