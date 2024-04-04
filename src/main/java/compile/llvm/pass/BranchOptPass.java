package compile.llvm.pass;

import compile.llvm.BasicBlock;
import compile.llvm.Function;
import compile.llvm.Module;
import compile.llvm.contant.ConstantNumber;
import compile.llvm.ir.BranchInst;
import compile.llvm.ir.Instruction;
import compile.llvm.ir.PHINode;
import compile.llvm.value.Use;

import java.util.HashSet;
import java.util.Set;

public class BranchOptPass extends FunctionPass {
    public BranchOptPass(Module module) {
        super(module);
    }

    @Override
    public boolean runOnFunction(Function func) {
        boolean modified = false;
        for (int i = 1; i < func.size(); i++) {
            BasicBlock block = func.get(i);
            Set<Use> uses = block.getUses();
            if (uses.isEmpty()) {
                if (block.getLast() instanceof BranchInst branchInst) {
                    Set<BasicBlock> nextBlocks = new HashSet<>();
                    if (branchInst.isConditional()) {
                        nextBlocks.add(branchInst.getOperand(1));
                        nextBlocks.add(branchInst.getOperand(2));
                    } else {
                        nextBlocks.add(branchInst.getOperand(0));
                    }
                    for (BasicBlock nextBlock : nextBlocks) {
                        for (Instruction inst : nextBlock) {
                            if (inst instanceof PHINode phiNode) {
                                for (int j = 0; j < phiNode.size(); j++) {
                                    if (phiNode.getBlockValue(j).getLeft() == block) {
                                        phiNode.remove(j);
                                        j--;
                                    }
                                }
                            }
                        }
                    }
                }
                func.remove(i);
                i--;
                modified = true;
            } else if (uses.size() == 1) {
                BranchInst branchInst = (BranchInst) uses.iterator().next().getUser();
                if (!branchInst.isConditional()) {
                    BasicBlock prevBlock = branchInst.getBlock();
                    prevBlock.remove(prevBlock.size() - 1);
                    for (Instruction inst : block) {
                        inst.setBlock(prevBlock);
                        prevBlock.add(inst);
                    }
                    if (block.getLast() instanceof BranchInst nextBranchInst) {
                        Set<BasicBlock> nextBlocks = new HashSet<>();
                        if (nextBranchInst.isConditional()) {
                            nextBlocks.add(nextBranchInst.getOperand(1));
                            nextBlocks.add(nextBranchInst.getOperand(2));
                        } else {
                            nextBlocks.add(nextBranchInst.getOperand(0));
                        }
                        for (BasicBlock nextBlock : nextBlocks) {
                            for (Instruction inst : nextBlock) {
                                if (inst instanceof PHINode phiNode) {
                                    for (int j = 0; j < phiNode.size(); j++) {
                                        if (phiNode.getBlockValue(j).getLeft() == block) {
                                            phiNode.setBlockValue(j, prevBlock);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    func.remove(i);
                    i--;
                    modified = true;
                }
            }
        }
        for (int i = 0; i < func.size(); i++) {
            BasicBlock block = func.get(i);
            if (block.getLast() instanceof BranchInst branchInst && branchInst.isConditional() && branchInst.getOperand(0) instanceof ConstantNumber condValue) {
                branchInst.remove(0);
                Use use = branchInst.remove(condValue.intValue());
                BasicBlock nextBlock = (BasicBlock) use.getValue();
                for (Instruction inst : nextBlock) {
                    if (inst instanceof PHINode phiNode) {
                        for (int j = 0; j < phiNode.size(); j++) {
                            if (phiNode.getBlockValue(j).getLeft() == block) {
                                phiNode.remove(j);
                                j--;
                            }
                        }
                    }
                }
                modified = true;
            }
        }
        return modified;
    }
}
