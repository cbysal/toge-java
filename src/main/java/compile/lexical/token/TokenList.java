package compile.lexical.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class TokenList implements Iterable<Token> {
    private final ArrayList<Token> tokens = new ArrayList<>();
    private int head;

    public boolean add(Token token) {
        return tokens.add(token);
    }

    public boolean hasNext() {
        return head < tokens.size();
    }

    // TODO replace peekType with match
    public TokenType peekType() {
        return peekType(0);
    }

    public TokenType peekType(int overlook) {
        return tokens.get(head + overlook).getType();
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

    public boolean match(TokenType... types) {
        TokenType t = tokens.get(head).getType();
        return Arrays.stream(types).anyMatch(type -> type == t);
    }

    public boolean matchAndThenThrow(TokenType type) {
        Token token = tokens.get(head);
        if (token.getType() == type) {
            head++;
            return true;
        }
        return false;
    }

    public int nextInt() {
        Token token = expectAndFetch(TokenType.INT_LIT);
        return ((IntToken) token).getValue(); // safe, because IntToken has the type INT_LIT
    }

    public float nextFloat() {
        Token token = expectAndFetch(TokenType.FLOAT_LIT);
        return ((FloatToken) token).getValue(); // safe, because FloatToken has the type FLOAT_LIT
    }

    public String nextIdentity() {
        Token token = expectAndFetch(TokenType.ID);
        return ((IdToken) token).getId(); // safe, because IdToken has the type ID
    }

    @Override
    public Iterator<Token> iterator() {
        return tokens.iterator();
    }

    @Override
    public void forEach(Consumer<? super Token> action) {
        tokens.forEach(action);
    }

    @Override
    public Spliterator<Token> spliterator() {
        return tokens.spliterator();
    }
}
