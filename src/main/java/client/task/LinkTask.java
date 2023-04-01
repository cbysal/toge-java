package client.task;

import client.option.OptionPool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LinkTask extends Task {
    private final List<Path> oFiles;
    private final Path target;

    public LinkTask(OptionPool options, List<Path> oFiles, Path target, List<Task> subTasks) {
        super(options, subTasks);
        this.oFiles = oFiles;
        this.target = target;
    }

    @Override
    public void run() {
        super.run();
        List<String> command = new ArrayList<>();
        if (!options.containsKey(OptionPool.CC)) {
            throw new RuntimeException("Link task need a third party linker. Please specify a linker with option: " + "--cc");
        }
        String linker = options.get(OptionPool.CC);
        command.add(linker);
        command.addAll(oFiles.stream().map(Path::toString).toList());
        command.add("-o");
        command.add(target.toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process;
        try {
            process = builder.start();
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Linking failed with error code: " + process.exitValue());
        }
    }
}
