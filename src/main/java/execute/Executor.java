package execute;

import compile.Compiler;
import preprocess.Preprocessor;

import java.nio.file.Path;
import java.util.HashMap;

public class Executor {
    private final String[] args;
    private final OptionPool options = new OptionPool();
    private boolean isProcessed;
    private Path source;
    private Path target;

    public Executor(String[] args) {
        this.args = args;
    }

    private void parseArgs() {
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) != '-') {
                source = Path.of(args[i]);
                continue;
            }
            switch (args[i].charAt(1)) {
                case 'O', 'S' -> {
                }
                case 'o' -> setTarget(Path.of(args[++i]));
                case '-' -> setExtraOptions(args[i].substring(2));
                default -> throw new RuntimeException("Unsupported option: " + args[i]);
            }
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
        parseArgs();
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
        public static final String PRINT_MIR = "print-mir";
        public static final String EMIT_MIR = "emit-mir";
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
