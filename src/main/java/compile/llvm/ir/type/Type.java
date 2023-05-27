package compile.llvm.ir.type;

public interface Type {
    int getSize();

    BasicType getRootBase();

    boolean equals(Type type);
}
