package compile.llvm.ir.instr;

import compile.llvm.ir.Value;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.PointerType;
import compile.llvm.ir.type.Type;

public class GetElementPtrInstr extends Instr {
    private final Value base, offset, index;

    public GetElementPtrInstr(Value base, Value offset) {
        super(base.getType());
        this.base = base;
        this.offset = offset;
        this.index = null;
    }

    public GetElementPtrInstr(Value base, Value offset, Value index) {
        super(processType(base.getType()));
        this.base = base;
        this.offset = offset;
        this.index = index;
    }

    private static Type processType(Type type) {
        PointerType pointerType = (PointerType) type;
        Type base = pointerType.base();
        if (base instanceof ArrayType arrayType) {
            return new PointerType(arrayType.base());
        }
        if (base instanceof BasicType) {
            return new PointerType(base);
        }
        return base;
    }

    public Value getBase() {
        return base;
    }

    public Value getOffset() {
        return offset;
    }

    public Value getIndex() {
        return index;
    }

    @Override
    public String toString() {
        if (index == null) {
            return String.format("%s = getelementptr inbounds %s, %s %s, %s", getTag(),
                    ((PointerType) base.getType()).base(), base.getType(), base.getTag(), offset.getRet());
        } else {
            return String.format("%s = getelementptr inbounds %s, %s %s, %s, %s", getTag(),
                    ((PointerType) base.getType()).base(), base.getType(), base.getTag(), offset.getRet(),
                    index.getRet());
        }
    }
}
