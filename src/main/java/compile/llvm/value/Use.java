package compile.llvm.value;

public class Use {
    private  User user;
    private  Value value;

    public Use(User user, Value value) {
        this.user = user;
        this.value = value;
    }

    public User getUser() {
        return user;
    }

    public Value getValue() {
        return value;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setValue(Value value) {
        this.value = value;
    }
}
