package org.mg.fileprocessing.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.HttpClientException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public ResponseEntity<String> postFile(Path path) {
        String url;
        try {
            url = buildUrl(Files.size(path));
        } catch (IOException e) {
            throw new FileHandlingException("Failed while reading size of file %s".formatted(path.getFileName()), e);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(FILE_PART, new FileSystemResource(path));

        ResponseEntity<String> responseEntity = executeRequest(getBaseRequest(HttpMethod.POST, url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body));

        if (log.isDebugEnabled()) {
            logResponse(responseEntity, url);
        }
        return responseEntity;
    }

    public ResponseEntity<String> getAnalysis(String analysisUrl) {
        ResponseEntity<String> responseEntity = executeRequest(getBaseRequest(HttpMethod.GET, analysisUrl));
        if (log.isDebugEnabled()) {
            logResponse(responseEntity, analysisUrl);
        }
        return responseEntity;
    }

    private String buildUrl(long fileSize) {
        return "%s%s".formatted(
                scanFileHttpClientProperties.getScanUrl(),
                fileSize >= scanFileHttpClientProperties.getBigScanThreshold()
                        ? scanFileHttpClientProperties.getBigScanSuffix()
                        : ""
        );
    }

    private RestClient.RequestBodySpec getBaseRequest(HttpMethod method, String url) {
        return restClient.method(method)
                .uri(url)
                .header(X_API_KEY_HEADER, scanFileHttpClientProperties.getApiKey())
                .accept(MediaType.APPLICATION_JSON);
    }

    private ResponseEntity<String> executeRequest(RestClient.RequestBodySpec request) {
        return request.retrieve()
                .onStatus(HttpStatusCode::isError, (rq, rs) -> {
                    log.error("Url {} returned error {} - {}", rq.getURI(), rs.getStatusCode(), rs.getStatusText());
                    throw new HttpClientException("Failed when connecting to endpoint: %s - %s - %s".formatted(rq.getURI(), rs.getStatusCode(), rs.getStatusText()));
                })
                .toEntity(String.class);
    }

    private void logResponse(ResponseEntity<String> responseEntity, String url) {
        log.debug("Received response with code: {} from {}", responseEntity.getStatusCode(), url);
        log.debug("Response: {}", responseEntity.getBody());
    }
}
