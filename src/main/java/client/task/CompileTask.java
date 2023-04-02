package client.task;

import client.option.OptionPool;
import compile.Compiler;

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
        // TODO Remove third party compiler when Compiler is complete.
        if (options.containsKey(OptionPool.CC)) {
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
            return;
        }
        Compiler compiler = new Compiler(options, iFile, sFile);
        compiler.compile();
    }
}
