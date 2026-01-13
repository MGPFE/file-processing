package org.mg.fileprocessing.controller;

import org.junit.jupiter.api.Test;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.HttpClientException;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.exception.UnsupportedContentTypeException;
import org.mg.fileprocessing.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mg.fileprocessing.TestUtils.*;

@WebMvcTest(FileController.class)
class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileService fileService;

    @Test
    public void shouldReturnListOfFiles() throws Exception {
        // Given
        String expected = getResourceAsString(Path.of("test-get-all-files.json"));

        given(fileService.findAll()).willReturn(List.of(
                new RetrieveFileDto(UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5"), "test", 200L),
                new RetrieveFileDto(UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f"), "test2", 100L)
        ));

        // When
        // Then
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldReturnEmptyListIfNoFiles() throws Exception {
        // Given
        String expected = "[]";

        given(fileService.findAll()).willReturn(List.of());

        // When
        // Then
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldReturnFileByUuid() throws Exception {
        // Given
        String expected = getResourceAsString(Path.of("test-get-file-by-uuid.json"));
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");

        given(fileService.findByUuid(uuid)).willReturn(new RetrieveFileDto(UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5"), "test", 200L));

        // When
        // Then
        mockMvc.perform(get("/files/%s".formatted(uuid)))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldReturn404WhenFileNotFoundByUuid() throws Exception {
        // Given
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        String reason = "File with UUID %s not found".formatted(uuid);

        given(fileService.findByUuid(uuid)).willThrow(new ResourceNotFoundException(reason));

        // When
        // Then
        mockMvc.perform(get("/files/%s".formatted(uuid)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.reason").value(reason))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    public void shouldCreateNewFile() throws Exception {
        // Given
        String filename = "test-file";
        byte[] data = "test-data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                data
        );

        String expected = getResourceAsString(Path.of("test-create-new-file.json"));
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");

        given(fileService.uploadFile(multipartFile)).willReturn(new RetrieveFileDto(uuid, filename, (long) data.length));

        // When
        // Then
        mockMvc.perform(
                    multipart("/files")
                            .file(multipartFile)
                ).andExpect(status().isCreated())
                .andExpect(header().stringValues("Location", "/%s".formatted(uuid)))
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldReturn400WhenUnsupportedContentTypeDuringFileUpload() throws Exception {
        // Given
        String reason = "Unsupported content type";
        String filename = "test-file";
        byte[] data = "test-data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                data
        );

        given(fileService.uploadFile(multipartFile)).willThrow(new UnsupportedContentTypeException(reason));

        // When
        // Then
        mockMvc.perform(
                        multipart("/files")
                                .file(multipartFile)
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.reason").value(reason))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    public void shouldReturn500WhenServerEncountersIssueDuringFileUpload() throws Exception {
        // Given
        String reason = "Internal server error";
        String filename = "test-file";
        byte[] data = "test-data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                data
        );

        given(fileService.uploadFile(multipartFile)).willThrow(new FileHandlingException(reason));

        // When
        // Then
        mockMvc.perform(
                        multipart("/files")
                                .file(multipartFile)
                ).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.reason").value(reason))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    public void shouldReturn500WhenHttpClientException() throws Exception {
        // Given
        String reason = "Internal server error";
        String filename = "test-file";
        byte[] data = "test-data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                data
        );

        given(fileService.uploadFile(multipartFile)).willThrow(new HttpClientException(reason));

        // When
        // Then
        mockMvc.perform(
                        multipart("/files")
                                .file(multipartFile)
                ).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.reason").value(reason))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    public void shouldDeleteFile() throws Exception {
        // Given
        String fileUuid = "ab58f6de-9d3a-40d6-b332-11c356078fb5";

        // When
        // Then
        mockMvc.perform(delete("/files/%s".formatted(fileUuid)))
                .andExpect(status().isNoContent());
    }
}