package org.mg.fileprocessing.repository;

import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.entity.ScanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findFileByUuid(UUID uuid);
    void deleteFileByUuid(UUID uuid);
    Optional<File> findFileByChecksum(String checksum);
    boolean existsByFileStorageName(String fileStorageName);
    Optional<File> findByFileStorageName(String fileStorageName);
    Page<File> findFilesByScanStatus(ScanStatus scanStatus, Pageable pageable);
}
