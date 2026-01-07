package org.mg.fileprocessing.dto;

import org.mg.fileprocessing.entity.File;

import java.util.UUID;

public record RetrieveFileDto(
        UUID uuid,
        String filename,
        Long size
) {
    public static RetrieveFileDto fromFile(File file) {
        return new RetrieveFileDto(file.getUuid(), file.getOriginalFilename(), file.getSize());
    }
}
