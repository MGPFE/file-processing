package org.mg.fileprocessing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mg.fileprocessing.checksum.ChecksumUtil;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.exception.UnsupportedContentTypeException;
import org.mg.fileprocessing.repository.FileRepository;
import org.mg.fileprocessing.storage.FileStorage;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// TODO tests here should take into consideration that user can get files that are his own or are public and can delete only his own files
class FileServiceTest {
    @Mock private FileRepository fileRepositoryMock;
    @Mock private ChecksumUtil checksumUtil;
    @Mock private FileStorage fileStorage;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        FileUploadProperties fileUploadProperties = new FileUploadProperties();

        fileRepositoryMock = Mockito.mock(FileRepository.class);
        checksumUtil = Mockito.mock(ChecksumUtil.class);
        fileStorage = Mockito.mock(FileStorage.class);

        fileService = new FileService(fileStorage, checksumUtil, fileUploadProperties, fileRepositoryMock);
    }

    @Test
    public void shouldFindFileByUuid() {
        // Given
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        File file = File.builder()
                .originalFilename("test-file")
                .uuid(uuid)
                .size(100L)
                .build();

        given(fileRepositoryMock.findFileByUuid(uuid)).willReturn(Optional.of(file));

        // When
        RetrieveFileDto result = fileService.findByUuid(uuid);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.uuid()).isEqualTo(uuid);
        assertThat(result.filename()).isEqualTo(file.getOriginalFilename());
        assertThat(result.size()).isEqualTo(file.getSize());
        verify(fileRepositoryMock, times(1)).findFileByUuid(uuid);
    }

    @Test
    public void shouldThrowExceptionWhenFileNotFoundByUuid() {
        // Given
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        given(fileRepositoryMock.findFileByUuid(uuid)).willReturn(Optional.empty());

        // When
        // Then
        assertThatThrownBy(() -> fileService.findByUuid(uuid))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("File with UUID %s not found".formatted(uuid));
        verify(fileRepositoryMock, times(1)).findFileByUuid(uuid);
    }

    @Test
    public void shouldFindAllFiles() {
        // Given
        UUID uuid = UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f");
        File file = File.builder()
                .originalFilename("test-file")
                .uuid(uuid)
                .size(100L)
                .build();
        RetrieveFileDto retrieveFileDto = new RetrieveFileDto(uuid, file.getOriginalFilename(), file.getSize());

        UUID uuid2 = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        File file2 = File.builder()
                .originalFilename("test-file2")
                .uuid(uuid2)
                .size(200L)
                .build();
        RetrieveFileDto retrieveFileDto2 = new RetrieveFileDto(uuid2, file2.getOriginalFilename(), file2.getSize());

        given(fileRepositoryMock.findAll()).willReturn(List.of(file, file2));

        // When
        List<RetrieveFileDto> result = fileService.findAll();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);
        assertThat(result).containsAll(List.of(retrieveFileDto, retrieveFileDto2));
        verify(fileRepositoryMock, times(1)).findAll();
    }

    @Test
    public void shouldReturnEmptyListWhenNoFilesFound() {
        // Given
        given(fileRepositoryMock.findAll()).willReturn(Collections.emptyList());

        // When
        List<RetrieveFileDto> result = fileService.findAll();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(fileRepositoryMock, times(1)).findAll();
    }

    @Test
    public void shouldDeleteFileByUuid() {
        // Given
        UUID uuid = UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f");

        willDoNothing().given(fileRepositoryMock).deleteFileByUuid(uuid);

        // When
        fileService.deleteFile(uuid);

        // Then
        verify(fileRepositoryMock, times(1)).deleteFileByUuid(uuid);
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
        willDoNothing().given(fileStorage).saveFileToStorage(multipartFile, fileStorageName);

        // When
        RetrieveFileDto result = fileService.uploadFile(multipartFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.size()).isEqualTo(5);
        verify(fileRepositoryMock, times(1)).save(any(File.class));
        verify(checksumUtil, times(1)).getChecksumAsString(any(InputStream.class));
        verify(fileStorage, times(1)).saveFileToStorage(multipartFile, fileStorageName);
    }

    @Test
    public void shouldSaveFileWhenAllowedContentTypesSpecified() throws IOException {
        // Given
        FileUploadProperties fileUploadProperties = new FileUploadProperties(List.of("image/jpg"));
        fileService = new FileService(fileStorage, checksumUtil, fileUploadProperties, fileRepositoryMock);
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
        willDoNothing().given(fileStorage).saveFileToStorage(multipartFile, fileStorageName);

        // When
        RetrieveFileDto result = fileService.uploadFile(multipartFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo(filename);
        assertThat(result.size()).isEqualTo(5);
        verify(fileRepositoryMock, times(1)).save(any(File.class));
        verify(checksumUtil, times(1)).getChecksumAsString(any(InputStream.class));
        verify(fileStorage, times(1)).saveFileToStorage(multipartFile, fileStorageName);
    }

    @Test
    public void shouldThrowExceptionWhenContentTypeNotAllowed() throws IOException {
        // Given
        FileUploadProperties fileUploadProperties = new FileUploadProperties(List.of("image/jpg"));
        fileService = new FileService(fileStorage, checksumUtil, fileUploadProperties, fileRepositoryMock);

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
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile))
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
        assertThatThrownBy(() -> fileService.uploadFile(multipartFile))
                .isInstanceOf(FileHandlingException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasRootCauseMessage(ioExceptionMessage)
                .hasMessage("Failed while streaming from file");
    }
}