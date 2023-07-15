package compile;

import execute.Executor;
import compile.codegen.CodeGenerator;
import compile.codegen.mirgen.MIRGenerator;
import compile.codegen.mirgen.MachineFunction;
import compile.codegen.regalloc.RegAllocator;
import compile.codegen.virgen.VIRGenerator;
import compile.codegen.virgen.VIROptimizer;
import compile.codegen.virgen.VirtualFunction;
import compile.lexical.LexicalParser;
import compile.lexical.token.TokenList;
import compile.symbol.GlobalSymbol;
import compile.symbol.SymbolTable;
import compile.syntax.SyntaxParser;
import compile.syntax.ast.RootAST;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Compiler {
    private final Executor.OptionPool options;
    private boolean isProcessed = false;
    private final String input;
    private final Path outputFile;

    public Compiler(Executor.OptionPool options, String input, Path outputFile) {
        this.options = options;
        this.input = input;
        this.outputFile = outputFile;
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        LexicalParser lexicalParser = new LexicalParser(input);
        TokenList tokens = lexicalParser.getTokens();
        SymbolTable symbolTable = new SymbolTable();
        SyntaxParser syntaxParser = new SyntaxParser(symbolTable, tokens);
        RootAST rootAST = syntaxParser.getRootAST();
        VIRGenerator virGenerator = new VIRGenerator(rootAST);
        Map<String, GlobalSymbol> consts = virGenerator.getConsts();
        Map<String, GlobalSymbol> globals = virGenerator.getGlobals();
        Map<String, VirtualFunction> vFuncs = virGenerator.getFuncs();
        if (options.containsKey(Executor.OptionPool.PRINT_VIR_BEFORE_OPTIMIZATION))
            printVIR(vFuncs);
        if (options.containsKey(Executor.OptionPool.EMIT_VIR_BEFORE_OPTIMIZATION))
            emitVIR(options.get(Executor.OptionPool.EMIT_VIR_BEFORE_OPTIMIZATION), vFuncs);
        VIROptimizer virOptimizer = new VIROptimizer(consts, globals, vFuncs);
        virOptimizer.optimize();
        if (options.containsKey(Executor.OptionPool.PRINT_VIR_AFTER_OPTIMIZATION))
            printVIR(vFuncs);
        if (options.containsKey(Executor.OptionPool.EMIT_VIR_AFTER_OPTIMIZATION))
            emitVIR(options.get(Executor.OptionPool.EMIT_VIR_AFTER_OPTIMIZATION), vFuncs);
        MIRGenerator mirGenerator = new MIRGenerator(consts, globals, vFuncs);
        consts = mirGenerator.getConsts();
        globals = mirGenerator.getGlobals();
        Map<String, MachineFunction> mFuncs = mirGenerator.getFuncs();
        if (options.containsKey(Executor.OptionPool.PRINT_MIR))
            printMIR(mFuncs);
        if (options.containsKey(Executor.OptionPool.EMIT_MIR))
            emitMIR(options.get(Executor.OptionPool.EMIT_MIR), mFuncs);
        RegAllocator regAllocator = new RegAllocator(mFuncs);
        regAllocator.allocate();
        CodeGenerator codeGenerator = new CodeGenerator(consts, globals, mFuncs);
        String output = codeGenerator.getOutput();
        if (options.containsKey(Executor.OptionPool.PRINT_ASM))
            System.out.println(output);
        try {
            Files.writeString(outputFile, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (options.containsKey(Executor.OptionPool.EMIT_ASM))
            emitASM(new File(options.get(Executor.OptionPool.EMIT_ASM)), output);
    }

    public void compile() {
        checkIfIsProcessed();
    }

    private void emitASM(File file, String output) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(output);
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

    private void emitVIR(String filePath, Map<String, VirtualFunction> funcs) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (VirtualFunction func : funcs.values()) {
                writer.write(func.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printVIR(Map<String, VirtualFunction> funcs) {
        System.out.println("============ print-vir ============");
        funcs.forEach((key, value) -> System.out.println(value));
    }

    private void printMIR(Map<String, MachineFunction> funcs) {
        System.out.println("============ print-mir ============");
        funcs.forEach((key, value) -> System.out.println(value));
    }
}
