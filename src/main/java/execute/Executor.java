package execute;

import compile.Compiler;
import org.apache.commons.cli.*;
import preprocess.Preprocessor;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;

public class Executor {
    private final OptionPool options = new OptionPool();
    private boolean isProcessed;
    private Path source;
    private Path target;

    public Executor(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("S").build());
        options.addOption(Option.builder("O").hasArg().type(Number.class).build());
        options.addOption(Option.builder("o").hasArg().type(File.class).build());
        CommandLine commandLine;
        try {
            commandLine = DefaultParser.builder().build().parse(options, args);
            if (commandLine.hasOption("o")) {
                setTarget(((File) commandLine.getParsedOptionValue("o")).toPath());
            }
            for (String arg : commandLine.getArgList()) {
                if (arg.startsWith("--")) {
                    setExtraOptions(arg.substring(2));
                } else {
                    source = Path.of(arg);
                }
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
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

    public void execute() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        Preprocessor preprocessor = new Preprocessor(source);
        String srcContent = preprocessor.preprocess();
        Compiler compiler = new Compiler(options, srcContent, target);
        compiler.compile();
    }

    public static class OptionPool {
        public static final String PRINT_VIR_BEFORE_OPTIMIZATION = "print-vir-before-optimization";
        public static final String EMIT_VIR_BEFORE_OPTIMIZATION = "emit-vir-before-optimization";
        public static final String PRINT_VIR_AFTER_OPTIMIZATION = "print-vir-after-optimization";
        public static final String EMIT_VIR_AFTER_OPTIMIZATION = "emit-vir-after-optimization";
        public static final String EMIT_MIR_BEFORE_OPTIMIZATION = "emit-mir-before-optimization";
        public static final String EMIT_MIR_AFTER_OPTIMIZATION = "emit-mir-after-optimization";
        public static final String PRINT_ASM = "print-asm";
        public static final String EMIT_ASM = "emit-asm";
        private final HashMap<String, String> pool = new HashMap<>();

        public boolean containsKey(String key) {
            return pool.containsKey(key);
        }

        public String get(String key) {
            return pool.get(key);
        }

        public void put(String key, String value) {
            pool.put(key, value);
        }
    }
}
