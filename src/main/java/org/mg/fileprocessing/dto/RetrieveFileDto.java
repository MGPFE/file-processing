package org.mg.fileprocessing.dto;

import lombok.Builder;
import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.entity.FileVisibility;

import java.util.UUID;

@Builder
public record RetrieveFileDto(
        UUID uuid,
        String filename,
        Long size,
        String checksum,
        String contentType,
        FileVisibility fileVisibility
) {
    public static RetrieveFileDto fromFile(File file) {
        return RetrieveFileDto.builder()
                .uuid(file.getUuid())
                .filename(file.getOriginalFilename())
                .size(file.getSize())
                .checksum(file.getChecksum())
                .contentType(file.getContentType())
                .fileVisibility(file.getFileVisibility())
                .build();
    }
}
