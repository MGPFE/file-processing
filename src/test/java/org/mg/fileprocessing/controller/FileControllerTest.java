package org.mg.fileprocessing.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.entity.User;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.HttpClientException;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.exception.UnsupportedContentTypeException;
import org.mg.fileprocessing.security.auth.SecurityConfig;
import org.mg.fileprocessing.security.auth.UserRole;
import org.mg.fileprocessing.security.auth.jwt.JwtUtil;
import org.mg.fileprocessing.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mg.fileprocessing.TestUtils.*;

@WebMvcTest(FileController.class)
@Import(SecurityConfig.class)
class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private FileService fileService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;

    private User testUser;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public Clock clock() {
            final Instant fixedNow = Instant.parse("2026-05-20T12:00:00Z");
            return Clock.fixed(fixedNow, ZoneId.of("UTC"));
        }
    }

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@email.com")
                .password("testpassword")
                .roles(List.of(UserRole.USER))
                .build();
    }

    @Test
    public void shouldReturn403WhenGetWithoutUser() throws Exception {
        // Given
        // When
        // Then
        mockMvc.perform(get("/files"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldReturn403WhenPostWithoutUser() throws Exception {
        // Given
        // When
        // Then
        mockMvc.perform(post("/files"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldReturnListOfFiles() throws Exception {
        // Given
        String expected = getResourceAsString(Path.of("test-get-all-files.json"));

        given(fileService.findAll(anyLong())).willReturn(List.of(
                RetrieveFileDto.builder()
                        .uuid(UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5")).filename("test2").size(100L).build(),
                RetrieveFileDto.builder()
                        .uuid(UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f")).filename("test2").size(100L).build()
        ));

        // When
        // Then
        mockMvc.perform(get("/files")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldReturnEmptyListIfNoFiles() throws Exception {
        // Given
        String expected = "[]";

        given(fileService.findAll(anyLong())).willReturn(List.of());

        // When
        // Then
        mockMvc.perform(get("/files")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldReturnFileByUuid() throws Exception {
        // Given
        String expected = getResourceAsString(Path.of("test-get-file-by-uuid.json"));
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");

        given(fileService.findByUuid(eq(uuid), anyLong())).willReturn(RetrieveFileDto.builder()
                .uuid(UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5")).filename("test").size(200L).build());

        // When
        // Then
        mockMvc.perform(get("/files/%s".formatted(uuid))
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldReturn404WhenFileNotFoundByUuid() throws Exception {
        // Given
        UUID uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
        String reason = "File with UUID %s not found".formatted(uuid);

        given(fileService.findByUuid(eq(uuid), anyLong())).willThrow(new ResourceNotFoundException(reason));

        // When
        // Then
        mockMvc.perform(get("/files/%s".formatted(uuid))
                        .with(user(testUser)))
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

        given(fileService.uploadFile(eq(multipartFile), any(User.class))).willReturn(RetrieveFileDto.builder()
                .uuid(uuid).filename(filename).size((long) data.length).build());

        // When
        // Then
        mockMvc.perform(
                    multipart("/files")
                            .file(multipartFile)
                            .with(user(testUser))
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

        given(fileService.uploadFile(eq(multipartFile), any(User.class))).willThrow(new UnsupportedContentTypeException(reason));

        // When
        // Then
        mockMvc.perform(
                        multipart("/files")
                                .file(multipartFile)
                                .with(user(testUser))
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

        given(fileService.uploadFile(eq(multipartFile), any(User.class))).willThrow(new FileHandlingException(reason));

        // When
        // Then
        mockMvc.perform(
                        multipart("/files")
                                .file(multipartFile)
                                .with(user(testUser))
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

        given(fileService.uploadFile(eq(multipartFile), any(User.class))).willThrow(new HttpClientException(reason));

        // When
        // Then
        mockMvc.perform(
                        multipart("/files")
                                .file(multipartFile)
                                .with(user(testUser))
                ).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.reason").value(reason))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    public void shouldReturn204WhenDeletingContent() throws Exception {
        // Given
        String fileUuid = "ab58f6de-9d3a-40d6-b332-11c356078fb5";

        // When
        // Then
        mockMvc.perform(delete("/files/%s".formatted(fileUuid)).with(user(testUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturn500WhenUnknownException() throws Exception {
        // Given
        String reason = "Server encountered an unexpected exception";
        String filename = "test-file";
        byte[] data = "test-data".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                data
        );

        given(fileService.uploadFile(eq(multipartFile), any(User.class))).willThrow(new RuntimeException());

        // When
        // Then
        mockMvc.perform(
                        multipart("/files")
                                .file(multipartFile)
                                .with(user(testUser))
                ).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.reason").value(reason))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}