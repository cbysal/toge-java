package preprocess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {
    private boolean isProcessed;
    private final Path input, output;
    private String content;

    public Preprocessor(Path input, Path output) {
        this.input = input;
        this.output = output;
    }

    public void preprocess() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        try {
            content = Files.readString(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        replaceMacroDef();
        removeEscapedNewLine();
        removeComment();
        try {
            Files.writeString(output, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void replaceMacroDef() {
        replaceBuiltInMacroFuncsWithLineNo();
    }

    private void replaceBuiltInMacroFuncsWithLineNo() {
        replaceMacroFuncWithLineNo("starttime", "_sysy_starttime");
        replaceMacroFuncWithLineNo("stoptime", "_sysy_stoptime");
    }

    private void replaceMacroFuncWithLineNo(String macro, String func) {
        Pattern macroPattern = Pattern.compile(String.format("\\b%s\\s*\\(\\s*\\)", macro));
        Matcher matcher = macroPattern.matcher(content);
        StringBuilder result = new StringBuilder(content.length());
        while (matcher.find()) {
            int start = matcher.start();
            long lineNo = content.chars().limit(start + 1).filter(c -> c == '\n').count() + 1;
            matcher.appendReplacement(result, String.format("%s(%d)", func, lineNo));
        }
        matcher.appendTail(result);
        content = result.toString();
    }

    private void removeEscapedNewLine() {
        Pattern pattern = Pattern.compile("\\\\\\n");
        Matcher matcher = pattern.matcher(content);
        content = matcher.replaceAll("");
    }

    private void removeComment() {
        Pattern pattern = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/");
        Matcher matcher = pattern.matcher(content);
        content = matcher.replaceAll("");
    }
}
