package org.mg.fileprocessing.compression;

import java.nio.file.Path;
import java.util.List;

public interface Compressor {
    Path compress(List<Path> paths);
}
