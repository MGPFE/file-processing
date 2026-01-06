package org.mg.fileprocessing.debug;

import org.mg.fileprocessing.entity.File;
import org.mg.fileprocessing.repository.FileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.UUID;

@Profile("dev")
public class DataInitializer {
    @Bean
    public CommandLineRunner commandLineRunner(FileRepository fileRepository) {
        return args -> {
            UUID file1Uuid = UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5");
            UUID file2Uuid = UUID.fromString("36a3a593-bc83-49b7-b7cc-e916a0e0ba9f");

            List<File> files = List.of(
                    File.builder().uuid(file1Uuid).originalFilename("test-file.jpg").fileStorageName("%s-test-file.jpg").size(200L).contentType("image/jpg").checksum("test-checksum").build(),
                    File.builder().uuid(file2Uuid).originalFilename("test-file-2.jpg").fileStorageName("%s-test-file-2.jpg").size(300L).contentType("image/jpg").checksum("test-checksum").build()
            );

            fileRepository.saveAll(files);
        };
    }
}
