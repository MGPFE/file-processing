package org.mg.fileprocessing.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FilePostResult(String self) {
}
