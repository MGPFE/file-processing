package org.mg.fileprocessing.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mg.fileprocessing.TestUtils.*;

@WebMvcTest(FileController.class)
class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnListOfFiles() throws Exception {
        // Given
        String expected = getResourceAsString(Path.of("test-get-all-files.json"));

        // When
        // Then
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldReturnFileById() throws Exception {
        // Given
        String expected = getResourceAsString(Path.of("test-get-file-by-id.json"));
        String fileUuid = "ab58f6de-9d3a-40d6-b332-11c356078fb5";

        // When
        // Then
        mockMvc.perform(get("/files/%s".formatted(fileUuid)))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    public void shouldCreateNewFile() throws Exception {
        // Given
        String expected = getResourceAsString(Path.of("test-create-new-file.json"));
        String fileUuid = "ab58f6de-9d3a-40d6-b332-11c356078fb5";

        // When
        // Then
        mockMvc.perform(post("/files"))
                .andExpect(status().isCreated())
                .andExpect(header().stringValues("Location", "/%s".formatted(fileUuid)))
                .andExpect(content().json(expected, STRICT));
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