package org.mg.fileprocessing.compression;

import java.nio.file.Path;

public interface Compressor {
    Path compress(Path path);
}
