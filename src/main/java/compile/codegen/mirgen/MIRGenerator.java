package compile.codegen.mirgen;

import compile.codegen.mirgen.mir.BMIR;
import compile.codegen.mirgen.mir.LabelMIR;
import compile.codegen.mirgen.mir.LiMIR;
import compile.codegen.mirgen.mir.RrMIR;
import compile.codegen.mirgen.trans.MIROpTrans;
import compile.vir.Block;
import compile.vir.VReg;
import compile.vir.VirtualFunction;
import compile.vir.ir.*;
import compile.vir.type.BasicType;
import compile.symbol.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MIRGenerator {
    private final Set<GlobalSymbol> globals;
    private final Map<String, VirtualFunction> vFuncs;
    private final Map<String, MachineFunction> mFuncs = new HashMap<>();
    private boolean isProcessed = false;

    public MIRGenerator(Set<GlobalSymbol> globals, Map<String, VirtualFunction> vFuncs) {
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
        for (ParamSymbol param : func.getParams())
            if (!param.isSingle() || param.getType() == BasicType.I32)
                iSize = Integer.min(iSize + 1, MReg.I_CALLER_REGS.size());
            else
                fSize = Integer.min(fSize + 1, MReg.F_CALLER_REGS.size());
        return Pair.of(iSize, fSize);
    }

    public Map<String, MachineFunction> getFuncs() {
        checkIfIsProcessed();
        return mFuncs;
    }

    public Set<GlobalSymbol> getGlobals() {
        checkIfIsProcessed();
        return globals;
    }

    private Pair<Integer, Map<Symbol, Integer>> calcLocalOffsets(List<LocalSymbol> locals) {
        int localSize = 0;
        Map<Symbol, Integer> localOffsets = new HashMap<>();
        for (LocalSymbol localSymbol : locals) {
            int size = localSymbol.size();
            localOffsets.put(localSymbol, localSize);
            localSize += size;
        }
        return Pair.of(localSize, localOffsets);
    }

    private Map<Symbol, Pair<Boolean, Integer>> calcParamOffsets(List<ParamSymbol> params) {
        Map<Symbol, Pair<Boolean, Integer>> paramOffsets = new HashMap<>();
        int iCallerNum = 0, fCallerNum = 0;
        for (ParamSymbol param : params) {
            if (param.isSingle() && param.getType() == BasicType.FLOAT)
                fCallerNum++;
            else
                iCallerNum++;
        }
        iCallerNum = Integer.min(iCallerNum, MReg.I_CALLER_REGS.size());
        fCallerNum = Integer.min(fCallerNum, MReg.F_CALLER_REGS.size());
        int iSize = 0, fSize = 0;
        for (ParamSymbol param : params) {
            if (!param.isSingle() || param.getType() == BasicType.I32) {
                if (iSize < MReg.I_CALLER_REGS.size())
                    paramOffsets.put(param, Pair.of(true, (iCallerNum + fCallerNum - iSize - 1) * 8));
                else
                    paramOffsets.put(param, Pair.of(false, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                iSize++;
            } else {
                if (fSize < MReg.F_CALLER_REGS.size())
                    paramOffsets.put(param, Pair.of(true, (fCallerNum - fSize - 1) * 8));
                else
                    paramOffsets.put(param, Pair.of(false, (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                fSize++;
            }
        }
        return paramOffsets;
    }

    private void vir2Mir() {
        for (Map.Entry<String, VirtualFunction> func : vFuncs.entrySet())
            mFuncs.put(func.getKey(), vir2MirSingle(func.getValue()));
    }

    private MachineFunction vir2MirSingle(VirtualFunction vFunc) {
        Map<Symbol, Pair<Boolean, Integer>> paramOffsets = calcParamOffsets(vFunc.getSymbol().getParams());
        Pair<Integer, Map<Symbol, Integer>> locals = calcLocalOffsets(vFunc.getLocals());
        Pair<Integer, Integer> callerNums = getCallerNumbers(vFunc.getSymbol());
        MachineFunction mFunc = new MachineFunction(vFunc.getSymbol(), locals.getLeft(), callerNums.getLeft(), callerNums.getRight());
        Map<VReg, MReg> replaceMap = new HashMap<>();
        Map<Symbol, Integer> localOffsets = locals.getRight();
        for (Block block : vFunc.getBlocks()) {
            mFunc.addIR(new LabelMIR(block.getLabel()));
            for (VIR vir : block) {
                if (vir instanceof BinaryVIR binaryVIR)
                    MIROpTrans.transBinary(mFunc.getIrs(), binaryVIR);
                if (vir instanceof BranchVIR branchVIR)
                    MIROpTrans.transBranch(mFunc.getIrs(), branchVIR);
                if (vir instanceof CallVIR callVIR) {
                    int paramNum = MIROpTrans.transCall(mFunc.getIrs(), callVIR);
                    mFunc.setMaxFuncParamNum(Integer.max(mFunc.getMaxFuncParamNum(), paramNum));
                }
                if (vir instanceof JumpVIR jumpVIR)
                    mFunc.getIrs().add(new BMIR(null, null, null, jumpVIR.target.getLabel()));
                if (vir instanceof LiVIR liVIR)
                    MIROpTrans.transLI(mFunc.getIrs(), liVIR);
                if (vir instanceof LoadVIR loadVIR)
                    MIROpTrans.transLoad(mFunc.getIrs(), loadVIR, localOffsets, paramOffsets);
                if (vir instanceof MovVIR movVIR)
                    MIROpTrans.transMov(mFunc.getIrs(), movVIR);
                if (vir instanceof RetVIR retVIR) {
                    if (retVIR.retVal instanceof VReg reg)
                        mFunc.getIrs().add(new RrMIR(RrMIR.Op.MV, retVIR.retVal.getType() == BasicType.I32 ? MReg.A0 : MReg.FA0, reg));
                    else if (retVIR.retVal instanceof InstantValue value) {
                        switch (value.getType()) {
                            case BasicType.I32 -> mFunc.getIrs().add(new LiMIR(MReg.A0, value.intValue()));
                            case BasicType.FLOAT ->
                                    mFunc.getIrs().add(new LiMIR(MReg.FA0, Float.floatToIntBits(value.floatValue())));
                            default -> throw new IllegalStateException("Unexpected value: " + value.getType());
                        }
                    }
                }
                if (vir instanceof UnaryVIR unaryVIR)
                    MIROpTrans.transUnary(mFunc.getIrs(), unaryVIR);
                if (vir instanceof StoreVIR storeVIR)
                    MIROpTrans.transStore(mFunc.getIrs(), storeVIR, localOffsets, paramOffsets);
            }
        }
        mFunc.getIrs().replaceAll(ir -> ir.replaceReg(replaceMap));
        return mFunc;
    }
}
