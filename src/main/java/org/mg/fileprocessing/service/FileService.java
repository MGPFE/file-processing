package org.mg.fileprocessing.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.checksum.ChecksumUtil;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.entity.FileVisibility;
import org.mg.fileprocessing.entity.ScanStatus;
import org.mg.fileprocessing.entity.User;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.exception.UnsupportedContentTypeException;
import org.mg.fileprocessing.repository.FileRepository;
import org.mg.fileprocessing.storage.FileStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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

    public List<RetrieveFileDto> findAll(Long userId) {
        return fileRepository.findFilesByUserIdOrFileVisibility(userId, FileVisibility.PUBLIC).stream()
                .map(RetrieveFileDto::fromFile)
                .toList();
    }

    public RetrieveFileDto findByUuid(UUID uuid, Long userId) {
        return fileRepository.findFileByUuidAndUserId(uuid, userId)
                .map(RetrieveFileDto::fromFile)
                .orElseThrow(() -> new ResourceNotFoundException("File with UUID %s not found".formatted(uuid)));
    }

    public Page<File> findByFilesStatus(ScanStatus scanStatus, Pageable pageable) {
        return fileRepository.findFilesByScanStatus(scanStatus, pageable);
    }

    @Transactional
    public RetrieveFileDto uploadFile(MultipartFile multipartFile, User user) {
        validateFileLength(multipartFile);
        String checksum = getChecksumForFile(multipartFile);

        // TODO we should probably add the user that posted it as co-owner
        return fileRepository.findFileByChecksum(checksum)
                .map(RetrieveFileDto::fromFile)
                .orElseGet(() -> saveNewFile(multipartFile, checksum, user));
    }

    private RetrieveFileDto saveNewFile(MultipartFile multipartFile, String checksum, User user) {
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
                .scanStatus(ScanStatus.NOT_STARTED)
                .user(user)
                .fileVisibility(FileVisibility.PRIVATE)
                .build();

        fileRepository.save(file);
        Path path = fileStorage.saveFileToStorage(multipartFile, fileStorageName);

        requestScan(path);

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

    public void requestScan(Path path) {
        kafkaTemplate.send("file.upload.scan", path.toString())
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("KAFKA ERROR: Message failed to send!", ex);
                    } else {
                        log.info("KAFKA SUCCESS: Message sent to partition {}", result.getRecordMetadata().partition());
                    }
                });
    }

    @Transactional
    // This method is not exposed to user
    public void deleteFile(String filename) {
        fileRepository.findByFileStorageName(filename)
                .ifPresent(this::deleteFile);
    }

    @Transactional
    public void deleteFile(UUID uuid, Long userId) {
        fileRepository.findFileByUuidAndUserId(uuid, userId)
                .ifPresent(this::deleteFile);
    }

    private void deleteFile(File file) {
        fileRepository.deleteFileByUuid(file.getUuid());
        fileStorage.deleteFileFromStorage(file.getFileStorageName());
    }

    @Transactional
    public void handleFileStatusUpdate(Path fileStorageName, ScanStatus scanStatus) {
        File dbFile = fileRepository.findByFileStorageName(fileStorageName.getFileName().toString())
                .orElseThrow(() -> new FileHandlingException("File %s not found in storage".formatted(fileStorageName)));

        dbFile.setScanStatus(scanStatus);
    }

    @Transactional
    public RetrieveFileDto updateFileVisibility(UUID fileId, Long userId, FileVisibility fileVisibility) {
        File dbFile = fileRepository.findFileByUuidAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File with id %s not found".formatted(fileId)));

        dbFile.setFileVisibility(fileVisibility);

        return RetrieveFileDto.fromFile(dbFile);
    }
}
