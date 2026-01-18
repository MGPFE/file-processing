package org.mg.fileprocessing.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(matchIfMissing = true, name = "storage.strategy", havingValue = "local")
public class LocalFileStorage implements FileStorage {
    private final FileStorageProperties fileStorageProperties;

    @Override
    public Path saveFileToStorage(MultipartFile multipartFile, String filename) {
        Path destinationPath = resolvePath(filename);

        if (!destinationPath.getParent().equals(fileStorageProperties.getPath().toAbsolutePath())) {
            throw new FileHandlingException("Cannot store file outside current directory");
        }

        try (InputStream is = multipartFile.getInputStream()) {
            if (!Files.exists(fileStorageProperties.getPath())) Files.createDirectories(fileStorageProperties.getPath());

            Files.copy(is, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileHandlingException("Failed to store a file", e);
        }

        return destinationPath;
    }

    @Override
    public Path getFilePathFromStorage(String filename) {
        return resolvePath(filename);
    }

    private Path resolvePath(String filename) {
        return fileStorageProperties.getPath()
                .resolve(Path.of(filename))
                .normalize()
                .toAbsolutePath();
    }

    @Override
    public void deleteFileFromStorage(String filename) {
        Path destinationPath = resolvePath(filename);

        try {
            Files.delete(destinationPath);
        } catch (IOException e) {
            log.error("Failed while deleting file {}", destinationPath);
        }
    }
}
