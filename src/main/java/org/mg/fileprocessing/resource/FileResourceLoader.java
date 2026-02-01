package org.mg.fileprocessing.resource;

import org.mg.fileprocessing.exception.FileHandlingException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Component
public class FileResourceLoader implements ResourceLoader {
    @Override
    public Resource loadPathAsResource(Path path) {
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new FileHandlingException("Failed while retrieving file %s".formatted(path), e);
        }
    }
}
