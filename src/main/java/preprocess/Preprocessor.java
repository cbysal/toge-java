package preprocess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {
    private static final Pattern ESCAPED_NEW_LINE_PATTERN = Pattern.compile("\\\\\\n");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/");
    private final Path input;
    private String content;

    public Preprocessor(Path input) {
        this.input = input;
    }

    public String preprocess() {
        try {
            content = Files.readString(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        replaceMacroDef();
        removeEscapedNewLine();
        removeComment();
        return content;
    }

    private void replaceMacroDef() {
        replaceBuiltinMacroFuncWithLineNo();
    }

    private void replaceBuiltinMacroFuncWithLineNo() {
        replaceMacroFuncWithLineNo("starttime", "_sysy_starttime");
        replaceMacroFuncWithLineNo("stoptime", "_sysy_stoptime");
    }

    private void replaceMacroFuncWithLineNo(String macro, String func) {
        Pattern macroPattern = Pattern.compile(String.format("\\b%s\\s*\\(\\s*\\)", macro));
        Matcher matcher = macroPattern.matcher(content);
        StringBuilder result = new StringBuilder(content.length());
        while (matcher.find()) {
            int start = matcher.start();
            long lineNo = content.substring(0, start + 1).lines().count();
            matcher.appendReplacement(result, String.format("%s(%d)", func, lineNo));
        }
        matcher.appendTail(result);
        content = result.toString();
    }

    private void removeEscapedNewLine() {
        Matcher matcher = ESCAPED_NEW_LINE_PATTERN.matcher(content);
        content = matcher.replaceAll("");
    }

    private void removeComment() {
        Matcher matcher = COMMENT_PATTERN.matcher(content);
        content = matcher.replaceAll("");
    }
}
