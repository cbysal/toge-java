package compile.codegen.regalloc;

import compile.codegen.Label;
import compile.codegen.Reg;
import compile.codegen.MReg;
import compile.codegen.mirgen.MachineFunction;
import compile.codegen.mirgen.mir.*;
import compile.codegen.VReg;
import compile.vir.type.BasicType;
import compile.vir.Argument;

import java.util.*;

public class RegAllocatorForSingleFunc {
    private final MachineFunction func;
    private final int paramInnerSize;
    private final List<MReg> iCallerRegs;
    private final List<MReg> fCallerRegs;
    private final List<MReg> iCalleeRegs = new ArrayList<>();
    private final List<MReg> fCalleeRegs = new ArrayList<>();
    private int funcParamSize, alignSize, spillSize, localSize;
    private int savedRegSize;
    private int callAddrSize;

    public RegAllocatorForSingleFunc(MachineFunction func) {
        this.func = func;
        this.iCallerRegs = MReg.I_CALLER_REGS.subList(0, func.getICallerNum());
        this.fCallerRegs = MReg.F_CALLER_REGS.subList(0, func.getFCallerNum());
        this.paramInnerSize = (func.getICallerNum() + func.getFCallerNum()) * 8;
    }

    public void allocate() {
        solveSpill();
        Map<VReg, MReg> vRegToMReg = calcVRegToMReg();
        func.getIrs().replaceAll(ir -> ir.replaceReg(vRegToMReg));
        makeFrameInfo();
        pushFrame();
        popFrame();
        replaceFakeMIRs();
    }

    private List<Block> calcBlocks() {
        List<MIR> irs = func.getIrs();
        Map<Label, Integer> labelIdMap = new HashMap<>();
        for (int i = 0; i < irs.size(); i++)
            if (irs.get(i) instanceof LabelMIR labelMIR)
                labelIdMap.put(labelMIR.label, i);
        Set<Integer> begins = new HashSet<>();
        begins.add(0);
        Map<Integer, Integer> jumpIdMap = new HashMap<>();
        Map<Integer, Boolean> isBranchMap = new HashMap<>();
        for (int i = 0; i < irs.size(); i++) {
            if (irs.get(i) instanceof BMIR bMIR) {
                begins.add(i + 1);
                jumpIdMap.put(i, labelIdMap.get(bMIR.label));
                isBranchMap.put(i, bMIR.hasCond());
                continue;
            }
            if (irs.get(i) instanceof LabelMIR)
                begins.add(i);
        }
        begins.add(irs.size());
        List<Integer> sortedBegins = new ArrayList<>(begins);
        sortedBegins.sort(Integer::compare);
        List<Block> blocks = new ArrayList<>();
        Map<Integer, Block> blockBeginMap = new HashMap<>();
        for (int i = 0; i < sortedBegins.size() - 1; i++) {
            int begin = sortedBegins.get(i);
            int end = sortedBegins.get(i + 1);
            Block block = new Block(begin, end);
            blocks.add(block);
            blockBeginMap.put(begin, block);
        }
        for (Block block : blocks) {
            int end = block.getEnd();
            if (isBranchMap.get(end - 1) != null) {
                block.addNext(blockBeginMap.get(jumpIdMap.get(end - 1)));
                boolean isBranch = isBranchMap.get(end - 1);
                if (isBranch)
                    block.addNext(blockBeginMap.get(end));
                continue;
            }
            Block next = blockBeginMap.get(end);
            if (next != null)
                block.addNext(next);
        }
        return blocks;
    }

    private Map<Reg, Set<Reg>> calcConflictMap() {
        Map<Reg, Set<Integer>> lifespans = calcLifespans();
        List<Set<Reg>> regsInEachIR = new ArrayList<>();
        for (int i = 0; i < func.getIrs().size(); i++)
            regsInEachIR.add(new HashSet<>());
        for (Map.Entry<Reg, Set<Integer>> lifespan : lifespans.entrySet())
            for (int id : lifespan.getValue())
                regsInEachIR.get(id).add(lifespan.getKey());
        Map<Reg, Set<Reg>> conflictMap = new HashMap<>();
        for (Set<Reg> regs : regsInEachIR) {
            for (Reg reg : regs) {
                Set<Reg> conflicts = conflictMap.getOrDefault(reg, new HashSet<>());
                conflicts.addAll(regs);
                conflictMap.put(reg, conflicts);
            }
        }
        for (Map.Entry<Reg, Set<Reg>> entry : conflictMap.entrySet())
            entry.getValue().remove(entry.getKey());
        return conflictMap;
    }

    private void calcInOut(List<Block> blocks) {
        boolean toContinue;
        do {
            toContinue = false;
            for (int i = blocks.size() - 1; i >= 0; i--) {
                Block block = blocks.get(i);
                int sizeBefore = block.sizeOfInOut();
                block.calcIn();
                block.calcOut();
                int sizeAfter = block.sizeOfInOut();
                if (sizeBefore != sizeAfter)
                    toContinue = true;
            }
        } while (toContinue);
    }

    private Map<Reg, Set<Integer>> calcLifespans() {
        List<MIR> irs = func.getIrs();
        List<Block> blocks = calcBlocks();
        calcUseDef(blocks);
        calcInOut(blocks);
        Map<Reg, Set<Integer>> lifespans = new HashMap<>();
        for (Block block : blocks) {
            Set<Reg> regs = block.getOut();
            for (int i = block.getEnd() - 1; i >= block.getBegin(); i--) {
                for (Reg reg : regs) {
                    Set<Integer> lifespan = lifespans.getOrDefault(reg, new HashSet<>());
                    lifespan.add(i);
                    lifespans.put(reg, lifespan);
                }
                MIR ir = irs.get(i);
                for (Reg reg : ir.getWrite()) {
                    Set<Integer> lifespan = lifespans.getOrDefault(reg, new HashSet<>());
                    lifespan.add(i);
                    lifespans.put(reg, lifespan);
                    regs.remove(reg);
                }
                for (Reg reg : ir.getRead()) {
                    Set<Integer> lifespan = lifespans.getOrDefault(reg, new HashSet<>());
                    lifespan.add(i);
                    lifespans.put(reg, lifespan);
                    regs.add(reg);
                }
            }
        }
        return lifespans;
    }

    private void calcUseDef(List<Block> blocks) {
        List<MIR> irs = func.getIrs();
        for (Block block : blocks) {
            for (int i = block.getBegin(); i < block.getEnd(); i++) {
                MIR ir = irs.get(i);
                if (ir instanceof CallMIR callMIR) {
                    int iSize = 0, fSize = 0;
                    for (Argument arg : callMIR.func.getArgs()) {
                        if (arg.getType() == BasicType.FLOAT && fSize < MReg.F_CALLER_REGS.size()) {
                            if (!block.containsInDef(MReg.F_CALLER_REGS.get(fSize)))
                                block.addUse(MReg.F_CALLER_REGS.get(fSize));
                            fSize++;
                        } else if (iSize < MReg.I_CALLER_REGS.size()) {
                            if (!block.containsInDef(MReg.I_CALLER_REGS.get(iSize)))
                                block.addUse(MReg.I_CALLER_REGS.get(iSize));
                            iSize++;
                        }
                    }
                    for (MReg reg : MReg.I_CALLER_REGS)
                        block.addDef(reg);
                    for (MReg reg : MReg.F_CALLER_REGS)
                        block.addDef(reg);
                }
                for (Reg reg : ir.getRead())
                    if (!block.containsInDef(reg))
                        block.addUse(reg);
                for (Reg reg : ir.getWrite())
                    block.addDef(reg);
            }
        }
    }

    private Map<VReg, MReg> calcVRegToMReg() {
        Map<Reg, Set<Reg>> conflictMap = calcConflictMap();
        Map<VReg, MReg> vRegToMRegMap = new HashMap<>();
        for (Reg toAllocateReg : conflictMap.keySet()) {
            if (toAllocateReg instanceof VReg vReg) {
                List<MReg> regs = vReg.getType() == BasicType.FLOAT ? MReg.F_REGS : MReg.I_REGS;
                Set<MReg> usedRegs = new HashSet<>();
                for (Reg reg : conflictMap.get(vReg)) {
                    if (reg instanceof VReg vReg1) {
                        MReg mReg = vRegToMRegMap.get(vReg1);
                        if (mReg != null)
                            usedRegs.add(mReg);
                        continue;
                    }
                    if (reg instanceof MReg mReg) {
                        usedRegs.add(mReg);
                        continue;
                    }
                    throw new RuntimeException();
                }
                for (MReg mReg : regs) {
                    if (usedRegs.contains(mReg))
                        continue;
                    vRegToMRegMap.put(vReg, mReg);
                    break;
                }
            }
        }
        return vRegToMRegMap;
    }

    private void makeFrameInfo() {
        funcParamSize = Integer.max(func.getMaxFuncParamNum() - MReg.I_CALLER_REGS.size(), 0) * 8;
        localSize = func.getLocalSize();
        Set<MReg> usedICalleeRegs = new HashSet<>();
        Set<MReg> usedFCalleeRegs = new HashSet<>();
        callAddrSize = 0;
        for (MIR ir : func.getIrs()) {
            if (ir instanceof CallMIR)
                callAddrSize = 8;
            for (Reg reg : ir.getRegs())
                if (reg instanceof MReg mReg) {
                    if (MReg.I_CALLEE_REGS.contains(mReg)) {
                        usedICalleeRegs.add(mReg);
                        continue;
                    }
                    if (MReg.F_CALLEE_REGS.contains(mReg))
                        usedFCalleeRegs.add(mReg);
                }
        }
        for (MReg reg : MReg.I_CALLEE_REGS)
            if (usedICalleeRegs.contains(reg))
                iCalleeRegs.add(reg);
        for (MReg reg : MReg.F_CALLEE_REGS)
            if (usedFCalleeRegs.contains(reg))
                fCalleeRegs.add(reg);
        savedRegSize = (iCalleeRegs.size() + fCalleeRegs.size()) * 8;
        alignSize = (funcParamSize + spillSize + localSize + paramInnerSize + savedRegSize + callAddrSize) % 8;
    }

    private void popFrame() {
        List<MReg> toSaveRegs = new ArrayList<>();
        if (callAddrSize != 0)
            toSaveRegs.add(MReg.RA);
        List.of(iCallerRegs, fCallerRegs, iCalleeRegs, fCalleeRegs).forEach(toSaveRegs::addAll);
        int totalSize = toSaveRegs.size() * 8 + funcParamSize + alignSize + spillSize + localSize;
        if (totalSize > 0) {
            if (totalSize < 2048)
                func.addIR(new RriMIR(RriMIR.Op.ADDI, MReg.SP, MReg.SP, totalSize));
            else {
                func.addIR(new LiMIR(MReg.T0, totalSize));
                func.addIR(new RrrMIR(RrrMIR.Op.ADD, MReg.SP, MReg.SP, MReg.T0));
            }
        }
        for (int i = 0; i < toSaveRegs.size(); i++) {
            MReg toSaveReg = toSaveRegs.get(i);
            if (iCallerRegs.contains(toSaveReg) || fCallerRegs.contains(toSaveReg))
                continue;
            func.addIR(new LoadMIR(toSaveReg, MReg.SP, -8 * (i + 1), toSaveReg.getType() == BasicType.I32 ? 8 : 4));
        }
    }

    private void pushFrame() {
        List<MIR> irs = func.getIrs();
        List<MIR> headIRs = new ArrayList<>();
        List<MReg> toSaveRegs = new ArrayList<>();
        if (callAddrSize != 0)
            toSaveRegs.add(MReg.RA);
        List.of(iCallerRegs, fCallerRegs, iCalleeRegs, fCalleeRegs).forEach(toSaveRegs::addAll);
        for (int i = 0; i < toSaveRegs.size(); i++) {
            MReg toSaveReg = toSaveRegs.get(i);
            headIRs.add(new StoreMIR(toSaveReg, MReg.SP, -8 * (i + 1), toSaveReg.getType() == BasicType.I32 ? 8 : 4));
        }
        int totalSize = toSaveRegs.size() * 8 + funcParamSize + alignSize + spillSize + localSize;
        if (totalSize > 0 && totalSize <= 255)
            headIRs.add(new RriMIR(RriMIR.Op.ADDI, MReg.SP, MReg.SP, -totalSize));
        else if (totalSize > 255) {
            headIRs.add(new LiMIR(MReg.T0, totalSize));
            headIRs.add(new RrrMIR(RrrMIR.Op.SUB, MReg.SP, MReg.SP, MReg.T0));
        }
        irs.addAll(0, headIRs);
    }

    private void replaceFakeMIRs() {
        List<MIR> irs = func.getIrs();
        for (int i = 0; i < irs.size(); i++) {
            MIR ir = irs.get(i);
            if (ir instanceof AddRegLocalMIR addRegLocalMIR) {
                int totalSize = funcParamSize + alignSize + spillSize + addRegLocalMIR.imm;
                if (totalSize < 2048)
                    irs.set(i, new RriMIR(RriMIR.Op.ADDI, addRegLocalMIR.dest, MReg.SP, totalSize));
                else {
                    irs.set(i, new LiMIR(MReg.T0, totalSize));
                    irs.add(i + 1, new RrrMIR(RrrMIR.Op.ADD, addRegLocalMIR.dest, MReg.SP, MReg.T0));
                    i++;
                }
                continue;
            }
            if (ir instanceof LoadItemMIR loadItemMIR) {
                int totalSize = switch (loadItemMIR.item) {
                    case SPILL -> funcParamSize + alignSize + loadItemMIR.imm;
                    case LOCAL -> funcParamSize + alignSize + spillSize + loadItemMIR.imm;
                    case PARAM_INNER ->
                            funcParamSize + alignSize + spillSize + localSize + savedRegSize + loadItemMIR.imm;
                    case PARAM_OUTER ->
                            funcParamSize + alignSize + spillSize + localSize + paramInnerSize + savedRegSize + callAddrSize + loadItemMIR.imm;
                };
                int size = switch (loadItemMIR.item) {
                    case LOCAL -> 4;
                    case SPILL, PARAM_INNER, PARAM_OUTER -> switch (loadItemMIR.dest.getType()) {
                        case BasicType.FLOAT -> 4;
                        case BasicType.I32 -> 8;
                        default -> throw new IllegalStateException("Unexpected value: " + loadItemMIR.dest.getType());
                    };
                };
                if (totalSize < 2048) {
                    irs.set(i, new LoadMIR(loadItemMIR.dest, MReg.SP, totalSize, size));
                } else {
                    irs.set(i, new LiMIR(MReg.T0, totalSize));
                    irs.add(i + 1, new RrrMIR(RrrMIR.Op.ADD, MReg.T0, MReg.SP, MReg.T0));
                    irs.add(i + 2, new LoadMIR(loadItemMIR.dest, MReg.T0, 0, size));
                    i += 2;
                }
                continue;
            }
            if (ir instanceof StoreItemMIR storeItemMIR) {
                int totalSize = switch (storeItemMIR.item) {
                    case PARAM_CALL -> storeItemMIR.imm;
                    case SPILL -> funcParamSize + alignSize + storeItemMIR.imm;
                    case LOCAL -> funcParamSize + alignSize + spillSize + storeItemMIR.imm;
                    case PARAM_INNER ->
                            funcParamSize + alignSize + spillSize + localSize + savedRegSize + storeItemMIR.imm;
                    case PARAM_OUTER ->
                            funcParamSize + alignSize + spillSize + localSize + paramInnerSize + savedRegSize + callAddrSize + storeItemMIR.imm;
                };
                int size = switch (storeItemMIR.item) {
                    case LOCAL -> 4;
                    case PARAM_CALL, PARAM_INNER, PARAM_OUTER, SPILL -> switch (storeItemMIR.src.getType()) {
                        case BasicType.FLOAT -> 4;
                        case BasicType.I32 -> 8;
                        default -> throw new IllegalStateException("Unexpected value: " + storeItemMIR.src.getType());
                    };
                };
                if (totalSize < 2048) {
                    irs.set(i, new StoreMIR(storeItemMIR.src, MReg.SP, totalSize, size));
                } else {
                    irs.set(i, new LiMIR(MReg.T0, totalSize));
                    irs.add(i + 1, new RrrMIR(RrrMIR.Op.ADD, MReg.T0, MReg.SP, MReg.T0));
                    irs.add(i + 2, new StoreMIR(storeItemMIR.src, MReg.T0, 0, size));
                    i += 2;
                }
            }
        }
    }

    private void solveSpill() {
        spillSize = 0;
        boolean toContinueOuter;
        do {
            toContinueOuter = false;
            Map<Reg, Set<Reg>> conflictMap = calcConflictMap();
            Set<VReg> allocatedVRegs = new HashSet<>();
            Map<VReg, MReg> vReg2MRegMap = new HashMap<>();
            Map<VReg, Integer> spilledRegs = new HashMap<>();
            boolean toContinueInner;
            do {
                toContinueInner = false;
                for (Reg toAllocateReg : conflictMap.keySet()) {
                    if (toAllocateReg instanceof VReg vReg) {
                        allocatedVRegs.add(vReg);
                        boolean toSpill = true;
                        List<MReg> regs = vReg.getType() == BasicType.FLOAT ? MReg.F_REGS : MReg.I_REGS;
                        Set<MReg> usedRegs = new HashSet<>();
                        for (Reg reg : conflictMap.get(vReg)) {
                            if (reg instanceof VReg vReg1) {
                                MReg mReg = vReg2MRegMap.get(vReg1);
                                if (mReg != null)
                                    usedRegs.add(mReg);
                                continue;
                            }
                            if (reg instanceof MReg mReg) {
                                usedRegs.add(mReg);
                                continue;
                            }
                            throw new RuntimeException();
                        }
                        for (MReg mReg : regs) {
                            if (usedRegs.contains(mReg))
                                continue;
                            vReg2MRegMap.put(vReg, mReg);
                            toSpill = false;
                            break;
                        }
                        if (toSpill) {
                            VReg toSpillReg = null;
                            int maxVal = 0;
                            for (VReg reg : allocatedVRegs) {
                                if (conflictMap.get(reg).size() > maxVal) {
                                    toSpillReg = reg;
                                    maxVal = conflictMap.get(reg).size();
                                }
                            }
                            spilledRegs.put(toSpillReg, spillSize);
                            spillSize += 8;
                            allocatedVRegs.remove(toSpillReg);
                            conflictMap.remove(toSpillReg);
                            for (Map.Entry<Reg, Set<Reg>> entry : conflictMap.entrySet())
                                entry.getValue().remove(toSpillReg);
                            toContinueInner = true;
                            toContinueOuter = true;
                            break;
                        }
                    }
                }
            } while (toContinueInner);
            for (Map.Entry<VReg, Integer> toSpill : spilledRegs.entrySet()) {
                VReg reg = toSpill.getKey();
                int offset = toSpill.getValue();
                List<MIR> newIRs = new ArrayList<>();
                for (MIR ir : func.getIrs()) {
                    if (ir.getRegs().contains(reg))
                        newIRs.addAll(ir.spill(reg, offset));
                    else
                        newIRs.add(ir);
                }
                func.getIrs().clear();
                func.getIrs().addAll(newIRs);
            }
        } while (toContinueOuter);
    }

    private static class Block {
        private final int begin, end;
        private final Set<Reg> liveUse = new HashSet<>(), liveDef = new HashSet<>();
        private final Set<Reg> liveIn = new HashSet<>(), liveOut = new HashSet<>();
        private final Set<Block> nexts = new HashSet<>();

        public Block(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        public void addUse(Reg reg) {
            liveUse.add(reg);
        }

        public void addDef(Reg reg) {
            liveDef.add(reg);
        }

        public void addNext(Block block) {
            nexts.add(block);
        }

        public void calcIn() {
            liveIn.clear();
            liveIn.addAll(liveOut);
            liveIn.removeAll(liveDef);
            liveIn.addAll(liveUse);
        }

        public void calcOut() {
            for (Block next : nexts)
                liveOut.addAll(next.liveIn);
        }

        public boolean containsInDef(Reg reg) {
            return liveDef.contains(reg);
        }

        public int getBegin() {
            return begin;
        }

        public int getEnd() {
            return end;
        }

        public Set<Reg> getRegs() {
            Set<Reg> regs = new HashSet<>();
            regs.addAll(liveUse);
            regs.addAll(liveDef);
            regs.addAll(liveIn);
            regs.addAll(liveOut);
            return regs;
        }

        public Set<Reg> getOut() {
            return liveOut;
        }

        public int sizeOfInOut() {
            return liveIn.size() + liveOut.size();
        }
    }
}
