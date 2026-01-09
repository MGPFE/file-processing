package org.mg.fileprocessing.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.checksum.ChecksumUtil;
import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.exception.UnsupportedContentTypeException;
import org.mg.fileprocessing.repository.FileRepository;
import org.mg.fileprocessing.storage.FileStorage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileStorage fileStorage;
    private final ChecksumUtil checksumUtil;
    private final FileUploadProperties fileUploadProperties;
    private final FileRepository fileRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

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

    @Transactional
    public RetrieveFileDto uploadFile(MultipartFile multipartFile) {
        validateFileLength(multipartFile);
        String checksum = getChecksumForFile(multipartFile);

        return fileRepository.findFileByChecksum(checksum)
                .map(RetrieveFileDto::fromFile)
                .orElseGet(() -> saveNewFile(multipartFile, checksum));
    }

    private RetrieveFileDto saveNewFile(MultipartFile multipartFile, String checksum) {
        String contentType = multipartFile.getContentType();
        validateContentType(contentType);

        String originalFilename = multipartFile.getOriginalFilename();
        String fileStorageName = generateFileStorageName(originalFilename, checksum);
        long size = multipartFile.getSize();

        File file = File.builder()
                .uuid(UUID.randomUUID())
                .checksum(checksum)
                .originalFilename(originalFilename)
                .fileStorageName(fileStorageName)
                .contentType(contentType)
                .size(size)
                .build();

        fileRepository.save(file);
        fileStorage.saveFileToStorage(multipartFile, fileStorageName);

        kafkaTemplate.send("file.upload.scan", fileStorageName)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("KAFKA ERROR: Message failed to send!", ex);
                    } else {
                        log.info("KAFKA SUCCESS: Message sent to partition {}", result.getRecordMetadata().partition());
                    }
                });

        return RetrieveFileDto.fromFile(file);
    }

    private void validateContentType(String contentType) {
        if (!fileUploadProperties.getAllowedContentTypes().isEmpty()
                && !fileUploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw new UnsupportedContentTypeException("Content type %s is not supported".formatted(contentType));
        }
    }

    private String getChecksumForFile(MultipartFile multipartFile) {
        String checksum;
        try (InputStream is = multipartFile.getInputStream()) {
            checksum = checksumUtil.getChecksumAsString(is);
        } catch (IOException e) {
            throw new FileHandlingException("Failed while streaming from file", e);
        }

        return checksum;
    }

    private String generateFileStorageName(String originalFilename, String checksum) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return checksum;
        } else {
            return "%s-%s".formatted(checksum, originalFilename);
        }
    }

    private void validateFileLength(MultipartFile multipartFile) {
        if (multipartFile.getSize() < 1L)
            throw new FileHandlingException("Cannot upload file smaller than 1 byte");
    }

    @Transactional
    public void deleteFile(UUID uuid) {
        fileRepository.deleteFileByUuid(uuid);
    }
}
