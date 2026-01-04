package org.mg.fileprocessing.service;

import org.mg.fileprocessing.dto.CreateFileDto;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    private static final List<RetrieveFileDto> db = List.of(
            new RetrieveFileDto(UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5"), "test", 200L),
            new RetrieveFileDto(UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f"), "test2", 100L)
    );

    public List<RetrieveFileDto> findAll() {
        return db;
    }

    public RetrieveFileDto findByUuid(UUID uuid) {
        return db.stream()
                .filter(retrieveFileDto -> retrieveFileDto.uuid().equals(uuid))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File with UUID %s not found".formatted(uuid)));
    }

    public RetrieveFileDto createFile(CreateFileDto createFileDto) {
        return new RetrieveFileDto(UUID.randomUUID(), createFileDto.filename(), (long) createFileDto.data().length());
    }

    public void deleteFile(UUID uuid) {
        // Pass for now
    }
}
