package org.mg.fileprocessing.repository;

import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.entity.FileVisibility;
import org.mg.fileprocessing.entity.ScanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findFileByUuid(UUID uuid);
    Optional<File> findFileByUuidAndUserId(UUID uuid, Long userId);
    @Query("SELECT f FROM File f WHERE f.uuid = :uuid AND (f.user.id = :userId OR f.fileVisibility = :fileVisibility)")
    Optional<File> findFileByUuidAndUserIdOrFileVisibility(UUID uuid, Long userId, FileVisibility fileVisibility);
    List<File> findFilesByUserIdOrFileVisibility(Long userId, FileVisibility fileVisibility);
    void deleteFileByUuid(UUID uuid);
    Optional<File> findFileByChecksum(String checksum);
    boolean existsByFileStorageName(String fileStorageName);
    Optional<File> findByFileStorageName(String fileStorageName);
    Page<File> findFilesByScanStatus(ScanStatus scanStatus, Pageable pageable);
}
