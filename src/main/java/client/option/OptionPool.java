package client.option;

import java.util.HashMap;

public class OptionPool {
    public static final String PRINT_TOKENS = "print-tokens";
    public static final String PRINT_AST = "print-ast";
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
