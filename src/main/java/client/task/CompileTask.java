package client.task;

import client.option.OptionPool;
import compile.Compiler;

import java.nio.file.Path;

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
        Compiler compiler = new Compiler(options, iFile, sFile);
        compiler.compile();
    }
}
