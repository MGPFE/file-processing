package org.mg.fileprocessing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mg.fileprocessing.dto.FileDownloadDto;
import org.mg.fileprocessing.dto.RetrieveFileDto;
import org.mg.fileprocessing.dto.UpdateFileVisibilityDto;
import org.mg.fileprocessing.entity.User;
import org.mg.fileprocessing.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private static final String ATTACHMENT_HEADER_FORMAT = "attachment; filename=\"%s\"";

    @GetMapping
    public ResponseEntity<List<RetrieveFileDto>> findAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.findAll(user.getId()));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<RetrieveFileDto> findById(@PathVariable("uuid") UUID uuid, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.findByUuid(uuid, user.getId()));
    }

    @GetMapping("/download/{uuid}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("uuid") UUID uuid, @AuthenticationPrincipal User user) {
        FileDownloadDto fileDownloadDto = fileService.downloadFile(uuid, user.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ATTACHMENT_HEADER_FORMAT.formatted(fileDownloadDto.filename()))
                .body(fileDownloadDto.resource());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RetrieveFileDto> uploadFile(@RequestPart("file") MultipartFile multipartFile, @AuthenticationPrincipal User user) {
        RetrieveFileDto retrieveFileDto = fileService.uploadFile(multipartFile, user);
        return ResponseEntity.created(URI.create("/%s".formatted(retrieveFileDto.uuid()))).body(retrieveFileDto);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteFile(@PathVariable("uuid") UUID uuid, @AuthenticationPrincipal User user) {
        fileService.deleteFile(uuid, user.getId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<RetrieveFileDto> updateFileVisibility(@PathVariable("uuid") UUID uuid,
                                                                @AuthenticationPrincipal User user,
                                                                @Valid @RequestBody UpdateFileVisibilityDto updateFileVisibilityDto) {
        return ResponseEntity.ok(fileService.updateFileVisibility(uuid, user.getId(), updateFileVisibilityDto.fileVisibility()));
    }
}
