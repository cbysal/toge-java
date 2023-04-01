package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public final class FileUtil {
    private FileUtil() {
    }

    // TODO makeTempFile will take up the temp file, using mem-file to replace this method.
    public static Path makeTempFile(String prefix, String suffix) {
        try {
            File file = File.createTempFile(prefix, suffix);
            file.deleteOnExit();
            return file.toPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
