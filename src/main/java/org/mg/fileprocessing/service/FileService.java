package org.mg.fileprocessing.service;

import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    public RetrieveFileDto uploadFile(MultipartFile multipartFile) {
        // TODO handle file save etc.
        return new RetrieveFileDto(UUID.randomUUID(), multipartFile.getName(), multipartFile.getSize());
    }

    public void deleteFile(UUID uuid) {
        fileRepository.deleteFileByUuid(uuid);
    }
}
