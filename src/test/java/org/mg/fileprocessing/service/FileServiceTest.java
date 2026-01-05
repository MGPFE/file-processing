package org.mg.fileprocessing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.repository.FileRepository;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// TODO tests here should take into consideration that user can get files that are his own or are public and can delete only his own files
class FileServiceTest {
    @Mock private FileRepository fileRepositoryMock;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileRepositoryMock = Mockito.mock(FileRepository.class);
        fileService = new FileService(fileRepositoryMock);
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
        Throwable throwable = assertThatThrownBy(() -> fileService.findByUuid(uuid)).actual();
        assertThat(throwable).isInstanceOf(ResourceNotFoundException.class);
        assertThat(throwable.getMessage()).isEqualTo("File with UUID %s not found".formatted(uuid));
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
}