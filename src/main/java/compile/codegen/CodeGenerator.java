package compile.codegen;

import compile.codegen.mirgen.MachineFunction;
import compile.codegen.mirgen.mir.LabelMIR;
import compile.codegen.mirgen.mir.MIR;
import compile.symbol.GlobalSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodeGenerator {
    private final Set<GlobalSymbol> globals;
    private final Map<String, MachineFunction> funcs;

    public CodeGenerator(Set<GlobalSymbol> globals, Map<String, MachineFunction> funcs) {
        this.globals = globals;
        this.funcs = funcs;
    }

    private void buildGlobals(StringBuilder builder) {
        List<GlobalSymbol> symbolsInData = new ArrayList<>();
        List<GlobalSymbol> symbolsInBss = new ArrayList<>();
        for (GlobalSymbol globalSymbol : globals)
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
            builder.append("\tret\n");
        }
    }

    public String getOutput() {
        StringBuilder builder = new StringBuilder();
        buildGlobals(builder);
        buildFuncs(builder);
        return builder.toString();
    }
}
