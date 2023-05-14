package client.task;

import client.option.OptionPool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AssembleTask extends Task {
    private final Path sFile, oFile;

    public AssembleTask(OptionPool options, Path sFile, Path oFile) {
        super(options);
        this.sFile = sFile;
        this.oFile = oFile;
    }

    public AssembleTask(OptionPool options, Path sFile, Path oFile, Task subTask) {
        super(options, subTask);
        this.sFile = sFile;
        this.oFile = oFile;
    }

    @Override
    public void run() {
        super.run();
        if (!options.containsKey(OptionPool.CC)) {
            throw new RuntimeException("No assembler! Please specify a assembler with option: --cc");
        }
        String assembler = options.get(OptionPool.CC);
        List<String> command = List.of(assembler, "-c", sFile.toString(), "-o", oFile.toString());
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
