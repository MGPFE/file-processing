package org.mg.fileprocessing.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScanAnalysisStats(
        int malicious,
        int suspicious,
        int undetected,
        int harmless,
        int timeout,
        @JsonProperty("confirmed-timeout") int confirmedTimeout,
        int failure,
        @JsonProperty("type-unsupported") int typeUnsupported) {
}
