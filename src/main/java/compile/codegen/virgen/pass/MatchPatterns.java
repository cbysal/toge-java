package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;
import compile.symbol.Type;
import compile.symbol.Value;

import java.util.*;

public class MatchPatterns extends Pass {
    public MatchPatterns(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        boolean modified = matchAbs();
        modified |= matchMulThenMod();
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

    private boolean matchMulThenMod() {
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
                if (!(item1 instanceof VReg loopVar) || !(item2 instanceof VReg loopNum))
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
                if (cond != BranchVIR.Type.LT || judgeBlock.size() != 1 || loopBlock.size() != 4)
                    continue;
                if (prevBlockMap.get(judgeBlock).size() != 2)
                    continue;
                if (prevBlockMap.get(loopBlock).size() != 1)
                    continue;
                int selfAddIndex = -1;
                for (int i = 0; i < loopBlock.size(); i++) {
                    VIR ir = loopBlock.get(i);
                    if (ir instanceof BinaryVIR binaryVIR && binaryVIR.type == BinaryVIR.Type.ADD && binaryVIR.target == loopVar && binaryVIR.left == loopVar && binaryVIR.right instanceof Value value && value.getType() == Type.INT && value.intValue() == 1) {
                        selfAddIndex = i;
                        break;
                    }
                }
                if (selfAddIndex == -1)
                    continue;
                List<VIR> mulAndMod = new ArrayList<>();
                for (int i = 0; i < loopBlock.size() - 1; i++)
                    if (i != selfAddIndex)
                        mulAndMod.add(loopBlock.get(i));
                if (mulAndMod.size() != 2)
                    continue;
                if (!(mulAndMod.get(0) instanceof BinaryVIR ir1 && ir1.type == BinaryVIR.Type.ADD && mulAndMod.get(1) instanceof BinaryVIR ir2 && ir2.type == BinaryVIR.Type.MOD))
                    continue;
                if (!(ir1.target == ir2.left && ir1.left == ir2.target))
                    continue;
                if (!(ir1.right instanceof Value mulValue && ir2.right instanceof Value modValue))
                    continue;
                VReg target = ir2.target;
                boolean zeroCheck = true;
                for (int i = initBlock.size() - 2; i >= 0; i--) {
                    VIR ir = initBlock.get(i);
                    if (ir instanceof LiVIR liVIR) {
                        if (liVIR.target == target && liVIR.value.intValue() != 0) {
                            zeroCheck = false;
                            break;
                        }
                        if (liVIR.target == loopVar && liVIR.value.intValue() != 0) {
                            zeroCheck = false;
                            break;
                        }
                    }
                }
                if (!zeroCheck)
                    continue;
                VReg midReg1 = new VReg(target.getType(), target.getSize());
                VReg midReg2 = new VReg(target.getType(), target.getSize());
                initBlock.add(initBlock.size() - 1, new BinaryVIR(BinaryVIR.Type.MOD, midReg1, loopNum, modValue));
                initBlock.add(initBlock.size() - 1, new BinaryVIR(BinaryVIR.Type.MUL, midReg2, midReg1, mulValue));
                initBlock.add(initBlock.size() - 1, new BinaryVIR(BinaryVIR.Type.MOD, target, midReg2, modValue));
                initBlock.set(initBlock.size() - 1, new JumpVIR(endBlock));
                blocks.remove(loopBlock);
                blocks.remove(judgeBlock);
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
