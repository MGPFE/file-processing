package org.mg.fileprocessing.service;

import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.dto.CreateFileDto;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.repository.FileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;

    public List<RetrieveFileDto> findAll() {
        return fileRepository.findAll().stream()
                .map(file -> new RetrieveFileDto(file.getUuid(), file.getOriginalFilename(), file.getSize()))
                .toList();
    }

    public RetrieveFileDto findByUuid(UUID uuid) {
        return fileRepository.findFileByUuid(uuid)
                .map(file -> new RetrieveFileDto(file.getUuid(), file.getOriginalFilename(), file.getSize()))
                .orElseThrow(() -> new ResourceNotFoundException("File with UUID %s not found".formatted(uuid)));
    }

    public RetrieveFileDto createFile(CreateFileDto createFileDto) {
        return new RetrieveFileDto(UUID.randomUUID(), createFileDto.filename(), (long) createFileDto.data().length());
    }

    public void deleteFile(UUID uuid) {
        // Pass for now
    }
}
