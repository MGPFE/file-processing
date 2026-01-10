package org.mg.fileprocessing.compression;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZipCompressor implements Compressor {
    private final CompressorProperties compressorProperties;
    private final Clock clock;

    @Override
    // TODO check if compression doesn't break files, add tests
    public Path compress(List<Path> paths) {
        Path compressedFile = compressorProperties.getPath().resolve("%d-%s.zip".formatted(Instant.now(clock).toEpochMilli(), UUID.randomUUID()));

        try (final FileOutputStream fileOutputStream = new FileOutputStream(compressedFile.toFile()); ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            for (Path path : paths) {
                File fileToZip = path.toFile();
                try (FileInputStream fileInputStream = new FileInputStream(fileToZip)) {
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipOutputStream.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fileInputStream.read()) >= 0) {
                        zipOutputStream.write(bytes, 0, length);
                    }
                }
                log.info("Successfully compressed file: {}", compressedFile);
            }
        } catch (IOException e) {
            throw new FileHandlingException("Failed while zipping files for scan", e);
        }

        return compressedFile;
    }
}
