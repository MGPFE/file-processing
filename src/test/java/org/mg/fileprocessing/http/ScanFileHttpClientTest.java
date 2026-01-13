package org.mg.fileprocessing.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.HttpClientException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ScanFileHttpClientTest {
    private ScanFileHttpClient scanFileHttpClient;
    private ScanFileHttpClientProperties scanFileHttpClientProperties;
    private MockRestServiceServer mockRestServiceServer;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        scanFileHttpClientProperties = new ScanFileHttpClientProperties("https://test.url", "ABCDEF", 1024L, "/bigScan");
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        scanFileHttpClient = new ScanFileHttpClient(scanFileHttpClientProperties, restClientBuilder.build());
    }

    @Test
    public void shouldSendSmallFile() throws IOException {
        // Given
        Path file = tempDir.resolve("small-file.txt");
        Files.writeString(file, "Testing");

        mockRestServiceServer.expect(requestTo("https://test.url"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-apikey", "ABCDEF"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(header("accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        // When
        scanFileHttpClient.postFile(file);

        // Then
        mockRestServiceServer.verify();
    }

    @Test
    public void shouldSendBigFile() throws IOException {
        // Given
        Path file = tempDir.resolve("big-file.txt");
        Files.write(file, new byte[2048]);

        mockRestServiceServer.expect(requestTo("https://test.url/bigScan"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-apikey", "ABCDEF"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(header("accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        // When
        scanFileHttpClient.postFile(file);

        // Then
        mockRestServiceServer.verify();
    }

    @Test
    public void shouldSendEdgeFile() throws IOException {
        // Given
        Path file = tempDir.resolve("edge-file.txt");
        Files.write(file, new byte[1024]);

        mockRestServiceServer.expect(requestTo("https://test.url/bigScan"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-apikey", "ABCDEF"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(header("accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        // When
        scanFileHttpClient.postFile(file);

        // Then
        mockRestServiceServer.verify();
    }

    @Test
    public void shouldThrowExceptionWhenFileDoesntExistDuringRead() {
        // Given
        Path file = tempDir.resolve("non-existent");

        // When
        // Then
        assertThatThrownBy(() -> scanFileHttpClient.postFile(file))
                .isInstanceOf(FileHandlingException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessage("Failed while reading size of file non-existent");
    }

    @Test
    public void shouldThrowExceptionWhenConnectionToScanEndpointFailed() throws IOException {
        // Given
        String url = "https://test.url/bigScan";
        Path file = tempDir.resolve("big-file.txt");
        Files.write(file, new byte[2048]);

        mockRestServiceServer.expect(requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-apikey", "ABCDEF"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(header("accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withGatewayTimeout());

        // When
        // Then
        assertThatThrownBy(() -> scanFileHttpClient.postFile(file))
                .isInstanceOf(HttpClientException.class)
                .hasMessage("Failed when connecting to scan endpoint: %s - %s - %s".formatted(url, "504 GATEWAY_TIMEOUT", "Gateway Timeout"));
        mockRestServiceServer.verify();
    }
}