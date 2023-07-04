package client;

import client.option.OptionPool;
import client.task.CompileTask;
import client.task.PreprocessTask;
import client.task.Task;
import utils.FileUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TaskParser {
    private static final String C_SUFFIX = ".c";
    private static final String SY_SUFFIX = ".sy";
    private static final String I_SUFFIX = ".i";

    private boolean isProcessed;
    private final String[] args;
    private int compileLevel = 0;
    private final List<Path> sources = new ArrayList<>();
    private Path target;
    private Task task;
    private final OptionPool options = new OptionPool();

    public TaskParser(String[] args) {
        this.args = args;
    }

    private void process() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        parseArgs();
        makeTasks();
    }

    private void parseArgs() {
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) != '-') {
                sources.add(Path.of(args[i]));
                continue;
            }
            switch (args[i].charAt(1)) {
                case 'S' -> {
                }
                case 'O' -> setCompileLevel(args[i].substring(2));
                case 'o' -> setTarget(Path.of(args[++i]));
                case '-' -> setExtraOptions(args[i].substring(2));
                default -> throw new RuntimeException("Unsupported option: " + args[i]);
            }
        }
    }

    private void setCompileLevel(String level) {
        compileLevel = Integer.parseInt(level);
    }

    private void setTarget(Path target) {
        if (this.target != null) {
            throw new RuntimeException("Multi output files: " + this.target + " & " + target + "!");
        }
        this.target = target;
    }

    private void setExtraOptions(String arg) {
        int splitIndex = arg.indexOf('=');
        if (splitIndex < 0) {
            options.put(arg, null);
        } else {
            String key = arg.substring(0, splitIndex);
            String value = arg.substring(splitIndex + 1);
            options.put(key, value);
        }
    }

    private void makeTasks() {
        Path source = sources.get(0);
        String fileName = source.getFileName().toString();
        int splitIndex = fileName.lastIndexOf('.');
        String prefix = fileName.substring(0, splitIndex);
        String suffix = fileName.substring(splitIndex);
        switch (suffix) {
            case C_SUFFIX, SY_SUFFIX -> {
                Path iFile = FileUtil.makeTempFile(prefix, I_SUFFIX);
                task = new PreprocessTask(options, source, iFile);
                task = new CompileTask(options, iFile, target, task);
            }
            case I_SUFFIX -> task = new CompileTask(options, source, target);
            default -> throw new RuntimeException();
        }
    }

    public Task getTask() {
        process();
        return task;
    }
}
