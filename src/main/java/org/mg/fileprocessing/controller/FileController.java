package org.mg.fileprocessing.controller;

import org.mg.fileprocessing.dto.FileDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileController {
    private static final List<FileDto> db = List.of(
            new FileDto(UUID.fromString("ab58f6de-9d3a-40d6-b332-11c356078fb5"), "test", 200L)
    );

    @GetMapping
    public ResponseEntity<List<FileDto>> findAll() {
        return ResponseEntity.ok(db);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileDto> findById() {
        // TODO it should return 404 when somebody is trying to get file that is not his and is not public
        return ResponseEntity.ok(db.get(0));
    }

    @PostMapping
    public ResponseEntity<FileDto> createFile() {
        // TODO replace the URI with actual id when saving to db
        return ResponseEntity.created(URI.create("/%s".formatted(db.get(0).uuid().toString()))).body(db.get(0));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile() {
        // TODO only owner or admin should be able to delete files
        return ResponseEntity.noContent().build();
    }
}
