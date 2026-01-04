package org.mg.fileprocessing.dto;

import java.util.UUID;

public record RetrieveFileDto(
        UUID uuid,
        String filename,
        Long size
) {
}
