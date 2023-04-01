package client.task;

import client.option.OptionPool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PreprocessTask extends Task {
    private final Path cFile, iFile;

    public PreprocessTask(OptionPool options, Path cFile, Path iFile) {
        super(options);
        this.cFile = cFile;
        this.iFile = iFile;
    }

    @Override
    public void run() {
        super.run();
        // TODO Third party preprocessor should be replaced.
        if (!options.containsKey(OptionPool.CC)) {
            throw new RuntimeException("Preprocess task need a third party preprocessor. Please specify a " +
                    "preprocessor with option: --cc");
        }
        String preprocessor = options.get(OptionPool.CC);
        List<String> command = List.of(preprocessor, "-E", cFile.toString(), "-o", iFile.toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process;
        try {
            process = builder.start();
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Assembling failed with error code: " + process.exitValue());
        }
    }
}
