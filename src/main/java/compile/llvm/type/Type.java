package compile.llvm.type;

public interface Type {
    int getSize();

    Type baseType();
}
