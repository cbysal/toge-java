package compile;

import client.option.OptionPool;
import compile.codegen.CodeGenerator;
import compile.codegen.machine.DataItem;
import compile.codegen.machine.Function;
import compile.codegen.machine.TextItem;
import compile.lexical.LexicalParser;
import compile.lexical.token.TokenList;
import compile.llvm.LLVMParser;
import compile.llvm.ir.Module;
import compile.symbol.SymbolTable;
import compile.syntax.SyntaxParser;
import compile.syntax.ast.AST;
import compile.syntax.ast.RootAST;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Compiler {
    private final OptionPool options;
    private boolean isProcessed;
    private final Path inputFile, outputFile;

    public Compiler(OptionPool options, Path inputFile, Path outputFile) {
        this.options = options;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public void compile() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        String input;
        try {
            input = Files.readString(inputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LexicalParser lexicalParser = new LexicalParser(input);
        TokenList tokens = lexicalParser.getTokens();
        if (options.containsKey(OptionPool.PRINT_TOKENS)) {
            printTokens(tokens, options.get(OptionPool.PRINT_TOKENS));
        }
        SymbolTable symbolTable = new SymbolTable();
        SyntaxParser syntaxParser = new SyntaxParser(symbolTable, tokens);
        RootAST root = syntaxParser.getRootAST();
        if (options.containsKey(OptionPool.PRINT_AST)) {
            printAST(root);
        }
        LLVMParser llvmParser = new LLVMParser(root);
        Module module = llvmParser.getModule();
        if (options.containsKey(OptionPool.PRINT_LLVM)) {
            printLLVM(module, options.get(OptionPool.PRINT_LLVM));
        }
        CodeGenerator codeGenerator = new CodeGenerator(module);
        Map<String, TextItem> textItems = codeGenerator.getTextItems();
        Map<String, DataItem> dataItems = codeGenerator.getDataItems();
        Map<String, Function> functions = codeGenerator.getFunctions();
        emitCode(textItems, dataItems, functions);
    }

    private static void printTokens(TokenList tokens, String target) {
        StringBuilder toPrintInfo = new StringBuilder("============ print-tokens ============\n");
        tokens.forEach(token -> toPrintInfo.append(token).append('\n'));
        if (target == null) {
            System.out.println(toPrintInfo);
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(target))) {
            writer.write(toPrintInfo.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printAST(AST root) {
        System.out.println("============ print-ast ============");
        root.print(0);
    }

    private void printLLVM(Module module, String target) {
        if (target == null) {
            System.out.println("============ print-llvm ============\n");
            System.out.println(module);
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(target))) {
            writer.write(module.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void emitCode(Map<String, TextItem> textItems, Map<String, DataItem> dataItems,
                          Map<String, Function> functions) {
        StringBuilder builder = new StringBuilder();
        builder.append("  .text\n");
        textItems.values().forEach(builder::append);
        builder.append("  .data\n");
        dataItems.values().forEach(builder::append);
        builder.append("  .text\n");
        functions.values().forEach(builder::append);
        try {
            Files.writeString(outputFile, builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
