package compile.llvm.ir.type;

public final class BasicType implements Type {
    private final String name;

    private BasicType(String name) {
        this.name = name;
    }

    @Override
    public int getSize() {
        return 4;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final BasicType VOID = new BasicType("void");
    public static final BasicType I1 = new BasicType("i1");
    public static final BasicType I32 = new BasicType("i32");
    public static final BasicType FLOAT = new BasicType("float");
}
