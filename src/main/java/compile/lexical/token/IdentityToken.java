package compile.lexical.token;

public class IdentityToken extends Token {
    private final String id;

    IdentityToken(String id) {
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
