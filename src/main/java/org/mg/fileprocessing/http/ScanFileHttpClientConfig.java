package org.mg.fileprocessing.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ScanFileHttpClientConfig {
    @Bean
    public RestClient restClient(RestClient.Builder restClientBuilder) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
