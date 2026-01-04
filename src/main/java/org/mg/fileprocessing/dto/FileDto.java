package org.mg.fileprocessing.dto;

import java.util.UUID;

public record FileDto(
        UUID uuid,
        String filename,
        Long size
) {
}
