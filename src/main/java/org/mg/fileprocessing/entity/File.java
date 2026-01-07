package org.mg.fileprocessing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "files",
        indexes = {
                @Index(name = "idx_file_storage_name", columnList = "file_storage_name")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, updatable = false)
    private UUID uuid;
    private String originalFilename;
    @Column(name="file_storage_name", unique = true, nullable = false)
    private String fileStorageName;
    private Long size;
    private String contentType;
    private String checksum;
}
