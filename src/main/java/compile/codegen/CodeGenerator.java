package compile.codegen;

import compile.codegen.mirgen.MachineFunction;
import compile.codegen.mirgen.mir.LabelMIR;
import compile.codegen.mirgen.mir.MIR;
import compile.llvm.GlobalVariable;
import compile.llvm.type.ArrayType;
import compile.llvm.type.BasicType;
import compile.llvm.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodeGenerator {
    private final Set<GlobalVariable> globals;
    private final Map<String, MachineFunction> funcs;

    public CodeGenerator(Set<GlobalVariable> globals, Map<String, MachineFunction> funcs) {
        this.globals = globals;
        this.funcs = funcs;
    }

    private void buildGlobals(StringBuilder builder) {
        List<GlobalVariable> symbolsInData = new ArrayList<>();
        List<GlobalVariable> symbolsInBss = new ArrayList<>();
        for (GlobalVariable global : globals)
            if (!global.isSingle() && global.isInBss())
                symbolsInBss.add(global);
            else
                symbolsInData.add(global);
        if (!symbolsInBss.isEmpty())
            builder.append("\t.bss\n");
        for (GlobalVariable global : symbolsInBss) {
            int size = global.getSize() / 8;
            builder.append("\t.align 8\n");
            builder.append("\t.size ").append(global.getRawName()).append(", ").append(size).append('\n');
            builder.append(global.getRawName()).append(":\n");
            builder.append("\t.space ").append(size).append('\n');
        }
        if (!symbolsInData.isEmpty())
            builder.append("\t.data\n");
        for (GlobalVariable global : symbolsInData) {
            int size = global.getSize() / 8;
            builder.append("\t.align 8\n");
            builder.append("\t.size ").append(global.getRawName()).append(", ").append(size).append('\n');
            builder.append(global.getRawName()).append(":\n");
            int num = size / 4;
            if (global.isSingle()) {
                Number number;
                if (global.getType().equals(BasicType.I32)) {
                    number = global.getInt();
                } else if (global.getType().equals(BasicType.FLOAT)) {
                    number = Float.floatToIntBits(global.getFloat());
                } else {
                    throw new IllegalStateException("Unexpected value: " + global.getType());
                }
                builder.append("\t.word ").append(number).append('\n');
            } else {
                Type type = global.getType();
                ArrayType arrayType;
                while (type instanceof ArrayType) {
                    arrayType = (ArrayType) type;
                    type = arrayType.baseType();
                }
                for (int i = 0; i < num; i++) {
                    Number number;
                    if (type.equals(BasicType.I32)) {
                        number = global.getInt(i);
                    } else if (type.equals(BasicType.FLOAT)) {
                        number = Float.floatToIntBits(global.getFloat(i));
                    } else {
                        throw new IllegalStateException("Unexpected value: " + type);
                    }
                    builder.append("\t.word ").append(number).append('\n');
                }
            }
        }
    }

    private void buildFuncs(StringBuilder builder) {
        builder.append("\t.text\n");
        for (MachineFunction func : funcs.values()) {
            builder.append("\t.align 8\n");
            builder.append("\t.global ").append(func.getRawName()).append('\n');
            builder.append(func.getRawName()).append(":\n");
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
