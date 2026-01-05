package org.mg.fileprocessing.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    void saveFileToStorage(MultipartFile multipartFile, String filename);
}
