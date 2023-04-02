package compile;

import client.option.OptionPool;
import compile.lexical.LexicalParser;
import compile.lexical.token.TokenList;
import compile.symbol.SymbolTable;
import compile.syntax.SyntaxParser;
import compile.syntax.ast.AST;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        AST root = syntaxParser.getRootAST();
        if (options.containsKey("print-ast")) {
            printAST(root);
        }
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

}
