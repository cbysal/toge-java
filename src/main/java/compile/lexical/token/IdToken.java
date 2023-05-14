package compile.lexical.token;

public class IdToken extends Token {
    private final String id;

    IdToken(String id) {
        super(TokenType.ID);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("ID %s", id);
    }
}
