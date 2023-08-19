package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Type;
import compile.symbol.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MatchPatterns extends Pass {
    public MatchPatterns(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = matchAbs();
        return modified;
    }

    private boolean matchAbs() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                Map<VReg, VReg> movMap = new HashMap<>();
                for (VIR ir : block)
                    if (ir instanceof MovVIR movVIR)
                        movMap.put(movVIR.target, movVIR.source);
                if (block.getLast() instanceof BranchVIR branchVIR) {
                    BranchVIR.Type type = branchVIR.type;
                    VReg passReg;
                    if ((type == BranchVIR.Type.LE || type == BranchVIR.Type.LT) && branchVIR.left instanceof VReg reg && branchVIR.right instanceof Value value && (value.getType() == Type.FLOAT ? value.floatValue() : value.intValue()) == 0) {
                        passReg = reg;
                    } else if ((type == BranchVIR.Type.GE || type == BranchVIR.Type.GT) && branchVIR.left instanceof Value value && branchVIR.right instanceof VReg reg && (value.getType() == Type.FLOAT ? value.floatValue() : value.intValue()) == 0) {
                        passReg = reg;
                    } else
                        continue;
                    Block nextBlock1 = branchVIR.trueBlock;
                    Block nextBlock2 = branchVIR.falseBlock;
                    if (nextBlock1.size() == 1 && nextBlock1.getLast() instanceof JumpVIR jumpVIR && jumpVIR.target == nextBlock2) {
                        if (nextBlock1.get(0) instanceof UnaryVIR unaryVIR && unaryVIR.type == UnaryVIR.Type.NEG && unaryVIR.target == unaryVIR.source && movMap.get(unaryVIR.target) == passReg) {
                            block.add(new UnaryVIR(UnaryVIR.Type.ABS, unaryVIR.target, unaryVIR.source));
                            block.set(block.size() - 1, new JumpVIR(nextBlock2));
                            blocks.remove(nextBlock1);
                            i--;
                            modified = true;
                        }
                    }
                }
            }
        }
        return modified;
    }
}
