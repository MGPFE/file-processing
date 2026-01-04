package org.mg.fileprocessing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class TestUtils {
    public static String getResourceAsString(Path path) {
        try (InputStream is = TestUtils.class.getClassLoader().getResourceAsStream(path.toString())) {
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
