package compile;

import compile.codegen.CodeGenerator;
import compile.codegen.mirgen.MIRGenerator;
import compile.codegen.mirgen.MachineFunction;
import compile.codegen.regalloc.RegAllocator;
import compile.llvm.Function;
import compile.llvm.GlobalVariable;
import compile.sysy.SysYLexer;
import compile.sysy.SysYParser;
import execute.Executor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Compiler {
    private final Executor.OptionPool options;
    private final String input;
    private final Path outputFile;

    public Compiler(Executor.OptionPool options, String input, Path outputFile) {
        this.options = options;
        this.input = input;
        this.outputFile = outputFile;
    }

    public void compile() {
        SysYLexer lexer = new SysYLexer(CharStreams.fromString(input));
        CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
        SysYParser parser = new SysYParser(commonTokenStream);
        SysYParser.RootContext rootContext = parser.root();
        ASTVisitor astVisitor = new ASTVisitor(rootContext);
        Set<GlobalVariable> globals = astVisitor.getGlobals();
        Map<String, Function> funcs = astVisitor.getFuncs();
        if (options.containsKey("emit-llvm"))
            emitLLVM(options.get("emit-llvm"), globals, funcs);
        if (options.containsKey("emit-opt-llvm"))
            emitLLVM(options.get("emit-opt-llvm"), globals, funcs);
        MIRGenerator mirGenerator = new MIRGenerator(globals, funcs);
        globals = mirGenerator.getGlobals();
        Map<String, MachineFunction> mFuncs = mirGenerator.getFuncs();
        if (options.containsKey("emit-mir"))
            emitMIR(options.get("emit-mir"), mFuncs);
        if (options.containsKey("emit-opt-mir"))
            emitMIR(options.get("emit-opt-mir"), mFuncs);
        RegAllocator regAllocator = new RegAllocator(mFuncs);
        regAllocator.allocate();
        CodeGenerator codeGenerator = new CodeGenerator(globals, mFuncs);
        String output = codeGenerator.getOutput();
        try {
            Files.writeString(outputFile, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void emitLLVM(String filePath, Set<GlobalVariable> globals, Map<String, Function> funcs) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (GlobalVariable global : globals) {
                writer.write(global.toString());
                writer.newLine();
            }
            if (!globals.isEmpty()) {
                writer.newLine();
            }
            for (Function func : funcs.values().stream().filter(func -> !func.getBlocks().isEmpty()).toList()) {
                writer.write(func.toString());
                writer.newLine();
            }
            for (Function func : funcs.values().stream().filter(func -> func.getBlocks().isEmpty()).toList()) {
                writer.write(func.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void emitMIR(String filePath, Map<String, MachineFunction> funcs) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (MachineFunction func : funcs.values()) {
                writer.write(func.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
