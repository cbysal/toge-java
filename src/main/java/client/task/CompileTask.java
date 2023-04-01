package client.task;

import client.option.OptionPool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CompileTask extends Task {
    private final Path iFile, sFile;

    public CompileTask(OptionPool options, Path iFile, Path sFile) {
        super(options);
        this.iFile = iFile;
        this.sFile = sFile;
    }

    public CompileTask(OptionPool options, Path iFile, Path sFile, Task subTask) {
        super(options, subTask);
        this.iFile = iFile;
        this.sFile = sFile;
    }

    @Override
    public void run() {
        super.run();
        // TODO Third party compiler should be replaced.
        if (!options.containsKey(OptionPool.CC)) {
            throw new RuntimeException("Compile task need a third party compiler. Please specify a compiler with " +
                    "option: --cc");
        }
        String assembler = options.get(OptionPool.CC);
        List<String> command = List.of(assembler, "-S", iFile.toString(), "-o", sFile.toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process;
        try {
            process = builder.start();
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Compiling failed with error code: " + process.exitValue());
        }
    }
}
