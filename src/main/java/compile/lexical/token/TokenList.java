package compile.lexical.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TokenList {
    private final List<Token> tokens = new ArrayList<>();
    private int head;

    public boolean add(Token token) {
        return tokens.add(token);
    }

    public Token expectAndFetch(TokenType... types) {
        Token token = tokens.get(head++);
        for (TokenType type : types) {
            if (token.getType() == type) {
                return token;
            }
        }
        throw new RuntimeException("Expect " + Arrays.deepToString(types) + " , but get " + token.getType());
    }

    public boolean expect(int n, TokenType... types) {
        Token token = tokens.get(head + n);
        return Arrays.stream(types).anyMatch(type -> type.equals(token.getType()));
    }

    public boolean expect(TokenType... types) {
        Token token = tokens.get(head);
        return Arrays.stream(types).anyMatch(type -> type.equals(token.getType()));
    }

    public boolean hasNext() {
        return head < tokens.size();
    }

    public TokenType next() {
        return tokens.get(head++).getType();
    }

    public TokenType next(TokenType... types) {
        TokenType nextType = next();
        if (Arrays.stream(types).noneMatch(type -> type.equals(nextType)))
            throw new RuntimeException("Expecting " + Arrays.toString(types) + ", but meeting " + nextType);
        return nextType;
    }

    public int nextInt() {
        Token token = expectAndFetch(TokenType.INT_LIT);
        return ((IntLitToken) token).second(); // safe, because IntToken has the type INT_LIT
    }

    public float nextFloat() {
        Token token = expectAndFetch(TokenType.FLOAT_LIT);
        return ((FloatLitToken) token).second(); // safe, because FloatToken has the type FLOAT_LIT
    }

    public String nextIdentity() {
        Token token = expectAndFetch(TokenType.ID);
        return ((IdentityToken) token).getId(); // safe, because IdToken has the type ID
    }

    public TokenType peek() {
        return tokens.get(head).getType();
    }

    public TokenType peek(int n) {
        return tokens.get(head + n).getType();
    }

    public TokenType peek(TokenType... types) {
        TokenType nextType = peek();
        if (Arrays.stream(types).noneMatch(type -> type.equals(nextType)))
            throw new RuntimeException("Expecting " + Arrays.toString(types) + ", but meeting " + nextType);
        return nextType;
    }
}
