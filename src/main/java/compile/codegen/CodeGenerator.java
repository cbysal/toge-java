package compile.codegen;

import compile.codegen.mirgen.MachineFunction;
import compile.codegen.mirgen.mir.LabelMIR;
import compile.codegen.mirgen.mir.MIR;
import compile.symbol.GlobalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodeGenerator {
    private boolean isProcessed = false;
    private final Map<String, GlobalSymbol> consts;
    private final Map<String, GlobalSymbol> globals;
    private final Map<String, MachineFunction> funcs;
    private String output = null;

    public CodeGenerator(Map<String, GlobalSymbol> consts, Map<String, GlobalSymbol> globals, Map<String,
            MachineFunction> funcs) {
        this.consts = consts;
        this.globals = globals;
        this.funcs = funcs;
    }

    private void buildConsts(StringBuilder builder) {
        if (!consts.isEmpty())
            builder.append("\t.text\n");
        for (GlobalSymbol constSymbol : consts.values()) {
            int size = constSymbol.size();
            builder.append("\t.align 8\n");
            builder.append("\t.size ").append(constSymbol.getName()).append(", ").append(size).append('\n');
            builder.append(constSymbol.getName()).append(":\n");
            int num = size / 4;
            if (constSymbol.isSingle()) {
                builder.append("\t.word ").append(constSymbol.getInt()).append('\n');
            } else {
                for (int i = 0; i < num; i++)
                    builder.append("\t.word ").append(constSymbol.getInt(i)).append('\n');
            }
        }
    }

    private void buildGlobals(StringBuilder builder) {
        List<GlobalSymbol> symbolsInData = new ArrayList<>();
        List<GlobalSymbol> symbolsInBss = new ArrayList<>();
        for (GlobalSymbol globalSymbol : globals.values())
            if (!globalSymbol.isSingle() && globalSymbol.isInBss())
                symbolsInBss.add(globalSymbol);
            else
                symbolsInData.add(globalSymbol);
        if (!symbolsInBss.isEmpty())
            builder.append("\t.bss\n");
        for (GlobalSymbol globalSymbol : symbolsInBss) {
            int size = globalSymbol.size();
            builder.append("\t.align 8\n");
            builder.append("\t.size ").append(globalSymbol.getName()).append(", ").append(size).append('\n');
            builder.append(globalSymbol.getName()).append(":\n");
            builder.append("\t.space ").append(size).append('\n');
        }
        if (!symbolsInData.isEmpty())
            builder.append("\t.data\n");
        for (GlobalSymbol globalSymbol : symbolsInData) {
            int size = globalSymbol.size();
            builder.append("\t.align 8\n");
            builder.append("\t.size ").append(globalSymbol.getName()).append(", ").append(size).append('\n');
            builder.append(globalSymbol.getName()).append(":\n");
            int num = size / 4;
            if (globalSymbol.isSingle()) {
                builder.append("\t.word ").append(globalSymbol.getInt()).append('\n');
            } else {
                for (int i = 0; i < num; i++)
                    builder.append("\t.word ").append(globalSymbol.getInt(i)).append('\n');
            }
        }
    }

    private void buildFuncs(StringBuilder builder) {
        builder.append("\t.text\n");
        for (MachineFunction func : funcs.values()) {
            builder.append("\t.align 8\n");
            builder.append("\t.global ").append(func.getName()).append('\n');
            builder.append(func.getName()).append(":\n");
            for (MIR ir : func.getIrs()) {
                if (!(ir instanceof LabelMIR))
                    builder.append('\t');
                builder.append(ir.toString().replaceAll("\n", "\n\t")).append('\n');
            }
        }
    }

    private void buildHeader(StringBuilder builder) {
        builder.append("\t.arch armv7ve\n").append("\t.fpu vfpv4\n").append("\t.arm\n");
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        StringBuilder builder = new StringBuilder();
        buildConsts(builder);
        buildGlobals(builder);
        buildFuncs(builder);
        output = builder.toString();
    }

    public String getOutput() {
        checkIfIsProcessed();
        return output;
    }
}
