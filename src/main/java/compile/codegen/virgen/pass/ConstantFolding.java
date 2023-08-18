package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Value;

import java.util.Map;
import java.util.Set;

public class ConstantFolding extends Pass {
    public ConstantFolding(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        if (binaryVIR.left instanceof Value left && binaryVIR.right instanceof Value right) {
                            Value result = switch (binaryVIR.type) {
                                case ADD -> left.add(right);
                                case SUB -> left.sub(right);
                                case MUL -> left.mul(right);
                                case DIV -> left.div(right);
                                case MOD -> left.mod(right);
                                case EQ -> left.eq(right);
                                case NE -> left.ne(right);
                                case GE -> left.ge(right);
                                case GT -> left.gt(right);
                                case LE -> left.le(right);
                                case LT -> left.lt(right);
                            };
                            switch (result.getType()) {
                                case INT -> block.set(i, new LiVIR(binaryVIR.target, result.getInt()));
                                case FLOAT -> block.set(i, new LiVIR(binaryVIR.target, result.getFloat()));
                            }
                            modified = true;
                        }
                        continue;
                    }
                    if (ir instanceof BranchVIR branchVIR) {
                        if (branchVIR.left instanceof Value left && branchVIR.right instanceof Value right) {
                            Value result = switch (branchVIR.type) {
                                case EQ -> left.eq(right);
                                case NE -> left.ne(right);
                                case GE -> left.ge(right);
                                case GT -> left.gt(right);
                                case LE -> left.le(right);
                                case LT -> left.lt(right);
                            };
                            block.set(i, new JumpVIR(result.isZero() ? branchVIR.falseBlock : branchVIR.trueBlock));
                            modified = true;
                            break;
                        }
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        if (unaryVIR.source instanceof Value source) {
                            Value result = switch (unaryVIR.type) {
                                case F2I -> source.toInt();
                                case I2F -> source.toFloat();
                                case L_NOT -> source.lNot();
                                case NEG -> source.neg();
                                case ABS -> source.abs();
                            };
                            switch (result.getType()) {
                                case INT -> block.set(i, new LiVIR(unaryVIR.target, result.getInt()));
                                case FLOAT -> block.set(i, new LiVIR(unaryVIR.target, result.getFloat()));
                            }
                            modified = true;
                        }
                        continue;
                    }
                }
            }
        }
        return modified;
    }
}
