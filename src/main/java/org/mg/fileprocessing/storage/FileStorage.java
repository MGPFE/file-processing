package org.mg.fileprocessing.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorage {
    Path saveFileToStorage(MultipartFile multipartFile, String filename);
}
