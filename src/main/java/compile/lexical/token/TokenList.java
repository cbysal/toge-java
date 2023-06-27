package compile.lexical.token;

import java.util.ArrayList;
import java.util.Arrays;

public class TokenList extends ArrayList<Token> {
    private int head;

    public TokenList() {
        super();
        reset();
    }

    public Token expectAndFetch(TokenType... types) {
        Token token = get(head++);
        for (TokenType type : types) {
            if (token.getType() == type) {
                return token;
            }
        }
        throw new RuntimeException("Expect " + Arrays.deepToString(types) + " , but get " + token.getType());
    }

    public boolean expect(int n, TokenType... types) {
        Token token = get(head + n);
        return Arrays.stream(types).anyMatch(type -> type.equals(token.getType()));
    }

    public boolean expect(TokenType... types) {
        Token token = get(head);
        return Arrays.stream(types).anyMatch(type -> type.equals(token.getType()));
    }

    public boolean hasNext() {
        return head < size();
    }

    public TokenType next() {
        return get(head++).getType();
    }

    public TokenType next(TokenType... types) {
        TokenType nextType = next();
        if (Arrays.stream(types).noneMatch(type -> type.equals(nextType)))
            throw new RuntimeException("Expecting " + Arrays.toString(types) + ", but meeting " + nextType);
        return nextType;
    }

    public int nextInt() {
        Token token = expectAndFetch(TokenType.INT_LIT);
        return ((IntLitToken) token).getValue(); // safe, because IntToken has the type INT_LIT
    }

    public float nextFloat() {
        Token token = expectAndFetch(TokenType.FLOAT_LIT);
        return ((FloatLitToken) token).getValue(); // safe, because FloatToken has the type FLOAT_LIT
    }

    public String nextIdentity() {
        Token token = expectAndFetch(TokenType.ID);
        return ((IdentityToken) token).getId(); // safe, because IdToken has the type ID
    }

    public TokenType peek() {
        return get(head).getType();
    }

    public TokenType peek(int n) {
        return get(head + n).getType();
    }

    public TokenType peek(TokenType... types) {
        TokenType nextType = peek();
        if (Arrays.stream(types).noneMatch(type -> type.equals(nextType)))
            throw new RuntimeException("Expecting " + Arrays.toString(types) + ", but meeting " + nextType);
        return nextType;
    }

    public void reset() {
        head = 0;
    }
}
