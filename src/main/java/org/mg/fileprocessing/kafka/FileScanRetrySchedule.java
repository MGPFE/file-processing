package org.mg.fileprocessing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.entity.ScanStatus;
import org.mg.fileprocessing.service.FileService;
import org.mg.fileprocessing.storage.FileStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScanRetrySchedule {
    private final FileService fileService;
    private final FileStorage fileStorage;
    private final FileScanRetryScheduleProperties fileScanRetryScheduleProperties;

    @Scheduled(cron = "0 * * * * *")
    public void retryScan() {
        log.info("Triggering file re-scan");
        Page<File> filePage = fileService.findByFilesStatus(ScanStatus.FAILURE_RETRIABLE, PageRequest.of(0, fileScanRetryScheduleProperties.getScanPageSize()));
        int reScannedFiles = 0;

        while (filePage.hasContent()) {
            for (File file : filePage.getContent()) {
                Path filePathFromStorage = null;
                try {
                    filePathFromStorage = fileStorage.getFilePathFromStorage(file.getFileStorageName());
                    fileService.handleFileStatusUpdate(filePathFromStorage, ScanStatus.RETRYING);
                    fileService.requestScan(filePathFromStorage);
                    reScannedFiles++;
                } catch (Exception e) {
                    log.error("Failed to re-scan file: {}", file.getFileStorageName(), e);
                    if (filePathFromStorage != null) {
                        fileService.handleFileStatusUpdate(filePathFromStorage, ScanStatus.FAILURE_RETRIABLE);
                    }
                }
            }

            if (filePage.hasNext()) {
                filePage = fileService.findByFilesStatus(ScanStatus.FAILURE_RETRIABLE, filePage.nextPageable());
            } else {
                break;
            }
        }

        log.info("Re-scanned {} files", reScannedFiles);
    }
}
