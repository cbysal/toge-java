package compile.codegen.mirgen;

import compile.codegen.Label;
import compile.codegen.MReg;
import compile.codegen.VReg;
import compile.codegen.mirgen.mir.*;
import compile.codegen.mirgen.trans.MIROpTrans;
import compile.symbol.*;
import compile.vir.Block;
import compile.vir.Argument;
import compile.vir.GlobalVariable;
import compile.vir.VirtualFunction;
import compile.vir.contant.ConstantNumber;
import compile.vir.ir.*;
import compile.vir.type.BasicType;
import compile.vir.value.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MIRGenerator {
    private final Set<GlobalVariable> globals;
    private final Map<String, VirtualFunction> vFuncs;
    private final Map<String, MachineFunction> mFuncs = new HashMap<>();
    private boolean isProcessed = false;

    public MIRGenerator(Set<GlobalVariable> globals, Map<String, VirtualFunction> vFuncs) {
        this.globals = globals;
        this.vFuncs = vFuncs;
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        vir2Mir();
    }

    private Pair<Integer, Integer> getCallerNumbers(FuncSymbol func) {
        int iSize = 0, fSize = 0;
        for (Argument arg : func.getArgs())
            if (arg.getType() == BasicType.FLOAT)
                fSize = Integer.min(fSize + 1, MReg.F_CALLER_REGS.size());
            else
                iSize = Integer.min(iSize + 1, MReg.I_CALLER_REGS.size());
        return Pair.of(iSize, fSize);
    }

    public Map<String, MachineFunction> getFuncs() {
        checkIfIsProcessed();
        return mFuncs;
    }

    public Set<GlobalVariable> getGlobals() {
        checkIfIsProcessed();
        return globals;
    }

    private Pair<Integer, Map<AllocaVIR, Integer>> calcLocalOffsets(Block block) {
        int localSize = 0;
        Map<AllocaVIR, Integer> localOffsets = new HashMap<>();
        for (VIR ir : block) {
            if (!(ir instanceof AllocaVIR allocaVIR))
                break;
            int size = allocaVIR.getType().getBaseType().getSize() / 8;
            localOffsets.put(allocaVIR, localSize);
            localSize += size;
        }
        return Pair.of(localSize, localOffsets);
    }

    private Map<Argument, Pair<Boolean, Integer>> calcArgOffsets(List<Argument> args) {
        Map<Argument, Pair<Boolean, Integer>> argOffsets = new HashMap<>();
        int iCallerNum = 0, fCallerNum = 0;
        for (Argument arg : args) {
            if (arg.getType() instanceof BasicType && arg.getType() == BasicType.FLOAT)
                fCallerNum++;
            else
                iCallerNum++;
        }
        iCallerNum = Integer.min(iCallerNum, MReg.I_CALLER_REGS.size());
        fCallerNum = Integer.min(fCallerNum, MReg.F_CALLER_REGS.size());
        int iSize = 0, fSize = 0;
        for (Argument arg : args) {
            if (!(arg.getType() instanceof BasicType) || arg.getType() == BasicType.I32) {
                if (iSize < MReg.I_CALLER_REGS.size())
                    argOffsets.put(arg, Pair.of(true, (iCallerNum + fCallerNum - iSize - 1) * 8));
                else
                    argOffsets.put(arg, Pair.of(false, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                iSize++;
            } else {
                if (fSize < MReg.F_CALLER_REGS.size())
                    argOffsets.put(arg, Pair.of(true, (fCallerNum - fSize - 1) * 8));
                else
                    argOffsets.put(arg, Pair.of(false, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                fSize++;
            }
        }
        return argOffsets;
    }

    private void vir2Mir() {
        for (Map.Entry<String, VirtualFunction> func : vFuncs.entrySet())
            mFuncs.put(func.getKey(), vir2MirSingle(func.getValue()));
    }

    private MachineFunction vir2MirSingle(VirtualFunction vFunc) {
        Map<Argument, Pair<Boolean, Integer>> argOffsets = calcArgOffsets(vFunc.getSymbol().getArgs());
        Pair<Integer, Map<AllocaVIR, Integer>> locals = calcLocalOffsets(vFunc.getBlocks().getFirst());
        Pair<Integer, Integer> callerNums = getCallerNumbers(vFunc.getSymbol());
        MachineFunction mFunc = new MachineFunction(vFunc.getSymbol(), locals.getLeft(), callerNums.getLeft(), callerNums.getRight());
        LabelMIR retLabelMIR = new LabelMIR(new Label());
        Map<VReg, MReg> replaceMap = new HashMap<>();
        Map<VIR, VReg> virRegMap = new HashMap<>();
        for (Block block : vFunc.getBlocks()) {
            for (VIR ir : block) {
                virRegMap.put(ir, new VReg(ir.getType()));
            }
        }
        Map<AllocaVIR, Integer> localOffsets = locals.getRight();
        for (Block block : vFunc.getBlocks()) {
            mFunc.addIR(new LabelMIR(block.getLabel()));
            for (VIR vir : block) {
                if (vir instanceof BinaryVIR binaryVIR)
                    MIROpTrans.transBinary(mFunc.getIrs(), virRegMap, binaryVIR);
                if (vir instanceof BranchVIR branchVIR)
                    MIROpTrans.transBranch(mFunc.getIrs(), virRegMap, branchVIR);
                if (vir instanceof CallVIR callVIR) {
                    int paramNum = MIROpTrans.transCall(mFunc.getIrs(), virRegMap, callVIR, localOffsets);
                    mFunc.setMaxFuncParamNum(Integer.max(mFunc.getMaxFuncParamNum(), paramNum));
                }
                if (vir instanceof JumpVIR jumpVIR)
                    mFunc.getIrs().add(new BMIR(null, null, null, jumpVIR.target.getLabel()));
                if (vir instanceof LiVIR liVIR)
                    MIROpTrans.transLI(mFunc.getIrs(), virRegMap, liVIR);
                if (vir instanceof GetElementPtrVIR getElementPtrVIR) {
                    Value pointer = getElementPtrVIR.getPointer();
                    if (pointer instanceof GlobalVariable global) {
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        VReg midReg3 = new VReg(BasicType.I32);
                        VReg midReg4 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg1, global));
                        mFunc.getIrs().add(new LiMIR(midReg2, getElementPtrVIR.getType().getBaseType().getSize() / 8));
                        switch (getElementPtrVIR.getIndexes().getLast()) {
                            case VIR ir -> mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, virRegMap.get(ir)));
                            case ConstantNumber value ->
                                    mFunc.getIrs().add(new LiMIR(midReg3, value.getType() == BasicType.I32 ? value.intValue() : Float.floatToIntBits(value.floatValue())));
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + getElementPtrVIR.getIndexes().getFirst());
                        }
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, virRegMap.get(getElementPtrVIR), midReg1, midReg4));
                        continue;
                    }
                    if (pointer instanceof Argument arg) {
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        VReg midReg3 = new VReg(BasicType.I32);
                        VReg midReg4 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, innerOffset.getRight()));
                        mFunc.getIrs().add(new LiMIR(midReg2, getElementPtrVIR.getType().getBaseType().getSize() / 8));
                        switch (getElementPtrVIR.getIndexes().getLast()) {
                            case VIR ir -> mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg3, virRegMap.get(ir)));
                            case ConstantNumber value ->
                                    mFunc.getIrs().add(new LiMIR(midReg3, value.getType() == BasicType.I32 ? value.intValue() : Float.floatToIntBits(value.floatValue())));
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + getElementPtrVIR.getIndexes().getLast());
                        }
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                        mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, virRegMap.get(getElementPtrVIR), midReg1, midReg4));
                    }
                    if (pointer instanceof AllocaVIR allocaVIR) {
                        if (getElementPtrVIR.getIndexes().size() == 2) {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            VReg midReg4 = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new AddRegLocalMIR(midReg1, localOffsets.get(allocaVIR)));
                            switch (getElementPtrVIR.getIndexes().getLast()) {
                                case VIR tempIR ->
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, virRegMap.get(tempIR)));
                                case ConstantNumber value ->
                                        mFunc.getIrs().add(new LiMIR(midReg2, value.getType() == BasicType.I32 ? value.intValue() : Float.floatToIntBits(value.floatValue())));
                                default ->
                                        throw new IllegalStateException("Unexpected value: " + getElementPtrVIR.getIndexes().getLast());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg3, pointer.getType().getBaseType().getBaseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, virRegMap.get(getElementPtrVIR), midReg1, midReg4));
                        } else {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            VReg midReg4 = new VReg(BasicType.I32);
                            mFunc.getIrs().add(new AddRegLocalMIR(midReg1, localOffsets.get(allocaVIR)));
                            switch (getElementPtrVIR.getIndexes().getLast()) {
                                case VIR tempIR ->
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, virRegMap.get(tempIR)));
                                case ConstantNumber value ->
                                        mFunc.getIrs().add(new LiMIR(midReg2, value.getType() == BasicType.I32 ? value.intValue() : Float.floatToIntBits(value.floatValue())));
                                default ->
                                        throw new IllegalStateException("Unexpected value: " + getElementPtrVIR.getIndexes().getLast());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg3, pointer.getType().getBaseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg4, midReg2, midReg3));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, virRegMap.get(getElementPtrVIR), midReg1, midReg4));
                        }
                        continue;
                    }
                    if (pointer instanceof VIR ir) {
                        if (getElementPtrVIR.getIndexes().size() == 2) {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            switch (getElementPtrVIR.getIndexes().getLast()) {
                                case VIR tempIR ->
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, virRegMap.get(tempIR)));
                                case ConstantNumber value ->
                                        mFunc.getIrs().add(new LiMIR(midReg1, value.getType() == BasicType.I32 ? value.intValue() : Float.floatToIntBits(value.floatValue())));
                                default ->
                                        throw new IllegalStateException("Unexpected value: " + getElementPtrVIR.getIndexes().getLast());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg2, pointer.getType().getBaseType().getBaseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg3, midReg1, midReg2));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, virRegMap.get(getElementPtrVIR), virRegMap.get(ir), midReg3));
                        } else {
                            VReg midReg1 = new VReg(BasicType.I32);
                            VReg midReg2 = new VReg(BasicType.I32);
                            VReg midReg3 = new VReg(BasicType.I32);
                            switch (getElementPtrVIR.getIndexes().getLast()) {
                                case VIR tempIR ->
                                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, virRegMap.get(tempIR)));
                                case ConstantNumber value ->
                                        mFunc.getIrs().add(new LiMIR(midReg1, value.getType() == BasicType.I32 ? value.intValue() : Float.floatToIntBits(value.floatValue())));
                                default ->
                                        throw new IllegalStateException("Unexpected value: " + getElementPtrVIR.getIndexes().getLast());
                            }
                            mFunc.getIrs().add(new LiMIR(midReg2, pointer.getType().getBaseType().getSize() / 8));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.MUL, midReg3, midReg1, midReg2));
                            mFunc.getIrs().add(new RrrMIR(RrrMIR.Op.ADD, virRegMap.get(getElementPtrVIR), virRegMap.get(ir), midReg3));
                        }
                    }
                }
                if (vir instanceof LoadVIR loadVIR) {
                    Value pointer = loadVIR.pointer;
                    if (pointer instanceof GlobalVariable global) {
                        VReg midReg = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg, global));
                        mFunc.getIrs().add(new LoadMIR(virRegMap.get(loadVIR), midReg, 0, 4));
                        continue;
                    }
                    if (pointer instanceof Argument arg) {
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, virRegMap.get(loadVIR), innerOffset.getRight()));
                    }
                    if (pointer instanceof AllocaVIR allocaVIR) {
                        VReg midReg = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new AddRegLocalMIR(midReg, localOffsets.get(allocaVIR)));
                        mFunc.getIrs().add(new LoadMIR(virRegMap.get(loadVIR), midReg, 0, allocaVIR.getType().getBaseType().getSize() / 8));
                        continue;
                    }
                    if (pointer instanceof VIR ir) {
                        mFunc.getIrs().add(new LoadMIR(virRegMap.get(loadVIR), virRegMap.get(ir), 0, ir.getType().getBaseType().getSize() / 8));
                    }
                }
                if (vir instanceof RetVIR retVIR) {
                    switch (retVIR.retVal) {
                        case VIR ir -> {
                            if (virRegMap.containsKey(ir)) {
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, retVIR.retVal.getType() == BasicType.I32 ? MReg.A0 : MReg.FA0, virRegMap.get(ir)));
                            } else {
                                VReg midReg = new VReg(retVIR.retVal.getType());
                                virRegMap.put(ir, midReg);
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, retVIR.retVal.getType() == BasicType.I32 ? MReg.A0 : MReg.FA0, midReg));
                            }
                        }
                        case VReg reg ->
                                mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, retVIR.retVal.getType() == BasicType.I32 ? MReg.A0 : MReg.FA0, reg));
                        case ConstantNumber value -> {
                            switch (value.getType()) {
                                case BasicType.I32 -> mFunc.getIrs().add(new LiMIR(MReg.A0, value.intValue()));
                                case BasicType.FLOAT ->
                                        mFunc.getIrs().add(new LiMIR(MReg.FA0, Float.floatToIntBits(value.floatValue())));
                                default -> throw new IllegalStateException("Unexpected value: " + value.getType());
                            }
                        }
                        case null -> {
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + retVIR.retVal);
                    }
                    mFunc.getIrs().add(new BMIR(null, null, null, retLabelMIR.label));
                }
                if (vir instanceof UnaryVIR unaryVIR)
                    MIROpTrans.transUnary(mFunc.getIrs(), virRegMap, unaryVIR);
                if (vir instanceof StoreVIR storeVIR) {
                    Value value = storeVIR.value;
                    Value pointer = storeVIR.pointer;
                    if (pointer instanceof GlobalVariable global) {
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LlaMIR(midReg1, global));
                        switch (value) {
                            case VIR ir -> mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, virRegMap.get(ir)));
                            case ConstantNumber v ->
                                    mFunc.getIrs().add(new LiMIR(midReg2, v.getType() == BasicType.I32 ? v.intValue() : Float.floatToIntBits(v.floatValue())));
                            default -> throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg2, midReg1, 0, 4));
                        continue;
                    }
                    if (pointer instanceof Argument arg) {
                        Pair<Boolean, Integer> innerOffset = argOffsets.get(arg);
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        mFunc.getIrs().add(new LoadItemMIR(innerOffset.getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, innerOffset.getRight()));
                        switch (value) {
                            case VIR ir -> mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg2, virRegMap.get(ir)));
                            case ConstantNumber v ->
                                    mFunc.getIrs().add(new LiMIR(midReg2, v.getType() == BasicType.I32 ? v.intValue() : Float.floatToIntBits(v.floatValue())));
                            default -> throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg2, midReg1, 0, 4));
                    }
                    if (pointer instanceof AllocaVIR allocaVIR) {
                        VReg midReg1 = new VReg(BasicType.I32);
                        VReg midReg2 = new VReg(BasicType.I32);
                        switch (value) {
                            case VIR tempIR ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg1, virRegMap.get(tempIR)));
                            case Argument arg ->
                                    mFunc.getIrs().add(new LoadItemMIR(argOffsets.get(arg).getLeft() ? LoadItemMIR.Item.PARAM_INNER : LoadItemMIR.Item.PARAM_OUTER, midReg1, argOffsets.get(arg).getRight()));
                            case ConstantNumber v ->
                                    mFunc.getIrs().add(new LiMIR(midReg1, v.getType() == BasicType.I32 ? v.intValue() : Float.floatToIntBits(v.floatValue())));
                            default -> throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new AddRegLocalMIR(midReg2, localOffsets.get(allocaVIR)));
                        mFunc.getIrs().add(new StoreMIR(midReg1, midReg2, 0, allocaVIR.getType().getBaseType().getSize() / 8));
                        continue;
                    }
                    if (pointer instanceof VIR ir) {
                        VReg midReg = new VReg(BasicType.I32);
                        switch (value) {
                            case VIR tempIR ->
                                    mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, midReg, virRegMap.get(tempIR)));
                            case ConstantNumber v ->
                                    mFunc.getIrs().add(new LiMIR(midReg, v.getType() == BasicType.I32 ? v.intValue() : Float.floatToIntBits(v.floatValue())));
                            default -> throw new IllegalStateException("Unexpected value: " + value);
                        }
                        mFunc.getIrs().add(new StoreMIR(midReg, virRegMap.get(ir), 0, ir.getType().getBaseType().getSize() / 8));
                    }
                }
            }
        }
        mFunc.getIrs().add(retLabelMIR);
        mFunc.getIrs().replaceAll(ir -> ir.replaceReg(replaceMap));
        return mFunc;
    }
}
