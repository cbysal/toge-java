package compile.lexical.token;

public class IdToken extends Token {
    private final String id;

    IdToken(String id) {
        super(Type.ID);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ID " + id;
    }
}
