package org.mg.fileprocessing.controller;

import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.service.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @GetMapping
    public ResponseEntity<List<RetrieveFileDto>> findAll() {
        return ResponseEntity.ok(fileService.findAll());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<RetrieveFileDto> findById(@PathVariable("uuid") UUID uuid) {
        // TODO it should return 404 when somebody is trying to get file that is not his and is not public
        return ResponseEntity.ok(fileService.findByUuid(uuid));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RetrieveFileDto> uploadFile(@RequestPart("file") MultipartFile multipartFile) {
        RetrieveFileDto retrieveFileDto = fileService.uploadFile(multipartFile);
        return ResponseEntity.created(URI.create("/%s".formatted(retrieveFileDto.uuid()))).body(retrieveFileDto);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteFile(@PathVariable("uuid") UUID uuid) {
        // TODO only owner or admin should be able to delete files
        fileService.deleteFile(uuid);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
