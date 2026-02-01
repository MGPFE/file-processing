package org.mg.fileprocessing.resource;

import org.springframework.core.io.Resource;

import java.nio.file.Path;

@FunctionalInterface
public interface ResourceLoader {
    Resource loadPathAsResource(Path path);
}
