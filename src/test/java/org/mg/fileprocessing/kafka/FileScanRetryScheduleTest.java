package org.mg.fileprocessing.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.entity.ScanStatus;
import org.mg.fileprocessing.service.FileService;
import org.mg.fileprocessing.storage.FileStorage;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileScanRetryScheduleTest {
    @Mock private FileService fileService;
    @Mock private FileStorage fileStorage;

    private FileScanRetrySchedule fileScanRetrySchedule;

    @BeforeEach
    void setUp() {
        FileScanRetryScheduleProperties fileScanRetryScheduleProperties = new FileScanRetryScheduleProperties(5);

        fileScanRetrySchedule = new FileScanRetrySchedule(fileService, fileStorage, fileScanRetryScheduleProperties);
    }

    @Test
    public void shouldRetryScanForFailureRetriableFile() {
        // Given
        String fileStorageName = "Test-storage.jpg";
        File file = File.builder()
                .fileStorageName(fileStorageName)
                .uuid(UUID.fromString("86dbe6ec-2ec6-49e8-8ba0-4a565e66c31f"))
                .build();
        Path path = Path.of("/test/path/" + fileStorageName);

        given(fileService.findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class))).willReturn(new PageImpl<>(List.of(file)));
        given(fileStorage.getFilePathFromStorage(fileStorageName)).willReturn(path);

        // When
        fileScanRetrySchedule.retryScan();

        // Then
        verify(fileService).findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class));
        verify(fileStorage).getFilePathFromStorage(fileStorageName);
        verify(fileService).handleFileStatusUpdate(path, ScanStatus.RETRYING);
        verify(fileService).requestScan(path);
        verify(fileService, never()).handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
    }

    @Test
    public void shouldRetryScanForFailureRetriableMultipleFilesSinglePage() {
        // Given
        String fileStorageName1 = "Test-storage-1.jpg";
        String fileStorageName2 = "Test-storage-2.jpg";
        String fileStorageName3 = "Test-storage-3.jpg";
        File file1 = File.builder()
                .fileStorageName(fileStorageName1)
                .uuid(UUID.fromString("86dbe6ec-2ec6-49e8-8ba0-4a565e66c31f"))
                .build();
        File file2 = File.builder()
                .fileStorageName(fileStorageName2)
                .uuid(UUID.fromString("86dbe6ec-2ec6-49e8-8ba0-4a565e66c31f"))
                .build();
        File file3 = File.builder()
                .fileStorageName(fileStorageName3)
                .uuid(UUID.fromString("86dbe6ec-2ec6-49e8-8ba0-4a565e66c31f"))
                .build();
        Path path1 = Path.of("/test/path/" + fileStorageName1);
        Path path2 = Path.of("/test/path/" + fileStorageName2);
        Path path3 = Path.of("/test/path/" + fileStorageName3);

        given(fileService.findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class))).willReturn(new PageImpl<>(List.of(file1, file2, file3)));
        given(fileStorage.getFilePathFromStorage(fileStorageName1)).willReturn(path1);
        given(fileStorage.getFilePathFromStorage(fileStorageName2)).willReturn(path2);
        given(fileStorage.getFilePathFromStorage(fileStorageName3)).willReturn(path3);

        // When
        fileScanRetrySchedule.retryScan();

        // Then
        verify(fileService).findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class));
        verify(fileStorage).getFilePathFromStorage(fileStorageName1);
        verify(fileStorage).getFilePathFromStorage(fileStorageName2);
        verify(fileStorage).getFilePathFromStorage(fileStorageName3);
        verify(fileService).handleFileStatusUpdate(path1, ScanStatus.RETRYING);
        verify(fileService).handleFileStatusUpdate(path2, ScanStatus.RETRYING);
        verify(fileService).handleFileStatusUpdate(path3, ScanStatus.RETRYING);
        verify(fileService).requestScan(path1);
        verify(fileService).requestScan(path2);
        verify(fileService).requestScan(path3);
        verify(fileService, never()).handleFileStatusUpdate(any(Path.class), eq(ScanStatus.FAILURE_RETRIABLE));
    }

    @Test
    public void shouldRetryScanForFailureRetriableMultipleFilesMultiplePages() {
        // Given
        String fileStorageName1 = "Test-storage-1.jpg";
        String fileStorageName2 = "Test-storage-2.jpg";
        File file1 = File.builder()
                .fileStorageName(fileStorageName1)
                .uuid(UUID.fromString("86dbe6ec-2ec6-49e8-8ba0-4a565e66c31f"))
                .build();
        File file2 = File.builder()
                .fileStorageName(fileStorageName2)
                .uuid(UUID.fromString("86dbe6ec-2ec6-49e8-8ba0-4a565e66c31f"))
                .build();
        Path path1 = Path.of("/test/path/" + fileStorageName1);
        Path path2 = Path.of("/test/path/" + fileStorageName2);

        PageRequest firstPageReq = PageRequest.of(0, 5);
        Page<File> firstPage = new PageImpl<>(List.of(file1), firstPageReq, 10);
        Page<File> secondPage = new PageImpl<>(List.of(file2));

        given(fileService.findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class)))
                .willReturn(firstPage)
                .willReturn(secondPage);
        given(fileStorage.getFilePathFromStorage(fileStorageName1)).willReturn(path1);
        given(fileStorage.getFilePathFromStorage(fileStorageName2)).willReturn(path2);

        // When
        fileScanRetrySchedule.retryScan();

        // Then
        verify(fileService, times(2)).findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class));
        verify(fileStorage).getFilePathFromStorage(fileStorageName1);
        verify(fileStorage).getFilePathFromStorage(fileStorageName2);
        verify(fileService).handleFileStatusUpdate(path1, ScanStatus.RETRYING);
        verify(fileService).handleFileStatusUpdate(path2, ScanStatus.RETRYING);
        verify(fileService).requestScan(path1);
        verify(fileService).requestScan(path2);
        verify(fileService, never()).handleFileStatusUpdate(any(Path.class), eq(ScanStatus.FAILURE_RETRIABLE));
    }

    @Test
    public void shouldDoNothingWhenFailedDuringRetrievingFilePath() {
        // Given
        String fileStorageName = "Test-storage.jpg";
        File file = File.builder()
                .fileStorageName(fileStorageName)
                .uuid(UUID.fromString("86dbe6ec-2ec6-49e8-8ba0-4a565e66c31f"))
                .build();
        Path path = Path.of("/test/path/" + fileStorageName);

        given(fileService.findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class))).willReturn(new PageImpl<>(List.of(file)));
        given(fileStorage.getFilePathFromStorage(fileStorageName)).willThrow(new RuntimeException("File doesn't exist"));

        // When
        fileScanRetrySchedule.retryScan();

        // Then
        verify(fileService).findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class));
        verify(fileStorage).getFilePathFromStorage(fileStorageName);
        verify(fileService, never()).handleFileStatusUpdate(path, ScanStatus.RETRYING);
        verify(fileService, never()).requestScan(path);
        verify(fileService, never()).handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
    }

    @Test
    public void shouldMarkFileForRescanWhenFailedDuringRequestingScan() {
        // Given
        String fileStorageName = "Test-storage.jpg";
        File file = File.builder()
                .fileStorageName(fileStorageName)
                .uuid(UUID.fromString("86dbe6ec-2ec6-49e8-8ba0-4a565e66c31f"))
                .build();
        Path path = Path.of("/test/path/" + fileStorageName);

        given(fileService.findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class))).willReturn(new PageImpl<>(List.of(file)));
        given(fileStorage.getFilePathFromStorage(fileStorageName)).willReturn(path);
        willThrow(new RuntimeException("Couldn't trigger scan")).given(fileService).requestScan(path);

        // When
        fileScanRetrySchedule.retryScan();

        // Then
        verify(fileService).findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class));
        verify(fileStorage).getFilePathFromStorage(fileStorageName);
        verify(fileService).handleFileStatusUpdate(path, ScanStatus.RETRYING);
        verify(fileService).requestScan(path);
        verify(fileService).handleFileStatusUpdate(path, ScanStatus.FAILURE_RETRIABLE);
    }

    @Test
    public void shouldDoNothingWhenNoFilesFoundToScan() {
        // Given
        given(fileService.findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class))).willReturn(Page.empty());

        // When
        fileScanRetrySchedule.retryScan();

        // Then
        verify(fileService).findByFilesStatus(eq(ScanStatus.FAILURE_RETRIABLE), any(Pageable.class));
        verify(fileStorage, never()).getFilePathFromStorage(anyString());
        verify(fileService, never()).handleFileStatusUpdate(any(Path.class), any(ScanStatus.class));
        verify(fileService, never()).requestScan(any(Path.class));
    }
}