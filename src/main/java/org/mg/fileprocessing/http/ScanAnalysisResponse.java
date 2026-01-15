package org.mg.fileprocessing.http;

import org.springframework.http.HttpStatusCode;

public record ScanAnalysisResponse(ScanAnalysisStats scanAnalysisStats, HttpStatusCode code) {
}
