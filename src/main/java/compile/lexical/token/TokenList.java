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
    public Token.Type peekType() {
        return peekType(0);
    }

    public Token.Type peekType(int overlook) {
        return tokens.get(head + overlook).getType();
    }

    public Token expectAndFetch(Token.Type... types) {
        Token token = tokens.get(head++);
        for (Token.Type type : types) {
            if (token.getType() == type) {
                return token;
            }
        }
        throw new RuntimeException("Expect " + Arrays.deepToString(types) + " , but get " + token.getType());
    }

    public boolean match(Token.Type... types) {
        Token.Type t = tokens.get(head).getType();
        return Arrays.stream(types).anyMatch(type -> type == t);
    }

    public boolean matchAndThenThrow(Token.Type type) {
        Token token = tokens.get(head);
        if (token.getType() == type) {
            head++;
            return true;
        }
        return false;
    }

    public int nextInt() {
        Token token = expectAndFetch(Token.Type.INT_LIT);
        return ((IntToken) token).getInt(); // safe, because IntToken has the type INT_LIT
    }

    public float nextFloat() {
        Token token = expectAndFetch(Token.Type.FLOAT_LIT);
        return ((FloatToken) token).getFloat(); // safe, because FloatToken has the type FLOAT_LIT
    }

    public String nextIdentity() {
        Token token = expectAndFetch(Token.Type.ID);
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
