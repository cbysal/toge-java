package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Value;

import java.util.*;

public class FullLoopUnrolling extends Pass {
    public FullLoopUnrolling(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = false;
        for (VirtualFunction func : funcs.values()) {
            List<Block> blocks = func.getBlocks();
            Map<Block, Set<Block>> prevBlockMap = analyzePrevBlocks(func.getBlocks());
            for (Block initBlock : blocks) {
                VIR initLastIR = initBlock.getLast();
                if (!(initLastIR instanceof JumpVIR initJump))
                    continue;
                Block judgeBlock = initJump.target;
                VIR judgeLastIR = judgeBlock.getLast();
                if (!(judgeLastIR instanceof BranchVIR judgeBranch))
                    continue;
                BranchVIR.Type cond = judgeBranch.type;
                Block trueBlock = judgeBranch.trueBlock;
                Block falseBlock = judgeBranch.falseBlock;
                VIRItem item1 = judgeBranch.left;
                VIRItem item2 = judgeBranch.right;
                VReg loopReg = null;
                Number loopLimit = null;
                if (item1 instanceof VReg reg && item2 instanceof Value value) {
                    loopReg = reg;
                    switch (value.getType()) {
                        case FLOAT -> loopLimit = value.floatValue();
                        case INT -> loopLimit = value.intValue();
                    }
                } else if (item1 instanceof Value value && item2 instanceof VReg reg) {
                    loopReg = reg;
                    switch (value.getType()) {
                        case FLOAT -> loopLimit = value.floatValue();
                        case INT -> loopLimit = value.intValue();
                    }
                    cond = switch (cond) {
                        case EQ -> BranchVIR.Type.EQ;
                        case NE -> BranchVIR.Type.NE;
                        case GE -> BranchVIR.Type.LE;
                        case GT -> BranchVIR.Type.LT;
                        case LE -> BranchVIR.Type.GE;
                        case LT -> BranchVIR.Type.GT;
                    };
                } else
                    continue;
                Block loopBlock, endBlock;
                if (trueBlock.getLast() instanceof JumpVIR loopBackJump && loopBackJump.target == judgeBlock) {
                    loopBlock = trueBlock;
                    endBlock = falseBlock;
                } else if (falseBlock.getLast() instanceof JumpVIR loopBackJump && loopBackJump.target == judgeBlock) {
                    loopBlock = falseBlock;
                    endBlock = trueBlock;
                    cond = switch (cond) {
                        case EQ -> BranchVIR.Type.NE;
                        case NE -> BranchVIR.Type.EQ;
                        case GE -> BranchVIR.Type.LT;
                        case GT -> BranchVIR.Type.LE;
                        case LE -> BranchVIR.Type.GT;
                        case LT -> BranchVIR.Type.GE;
                    };
                } else
                    continue;
                if (prevBlockMap.get(judgeBlock).size() != 2)
                    continue;
                if (prevBlockMap.get(loopBlock).size() != 1)
                    continue;
                Number initValue = null;
                for (int i = initBlock.size() - 2; i >= 0; i--) {
                    VIR ir = initBlock.get(i);
                    if (ir instanceof LiVIR liVIR) {
                        if (liVIR.target == loopReg) {
                            initValue = liVIR.value;
                            break;
                        }
                    } else if (ir.getWrite() == loopReg)
                        break;
                }
                if (initValue == null)
                    continue;
                int counter = 0;
                VIR toTestIR = null;
                for (VIR ir : loopBlock) {
                    if (ir.getWrite() == loopReg) {
                        toTestIR = ir;
                        counter++;
                        if (counter > 1)
                            break;
                    }
                }
                if (counter > 1)
                    continue;
                if (!(toTestIR instanceof BinaryVIR loopIR))
                    continue;
                if (!(loopIR.left == loopReg && loopIR.right instanceof Value) && !(loopIR.left instanceof Value && loopIR.right == loopReg))
                    continue;
                boolean flag = false;
                for (VIR ir : judgeBlock) {
                    if (ir.getWrite() == loopReg) {
                        flag = true;
                        break;
                    }
                }
                if (flag)
                    continue;
                // TODO see this? optimize the algorithm to make it bigger
                if (loopBlock.size() * loopLimit.intValue() > 10000)
                    continue;
                modified = true;
                Block unrolledBlock = new Block();
                Number curValue = initValue;
                while (true) {
                    for (int i = 0; i < judgeBlock.size() - 1; i++)
                        unrolledBlock.add(judgeBlock.get(i).copy());
                    if (!judgeLoop(cond, curValue, loopLimit))
                        break;
                    for (int i = 0; i < loopBlock.size() - 1; i++) {
                        VIR ir = loopBlock.get(i);
                        if (ir != loopIR)
                            unrolledBlock.add(ir.copy());
                        else {
                            curValue = nextValue(loopIR, curValue);
                            unrolledBlock.add(new LiVIR(loopReg, curValue));
                        }
                    }
                }
                blocks.set(blocks.indexOf(judgeBlock), unrolledBlock);
                blocks.remove(loopBlock);
                initBlock.set(initBlock.size() - 1, new JumpVIR(unrolledBlock));
                unrolledBlock.add(new JumpVIR(endBlock));
                break;
            }
        }
        return modified;
    }

    private Number nextValue(BinaryVIR loopIR, Number curValue) {
        if (curValue instanceof Integer) {
            int leftValue, rightValue;
            if (loopIR.left instanceof Value value) {
                leftValue = value.intValue();
                rightValue = curValue.intValue();
            } else {
                leftValue = curValue.intValue();
                rightValue = ((Value) loopIR.right).intValue();
            }
            return switch (loopIR.type) {
                case ADD -> leftValue + rightValue;
                case SUB -> leftValue - rightValue;
                case MUL -> leftValue * rightValue;
                case DIV -> leftValue / rightValue;
                case MOD -> leftValue % rightValue;
                case EQ -> leftValue == rightValue ? 1 : 0;
                case NE -> leftValue != rightValue ? 1 : 0;
                case GE -> leftValue >= rightValue ? 1 : 0;
                case GT -> leftValue > rightValue ? 1 : 0;
                case LE -> leftValue <= rightValue ? 1 : 0;
                case LT -> leftValue < rightValue ? 1 : 0;
            };
        } else {
            float leftValue, rightValue;
            if (loopIR.left instanceof Value value) {
                leftValue = value.floatValue();
                rightValue = curValue.intValue();
            } else {
                leftValue = curValue.intValue();
                rightValue = ((Value) loopIR.right).floatValue();
            }
            return switch (loopIR.type) {
                case ADD -> leftValue + rightValue;
                case SUB -> leftValue - rightValue;
                case MUL -> leftValue * rightValue;
                case DIV -> leftValue / rightValue;
                case MOD -> leftValue % rightValue;
                case EQ -> leftValue == rightValue ? 1 : 0;
                case NE -> leftValue != rightValue ? 1 : 0;
                case GE -> leftValue >= rightValue ? 1 : 0;
                case GT -> leftValue > rightValue ? 1 : 0;
                case LE -> leftValue <= rightValue ? 1 : 0;
                case LT -> leftValue < rightValue ? 1 : 0;
            };
        }
    }

    private boolean judgeLoop(BranchVIR.Type cond, Number curValue, Number loopLimit) {
        if (curValue instanceof Integer && loopLimit instanceof Integer)
            return switch (cond) {
                case EQ -> curValue.intValue() == loopLimit.intValue();
                case NE -> curValue.intValue() != loopLimit.intValue();
                case GE -> curValue.intValue() >= loopLimit.intValue();
                case GT -> curValue.intValue() > loopLimit.intValue();
                case LE -> curValue.intValue() <= loopLimit.intValue();
                case LT -> curValue.intValue() < loopLimit.intValue();
            };
        else
            return switch (cond) {
                case EQ -> curValue.floatValue() == loopLimit.floatValue();
                case NE -> curValue.floatValue() != loopLimit.floatValue();
                case GE -> curValue.floatValue() >= loopLimit.floatValue();
                case GT -> curValue.floatValue() > loopLimit.floatValue();
                case LE -> curValue.floatValue() <= loopLimit.floatValue();
                case LT -> curValue.floatValue() < loopLimit.floatValue();
            };
    }

    private Map<Block, Set<Block>> analyzePrevBlocks(List<Block> blocks) {
        Map<Block, Set<Block>> prevBlockMap = new HashMap<>();
        for (Block block : blocks)
            prevBlockMap.put(block, new HashSet<>());
        for (Block block : blocks) {
            if (block.getLast() instanceof BranchVIR branchVIR) {
                prevBlockMap.get(branchVIR.trueBlock).add(block);
                prevBlockMap.get(branchVIR.falseBlock).add(block);
            } else if (block.getLast() instanceof JumpVIR jumpVIR)
                prevBlockMap.get(jumpVIR.target).add(block);
        }
        return prevBlockMap;
    }
}
