package org.mg.fileprocessing.dto;

import lombok.Builder;
import org.springframework.core.io.Resource;

@Builder
public record FileDownloadDto(Resource resource, String filename) {
}
