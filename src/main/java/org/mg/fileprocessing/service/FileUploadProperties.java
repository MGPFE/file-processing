package org.mg.fileprocessing.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "upload")
@Configuration
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadProperties {
    private List<String> allowedContentTypes = new ArrayList<>();
}
