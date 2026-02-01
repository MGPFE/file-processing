package org.mg.fileprocessing.dto;

import org.springframework.core.io.Resource;

public record FileDownloadDto(Resource resource, String filename) {
}
