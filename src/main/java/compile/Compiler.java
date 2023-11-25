package compile;

import compile.codegen.CodeGenerator;
import compile.codegen.mirgen.MIRGenerator;
import compile.codegen.mirgen.MIRPassManager;
import compile.codegen.mirgen.MachineFunction;
import compile.codegen.regalloc.RegAllocator;
import compile.codegen.virgen.VIRGenerator;
import compile.codegen.virgen.VIRPassManager;
import compile.codegen.virgen.VirtualFunction;
import compile.lexical.LexicalParser;
import compile.lexical.token.TokenList;
import compile.symbol.GlobalSymbol;
import compile.symbol.SymbolTable;
import compile.syntax.SyntaxParser;
import compile.syntax.SyntaxPassManager;
import compile.syntax.ast.RootAST;
import execute.Executor;

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
        LexicalParser lexicalParser = new LexicalParser(input);
        TokenList tokens = lexicalParser.getTokens();
        SymbolTable symbolTable = new SymbolTable();
        SyntaxParser syntaxParser = new SyntaxParser(symbolTable, tokens);
        RootAST rootAST = syntaxParser.getRootAST();
        SyntaxPassManager syntaxPassManager = new SyntaxPassManager(rootAST);
        syntaxPassManager.run();
        rootAST = syntaxPassManager.getRootAST();
        VIRGenerator virGenerator = new VIRGenerator(rootAST);
        Set<GlobalSymbol> globals = virGenerator.getGlobals();
        Map<String, VirtualFunction> vFuncs = virGenerator.getFuncs();
        if (options.containsKey("emit-vir"))
            emitVIR(options.get("emit-vir"), vFuncs);
        VIRPassManager virPassManager = new VIRPassManager(globals, vFuncs);
        virPassManager.run();
        if (options.containsKey("emit-opt-vir"))
            emitVIR(options.get("emit-opt-vir"), vFuncs);
        MIRGenerator mirGenerator = new MIRGenerator(globals, vFuncs);
        globals = mirGenerator.getGlobals();
        Map<String, MachineFunction> mFuncs = mirGenerator.getFuncs();
        if (options.containsKey("emit-mir"))
            emitMIR(options.get("emit-mir"), mFuncs);
        MIRPassManager mirPassManager = new MIRPassManager(globals, mFuncs);
        mirPassManager.run();
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
}
