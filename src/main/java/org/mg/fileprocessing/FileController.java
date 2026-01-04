package org.mg.fileprocessing;

import org.mg.fileprocessing.dto.FileDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {
    @GetMapping
    public ResponseEntity<List<FileDto>> findAll() {

    }
}
