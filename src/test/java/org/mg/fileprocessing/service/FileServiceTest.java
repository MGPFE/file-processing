package org.mg.fileprocessing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mg.fileprocessing.checksum.ChecksumUtil;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.entity.FileVisibility;
import org.mg.fileprocessing.entity.ScanStatus;
import org.mg.fileprocessing.entity.User;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.exception.UnsupportedContentTypeException;
import org.mg.fileprocessing.repository.FileRepository;
import org.mg.fileprocessing.security.auth.UserRole;
import org.mg.fileprocessing.storage.FileStorage;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Captor
    private ArgumentCaptor<File> fileArgumentCaptor;

    @Mock private FileRepository fileRepositoryMock;
    @Mock private ChecksumUtil checksumUtil;
    @Mock private FileStorage fileStorage;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    private FileService fileService;

    private User user;

    @BeforeEach
    void setUp() {
        FileUploadProperties fileUploadProperties = new FileUploadProperties();

        user = User.builder()
                .id(1L)
                .email("test@email.com")
                .password("testpassword")
                .roles(List.of(UserRole.USER))
                .build();

        fileService = new FileService(fileStorage, checksumUtil, fileUploadProperties, fileRepositoryMock, kafkaTemplate);
    }

    @Test
    public void shouldFindFileByUuid() {
        // Given
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        File file = File.builder()
                .originalFilename("test-file")
                .uuid(uuid)
                .size(100L)
                .user(user)
                .build();

        given(fileRepositoryMock.findFileByUuidAndUserId(uuid, user.getId())).willReturn(Optional.of(file));

        // When
        RetrieveFileDto result = fileService.findByUuid(uuid, user.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.uuid()).isEqualTo(uuid);
        assertThat(result.filename()).isEqualTo(file.getOriginalFilename());
        assertThat(result.size()).isEqualTo(file.getSize());
        verify(fileRepositoryMock).findFileByUuidAndUserId(uuid, user.getId());
    }

    @Test
    public void shouldThrowExceptionWhenFileNotFoundByUuid() {
        // Given
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        given(fileRepositoryMock.findFileByUuidAndUserId(uuid, user.getId())).willReturn(Optional.empty());

        // When
        // Then
        assertThatThrownBy(() -> fileService.findByUuid(uuid, user.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("File with UUID %s not found".formatted(uuid));
        verify(fileRepositoryMock).findFileByUuidAndUserId(uuid, user.getId());
    }

    @Test
    public void shouldFindAllFiles() {
        // Given
        UUID uuid = UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f");
        File file = File.builder()
                .originalFilename("test-file")
                .uuid(uuid)
                .size(100L)
                .user(user)
                .build();
        RetrieveFileDto retrieveFileDto = RetrieveFileDto.builder()
                .uuid(uuid)
                .filename(file.getOriginalFilename())
                .size(file.getSize())
                .build();

        UUID uuid2 = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        File file2 = File.builder()
                .originalFilename("test-file2")
                .uuid(uuid2)
                .size(200L)
                .user(user)
                .build();
        RetrieveFileDto retrieveFileDto2 = RetrieveFileDto.builder()
                .uuid(uuid2)
                .filename(file2.getOriginalFilename())
                .size(file2.getSize())
                .build();

        given(fileRepositoryMock.findFilesByUserIdOrFileVisibility(user.getId(), FileVisibility.PUBLIC)).willReturn(List.of(file, file2));

        // When
        List<RetrieveFileDto> result = fileService.findAll(user.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);
        assertThat(result).containsAll(List.of(retrieveFileDto, retrieveFileDto2));
        verify(fileRepositoryMock).findFilesByUserIdOrFileVisibility(user.getId(), FileVisibility.PUBLIC);
    }

    @Test
    public void shouldReturnEmptyListWhenNoFilesFound() {
        // Given
        given(fileRepositoryMock.findFilesByUserIdOrFileVisibility(user.getId(), FileVisibility.PUBLIC)).willReturn(Collections.emptyList());

        // When
        List<RetrieveFileDto> result = fileService.findAll(user.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(fileRepositoryMock).findFilesByUserIdOrFileVisibility(user.getId(), FileVisibility.PUBLIC);
    }

    @Test
    public void shouldDeleteFileByUuid() {
        // Given
        UUID uuid = UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f");
        String filename = "test";

        given(fileRepositoryMock.findFileByUuidAndUserId(uuid, user.getId())).willReturn(Optional.of(File.builder().uuid(uuid).fileStorageName(filename).build()));

        // When
        fileService.deleteFile(uuid, user.getId());

        // Then
        verify(fileRepositoryMock).findFileByUuidAndUserId(uuid, user.getId());
        verify(fileRepositoryMock).deleteFileByUuid(uuid);
        verify(fileStorage).deleteFileFromStorage(filename);
    }

    @Test
    public void shouldDoNothingWhenDeleteFileByUuidNoFile() {
        // Given
        UUID uuid = UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f");
        String filename = "test";

        given(fileRepositoryMock.findFileByUuidAndUserId(uuid, user.getId())).willReturn(Optional.empty());

        // When
        fileService.deleteFile(uuid, user.getId());

        // Then
        verify(fileRepositoryMock).findFileByUuidAndUserId(uuid, user.getId());
        verify(fileRepositoryMock, never()).deleteFileByUuid(uuid);
        verify(fileStorage, never()).deleteFileFromStorage(filename);
    }

    @Test
    public void shouldSaveFileWhenUsingDefaultAllowedContentTypes() throws IOException {
        // Given
        String filename = "filename.jpg";
        String checksum = "05D84936CE1050C2B19D0618D342EA7403B96A46FBB73F86623AF1BD63384652";
        String fileStorageName = "%s-%s".formatted(checksum, filename);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "image/jpg",
                "BYTES".getBytes(StandardCharsets.UTF_8)
        );

        given(checksumUtil.getChecksumAsString(any(InputStream.class))).willReturn(checksum);
        given(fileStorage.saveFileToStorage(multipartFile, fileStorageName)).willReturn(Path.of("%s-%s".formatted(checksum, filename)));
        given(kafkaTemplate.send(anyString(), anyString())).willReturn(
                CompletableFuture.completedFuture(null));

        // When
        RetrieveFileDto result = fileService.uploadFile(multipartFile, user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.size()).isEqualTo(5);
        verify(fileRepositoryMock).save(any(File.class));
        verify(checksumUtil).getChecksumAsString(any(InputStream.class));
        verify(fileStorage).saveFileToStorage(multipartFile, fileStorageName);
    }

    @Test
    public void shouldSaveFileWhenAllowedContentTypesSpecified() throws IOException {
        // Given
        FileUploadProperties fileUploadProperties = new FileUploadProperties(List.of("image/jpg"));
        fileService = new FileService(fileStorage, checksumUtil, fileUploadProperties, fileRepositoryMock, kafkaTemplate);
        String filename = "filename.jpg";
        String checksum = "05D84936CE1050C2B19D0618D342EA7403B96A46FBB73F86623AF1BD63384652";
        String fileStorageName = "%s-%s".formatted(checksum, filename);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "image/jpg",
                "BYTES".getBytes(StandardCharsets.UTF_8)
        );

        given(checksumUtil.getChecksumAsString(any(InputStream.class))).willReturn(checksum);
        given(fileStorage.saveFileToStorage(multipartFile, fileStorageName)).willReturn(Path.of("%s-%s".formatted(checksum, filename)));
        given(kafkaTemplate.send(anyString(), anyString())).willReturn(
                CompletableFuture.completedFuture(null));

        // When
        RetrieveFileDto result = fileService.uploadFile(multipartFile, user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.size()).isEqualTo(5);
        verify(fileRepositoryMock).save(any(File.class));
        verify(checksumUtil).getChecksumAsString(any(InputStream.class));
        verify(fileStorage).saveFileToStorage(multipartFile, fileStorageName);
    }

    @Test
    public void shouldThrowExceptionWhenContentTypeNotAllowed() {
        // Given
        FileUploadProperties fileUploadProperties = new FileUploadProperties(List.of("image/jpg"));
        fileService = new FileService(fileStorage, checksumUtil, fileUploadProperties, fileRepositoryMock, kafkaTemplate);

        String filename = "filename.jpg";
        String contentType = "image/png";

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                contentType,
                "BYTES".getBytes(StandardCharsets.UTF_8)
        );

        // When
        // Then
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, user))
                .isInstanceOf(UnsupportedContentTypeException.class)
                .hasMessage("Content type %s is not supported".formatted(contentType));
    }

    @Test
    public void shouldThrowExceptionWhenChecksumUtilThrowsIOException() throws IOException {
        // Given
        String filename = "filename.jpg";
        String contentType = "image/jpg";
        String ioExceptionMessage = "Dummy exception";

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                contentType,
                "BYTES".getBytes(StandardCharsets.UTF_8)
        );

        given(checksumUtil.getChecksumAsString(any(InputStream.class))).willThrow(new IOException(ioExceptionMessage));

        // When
        // Then
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, user))
                .isInstanceOf(FileHandlingException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasRootCauseMessage(ioExceptionMessage)
                .hasMessage("Failed while streaming from file");
    }

    @Test
    public void shouldNotSaveNewFileIfFileWithChecksumAlreadyExists() throws IOException {
        // Given
        String filename = "filename.jpg";
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        String dummyChecksum = "DUMMY_CHECKSUM";
        byte[] content = "TEST".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                content
        );

        File file = File.builder()
                .id(1L)
                .uuid(uuid)
                .originalFilename(filename)
                .fileStorageName("%s-%s".formatted(dummyChecksum, filename))
                .size((long) content.length)
                .contentType(MediaType.IMAGE_JPEG_VALUE)
                .checksum(dummyChecksum)
                .scanStatus(ScanStatus.NOT_STARTED)
                .build();

        given(checksumUtil.getChecksumAsString(any(InputStream.class))).willReturn(dummyChecksum);
        given(fileRepositoryMock.findFileByChecksum(dummyChecksum)).willReturn(Optional.of(file));

        // When
        RetrieveFileDto result = fileService.uploadFile(multipartFile, user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.uuid()).isEqualTo(uuid);
        assertThat(result.size()).isEqualTo(content.length);
        verify(fileRepositoryMock, never()).save(any(File.class));
        verify(fileStorage, never()).saveFileToStorage(any(MultipartFile.class), anyString());
    }

    @Test
    public void shouldSaveNewFileWhenFileWithChecksumDoesntExist() throws IOException {
        // Given
        String filename = "filename.jpg";
        String dummyChecksum = "DUMMY_CHECKSUM";
        byte[] content = "TEST".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "image/jpg",
                content
        );

        given(checksumUtil.getChecksumAsString(any(InputStream.class))).willReturn(dummyChecksum);
        given(fileRepositoryMock.findFileByChecksum(dummyChecksum)).willReturn(Optional.empty());
        given(kafkaTemplate.send(anyString(), anyString())).willReturn(
                CompletableFuture.completedFuture(null));
        given(fileStorage.saveFileToStorage(eq(multipartFile), anyString())).willReturn(Path.of("%s-%s".formatted(dummyChecksum, filename)));

        // When
        RetrieveFileDto result = fileService.uploadFile(multipartFile, user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.uuid()).isNotNull();
        assertThat(result.size()).isEqualTo(content.length);
        verify(fileRepositoryMock).save(fileArgumentCaptor.capture());
        verify(fileStorage).saveFileToStorage(multipartFile, "%s-%s".formatted(dummyChecksum, filename));

        File capturedFile = fileArgumentCaptor.getValue();

        assertThat(capturedFile.getUuid()).isNotNull();
        assertThat(capturedFile.getOriginalFilename()).isEqualTo(filename);
        assertThat(capturedFile.getChecksum()).isEqualTo(dummyChecksum);
        assertThat(capturedFile.getSize()).isEqualTo(content.length);
        assertThat(capturedFile.getFileStorageName()).isEqualTo("%s-%s".formatted(dummyChecksum, filename));
    }

    @Test
    public void shouldNotSaveFileWithZeroBytes() {
        // Given
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "zero-bytes.txt",
                "text/plain",
                new byte[0]
        );

        // When
        // Then
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, user))
                .isInstanceOf(FileHandlingException.class)
                .hasMessage("Cannot upload file smaller than 1 byte");
        verify(fileRepositoryMock, never()).findFileByChecksum(anyString());
        verify(fileRepositoryMock, never()).save(any(File.class));
        verify(fileStorage, never()).saveFileToStorage(any(MultipartFile.class), anyString());
    }

    @Test
    public void shouldUseChecksumAsFileStorageNameWhenOriginalFilenameIsBlank() throws IOException {
        // Given
        String filename = "";
        String dummyChecksum = "DUMMY_CHECKSUM";
        byte[] content = "TEST".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                content
        );

        given(checksumUtil.getChecksumAsString(any(InputStream.class))).willReturn(dummyChecksum);
        given(fileRepositoryMock.findFileByChecksum(dummyChecksum)).willReturn(Optional.empty());
        given(fileStorage.saveFileToStorage(eq(multipartFile), anyString())).willReturn(Path.of("%s-%s".formatted(dummyChecksum, filename)));
        given(kafkaTemplate.send(anyString(), anyString())).willReturn(
                CompletableFuture.completedFuture(null));

        // When
        RetrieveFileDto result = fileService.uploadFile(multipartFile, user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.uuid()).isNotNull();
        assertThat(result.size()).isEqualTo(content.length);
        verify(fileRepositoryMock).save(fileArgumentCaptor.capture());
        verify(fileStorage).saveFileToStorage(multipartFile, dummyChecksum);


        File capturedFile = fileArgumentCaptor.getValue();

        assertThat(capturedFile.getUuid()).isNotNull();
        assertThat(capturedFile.getOriginalFilename()).isEqualTo(filename);
        assertThat(capturedFile.getChecksum()).isEqualTo(dummyChecksum);
        assertThat(capturedFile.getSize()).isEqualTo(content.length);
        assertThat(capturedFile.getFileStorageName()).isEqualTo(dummyChecksum);
    }

    @Test
    public void shouldUseChecksumAsFileStorageNameWhenOriginalFilenameIsNull() throws IOException {
        // Given
        String filename = null;
        String dummyChecksum = "DUMMY_CHECKSUM";
        byte[] content = "TEST".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                content
        );

        given(checksumUtil.getChecksumAsString(any(InputStream.class))).willReturn(dummyChecksum);
        given(fileRepositoryMock.findFileByChecksum(dummyChecksum)).willReturn(Optional.empty());
        given(fileStorage.saveFileToStorage(eq(multipartFile), anyString())).willReturn(Path.of("%s-%s".formatted(dummyChecksum, filename)));
        given(kafkaTemplate.send(anyString(), anyString())).willReturn(
                CompletableFuture.completedFuture(null));

        // When
        RetrieveFileDto result = fileService.uploadFile(multipartFile, user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo("");
        assertThat(result.uuid()).isNotNull();
        assertThat(result.size()).isEqualTo(content.length);
        verify(fileRepositoryMock).save(fileArgumentCaptor.capture());
        verify(fileStorage).saveFileToStorage(multipartFile, dummyChecksum);

        File capturedFile = fileArgumentCaptor.getValue();

        assertThat(capturedFile.getUuid()).isNotNull();
        assertThat(capturedFile.getOriginalFilename()).isEqualTo("");
        assertThat(capturedFile.getChecksum()).isEqualTo(dummyChecksum);
        assertThat(capturedFile.getSize()).isEqualTo(content.length);
        assertThat(capturedFile.getFileStorageName()).isEqualTo(dummyChecksum);
    }

    @Test
    public void deleteFileWithFilenameShouldDelegateToDeleteFileWithUuid() {
        // Given
        String filename = "test";
        UUID uuid = UUID.randomUUID();

        given(fileRepositoryMock.findByFileStorageName(filename)).willReturn(Optional.of(File.builder().uuid(uuid).fileStorageName(filename).build()));

        // When
        fileService.deleteFile(filename);

        // Then
        verify(fileRepositoryMock).findByFileStorageName(filename);
        verify(fileRepositoryMock).deleteFileByUuid(uuid);
        verify(fileStorage).deleteFileFromStorage(filename);
    }

    @Test
    public void deleteFileWithFilenameShouldDoNothingWhenFileNotPresent() {
        // Given
        String filename = "test";
        UUID uuid = UUID.randomUUID();

        given(fileRepositoryMock.findByFileStorageName(filename)).willReturn(Optional.empty());

        // When
        fileService.deleteFile(filename);

        // Then
        verify(fileRepositoryMock).findByFileStorageName(filename);
        verify(fileRepositoryMock, never()).deleteFileByUuid(uuid);
        verify(fileStorage, never()).deleteFileFromStorage(filename);
    }

    @Test
    public void shouldHandleFileStatusUpdate() {
        // Given
        Path path = Path.of("/test/path.jpg");
        ScanStatus scanStatus = ScanStatus.IN_PROGRESS;
        File file = File.builder()
                .scanStatus(ScanStatus.NOT_STARTED)
                .build();

        given(fileRepositoryMock.findByFileStorageName(path.getFileName().toString())).willReturn(Optional.of(file));

        // When
        fileService.handleFileStatusUpdate(path, scanStatus);

        // Then
        verify(fileRepositoryMock).findByFileStorageName(path.getFileName().toString());
        assertThat(file.getScanStatus()).isEqualTo(scanStatus);
    }

    @Test
    public void shouldThrowExceptionWhenFileForStatusUpdateDoesntExist() {
        // Given
        Path path = Path.of("/test/path.jpg");
        ScanStatus scanStatus = ScanStatus.IN_PROGRESS;

        given(fileRepositoryMock.findByFileStorageName(path.getFileName().toString())).willReturn(Optional.empty());

        // When
        // Then
        assertThatThrownBy(() -> fileService.handleFileStatusUpdate(path, scanStatus))
                .isInstanceOf(FileHandlingException.class)
                .hasMessage("File " + path + " not found in storage");
    }

    @Test
    public void shouldFindFilesByStatus() {
        // Given
        ScanStatus scanStatus = ScanStatus.IN_PROGRESS;
        PageRequest pageRequest = PageRequest.of(0, 10);

        // When
        fileService.findByFilesStatus(scanStatus, pageRequest);

        // Then
        verify(fileRepositoryMock).findFilesByScanStatus(scanStatus, pageRequest);
    }

    @Test
    public void shouldUpdateFileVisibility() {
        // Given
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        File file = File.builder()
                .id(1L)
                .uuid(uuid)
                .fileVisibility(FileVisibility.PRIVATE)
                .user(user)
                .build();

        given(fileRepositoryMock.findFileByUuidAndUserId(uuid, user.getId())).willReturn(Optional.of(file));

        // When
        RetrieveFileDto retrieveFileDto = fileService.updateFileVisibility(uuid, user.getId(), FileVisibility.PUBLIC);

        // Then
        assertNotNull(retrieveFileDto);
        assertThat(retrieveFileDto.fileVisibility()).isEqualTo(FileVisibility.PUBLIC);
        verify(fileRepositoryMock).findFileByUuidAndUserId(uuid, user.getId());
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingFileVisibilityAndFileDoesntExist() {
        // Given
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");

        given(fileRepositoryMock.findFileByUuidAndUserId(uuid, user.getId())).willReturn(Optional.empty());

        // When
        // Then
        assertThatThrownBy(() -> fileService.updateFileVisibility(uuid, user.getId(), FileVisibility.PUBLIC))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("File with id %s not found".formatted(uuid));
    }
}