package org.mg.fileprocessing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldReturnListOfFiles() throws Exception {
        // given
        String expected = TestUtils.getResourceAsString(Path.of("test-get-all-files.json"));

        // when
        // then
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected));
    }
}