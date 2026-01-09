package org.mg.fileprocessing.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FileScanConsumer {
    @KafkaListener(topics = "file.upload.scan", groupId = "test-group-id")
    public void consumeFileScan(String message) {
        log.info("Consumed file: {}", message);
    }
}
