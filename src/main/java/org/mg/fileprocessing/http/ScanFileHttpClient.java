package org.mg.fileprocessing.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.HttpClientException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanFileHttpClient {
    private final ScanFileHttpClientProperties scanFileHttpClientProperties;
    private final RestClient restClient;

    private static final String X_API_KEY_HEADER = "x-apikey";
    private static final String FILE_PART = "file";

    public void postFile(Path path) {
        String url;
        try {
            url = buildUrl(Files.size(path));
        } catch (IOException e) {
            throw new FileHandlingException("Failed while reading size of file %s".formatted(path.getFileName()), e);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(FILE_PART, new FileSystemResource(path));

        ResponseEntity<String> responseEntity = restClient.post()
                .uri(url)
                .header(X_API_KEY_HEADER, scanFileHttpClientProperties.getApiKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    log.error("Url {} returned error {} - {}", url, response.getStatusCode(), response.getStatusText());
                    throw new HttpClientException("Failed when connecting to scan endpoint: %s - %s - %s".formatted(url, response.getStatusCode(), response.getStatusText()));
                })
                .toEntity(String.class);

        log.info("Received response with code: {} from {}", responseEntity.getStatusCode(), url);
        log.info("Response: {}", responseEntity.getBody());
    }

    private String buildUrl(long fileSize) {
        return "%s%s".formatted(
                scanFileHttpClientProperties.getUrl(),
                fileSize >= scanFileHttpClientProperties.getBigScanThreshold()
                        ? scanFileHttpClientProperties.getBigScanSuffix()
                        : ""
        );
    }
}
