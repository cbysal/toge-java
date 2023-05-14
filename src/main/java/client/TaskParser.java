package client;

import client.option.OptionPool;
import client.task.*;
import utils.FileUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TaskParser {
    private enum TargetState {
        PREPROCESS, COMPILE, ASSEMBLE, LINK
    }

    private static final String C_SUFFIX = ".c";
    private static final String SY_SUFFIX = ".sy";
    private static final String I_SUFFIX = ".i";
    private static final String S_SUFFIX = ".s";
    private static final String O_SUFFIX = ".o";
    private static final String A_SUFFIX = ".a";
    private static final String SO_SUFFIX = ".so";

    private boolean isProcessed;
    private final String[] args;
    private TargetState targetState = TargetState.LINK;
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
                case 'E' -> setCompileState(TargetState.PREPROCESS);
                case 'S' -> setCompileState(TargetState.COMPILE);
                case 'c' -> setCompileState(TargetState.ASSEMBLE);
                case 'o' -> setTarget(Path.of(args[++i]));
                case '-' -> setExtraOptions(args[i].substring(2));
                default -> throw new RuntimeException("Unsupported option: " + args[i]);
            }
        }
    }

    private void setCompileState(TargetState state) {
        if (targetState != TargetState.LINK) {
            throw new RuntimeException("Multi target states: " + targetState + " & " + state + "!");
        }
        targetState = state;
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
        switch (targetState) {
            case PREPROCESS -> makeTasksInPreprocessMode();
            case COMPILE -> makeTasksInCompileMode();
            case ASSEMBLE -> makeTasksInAssembleMode();
            case LINK -> makeTasksInLinkMode();
            default -> throw new RuntimeException("Unhandled target state: " + targetState);
        }
    }

    private void makeTasksInPreprocessMode() {
        Path source = sources.get(0);
        String fileName = source.getFileName().toString();
        String suffix = fileName.substring(fileName.lastIndexOf('.'));
        if (C_SUFFIX.equals(suffix) || SY_SUFFIX.equals(suffix)) {
            task = new PreprocessTask(options, source, target);
        } else {
            throw new RuntimeException("Unprocessable file in target state " + targetState + ": " + source);
        }
    }

    private void makeTasksInCompileMode() {
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
            default -> throw new RuntimeException("Unprocessable file in target state " + targetState + ": " + source);
        }
    }

    private void makeTasksInAssembleMode() {
        Path source = sources.get(0);
        String fileName = source.getFileName().toString();
        int splitIndex = fileName.lastIndexOf('.');
        String prefix = fileName.substring(0, splitIndex);
        String suffix = fileName.substring(splitIndex);
        switch (suffix) {
            case C_SUFFIX, SY_SUFFIX -> {
                Path iFile = FileUtil.makeTempFile(prefix, I_SUFFIX);
                Path sFile = FileUtil.makeTempFile(prefix, S_SUFFIX);
                task = new PreprocessTask(options, source, iFile);
                task = new CompileTask(options, iFile, sFile, task);
                task = new AssembleTask(options, sFile, target, task);
            }
            case I_SUFFIX -> {
                Path sFile = FileUtil.makeTempFile(prefix, S_SUFFIX);
                task = new CompileTask(options, source, sFile);
                task = new AssembleTask(options, sFile, target, task);
            }
            case S_SUFFIX -> task = new AssembleTask(options, source, target);
            default -> throw new RuntimeException("Unprocessable file in target state " + targetState + ": " + source);
        }
    }

    private void makeTasksInLinkMode() {
        List<Path> binaryFiles = new ArrayList<>();
        List<Task> assembleTasks = new ArrayList<>();
        for (Path source : sources) {
            String fileName = source.getFileName().toString();
            int splitIndex = fileName.lastIndexOf('.');
            String prefix = fileName.substring(0, splitIndex);
            String suffix = fileName.substring(splitIndex);
            switch (suffix) {
                case C_SUFFIX, SY_SUFFIX -> handleCFile(binaryFiles, assembleTasks, source, prefix);
                case I_SUFFIX -> handleIFile(binaryFiles, assembleTasks, source, prefix);
                case S_SUFFIX -> handleSFile(binaryFiles, assembleTasks, source, prefix);
                case O_SUFFIX, A_SUFFIX, SO_SUFFIX -> handleOFile(binaryFiles, source);
                default ->
                        throw new RuntimeException("Unprocessable file in target state " + targetState + ": " + source);
            }
        }
        task = new LinkTask(options, binaryFiles, target, assembleTasks);
    }

    private void handleCFile(List<Path> binaryFiles, List<Task> assembleTasks, Path source, String prefix) {
        Path iFile = FileUtil.makeTempFile(prefix, I_SUFFIX);
        Path sFile = FileUtil.makeTempFile(prefix, S_SUFFIX);
        Path oFile = FileUtil.makeTempFile(prefix, O_SUFFIX);
        task = new PreprocessTask(options, source, iFile);
        task = new CompileTask(options, iFile, sFile, task);
        task = new AssembleTask(options, sFile, oFile, task);
        binaryFiles.add(oFile);
        assembleTasks.add(task);
    }

    private void handleIFile(List<Path> binaryFiles, List<Task> assembleTasks, Path source, String prefix) {
        Path sFile = FileUtil.makeTempFile(prefix, S_SUFFIX);
        Path oFile = FileUtil.makeTempFile(prefix, O_SUFFIX);
        task = new CompileTask(options, source, sFile);
        task = new AssembleTask(options, sFile, oFile, task);
        binaryFiles.add(oFile);
        assembleTasks.add(task);
    }

    private void handleSFile(List<Path> binaryFiles, List<Task> assembleTasks, Path source, String prefix) {
        Path oFile = FileUtil.makeTempFile(prefix, O_SUFFIX);
        task = new AssembleTask(options, source, oFile);
        binaryFiles.add(oFile);
        assembleTasks.add(task);
    }

    private static void handleOFile(List<Path> binaryFiles, Path source) {
        binaryFiles.add(source);
    }

    public Task getTask() {
        process();
        return task;
    }
}
