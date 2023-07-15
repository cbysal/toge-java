package compile.codegen.mirgen;

import compile.codegen.mirgen.mir.LabelMIR;
import compile.codegen.mirgen.mir.MIR;
import compile.codegen.mirgen.trans.MIROpTrans;
import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MIRGenerator {
    private boolean isProcessed = false;
    private final Map<String, GlobalSymbol> consts;
    private final Map<String, GlobalSymbol> globals;
    private final Map<String, VirtualFunction> vFuncs;
    private final Map<String, MachineFunction> mFuncs = new HashMap<>();

    public MIRGenerator(Map<String, GlobalSymbol> consts, Map<String, GlobalSymbol> globals, Map<String,
            VirtualFunction> vFuncs) {
        this.consts = consts;
        this.globals = globals;
        this.vFuncs = vFuncs;
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        vir2Mir();
    }

    private Map.Entry<Integer, Integer> getCallerNumbers(FuncSymbol func) {
        int iSize = 0, fSize = 0;
        for (ParamSymbol param : func.getParams())
            if (!param.isSingle() || param.getType() == Type.INT)
                iSize = Integer.min(iSize + 1, MReg.I_CALLER_REGS.size());
            else
                fSize = Integer.min(fSize + 1, MReg.F_CALLER_REGS.size());
        return Map.entry(iSize, fSize);
    }

    public Map<String, GlobalSymbol> getConsts() {
        checkIfIsProcessed();
        return consts;
    }

    public Map<String, MachineFunction> getFuncs() {
        checkIfIsProcessed();
        return mFuncs;
    }

    public Map<String, GlobalSymbol> getGlobals() {
        checkIfIsProcessed();
        return globals;
    }

    private Map.Entry<Integer, Map<Symbol, Integer>> calcLocalOffsets(List<LocalSymbol> locals) {
        int localSize = 0;
        Map<Symbol, Integer> localOffsets = new HashMap<>();
        for (LocalSymbol localSymbol : locals) {
            int size = localSymbol.size();
            localOffsets.put(localSymbol, localSize);
            localSize += size;
        }
        return Map.entry(localSize, localOffsets);
    }

    private Map<Symbol, Map.Entry<Boolean, Integer>> calcParamOffsets(List<ParamSymbol> params) {
        Map<Symbol, Map.Entry<Boolean, Integer>> paramOffsets = new HashMap<>();
        int fCallerNum = 0;
        for (ParamSymbol param : params)
            if (param.isSingle() && param.getType() == Type.FLOAT)
                fCallerNum++;
        fCallerNum = Integer.min(fCallerNum, MReg.F_CALLER_REGS.size());
        int iSize = 0, fSize = 0;
        for (ParamSymbol param : params) {
            if (!param.isSingle() || param.getType() == Type.INT) {
                if (iSize < MReg.I_CALLER_REGS.size())
                    paramOffsets.put(param, Map.entry(true, (iSize + fCallerNum) * 8));
                else
                    paramOffsets.put(param, Map.entry(false,
                            (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
                iSize++;
            } else {
                if (fSize < MReg.F_CALLER_REGS.size())
                    paramOffsets.put(param, Map.entry(true, fSize * 8));
                else
                    paramOffsets.put(param, Map.entry(false,
                            (Integer.max(iSize - MReg.I_CALLER_REGS.size(), 0) + Integer.max(fSize - MReg.F_CALLER_REGS.size(), 0)) * 8));
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
        Map<Symbol, Map.Entry<Boolean, Integer>> paramOffsets = calcParamOffsets(vFunc.getSymbol().getParams());
        Map.Entry<Integer, Map<Symbol, Integer>> locals = calcLocalOffsets(vFunc.getLocals());
        Map.Entry<Integer, Integer> callerNums = getCallerNumbers(vFunc.getSymbol());
        MachineFunction mFunc = new MachineFunction(vFunc.getSymbol(), locals.getKey(), callerNums.getKey(),
                callerNums.getValue());
        Map<VReg, MReg> replaceMap = new HashMap<>();
        if (vFunc.getRetVal() != null) {
            if (vFunc.getRetVal().getType() == Type.FLOAT)
                replaceMap.put(vFunc.getRetVal(), MReg.FA0);
            else
                replaceMap.put(vFunc.getRetVal(), MReg.A0);
        }
        Map<Symbol, Integer> localOffsets = locals.getValue();
        for (Block block : vFunc.getBlocks()) {
            mFunc.addIR(new LabelMIR(block.getLabel()));
            for (VIR vir : block) {
                if (vir instanceof BinaryVIR binaryVIR)
                    MIROpTrans.transBinary(mFunc.getIrs(), binaryVIR);
                if (vir instanceof CallVIR callVIR) {
                    int paramNum = MIROpTrans.transCall(mFunc.getIrs(), callVIR);
                    mFunc.setMaxFuncParamNum(Integer.max(mFunc.getMaxFuncParamNum(), paramNum));
                }
                if (vir instanceof LIVIR liVIR)
                    MIROpTrans.transLI(mFunc.getIrs(), liVIR);
                if (vir instanceof LoadVIR loadVIR)
                    MIROpTrans.transLoad(mFunc.getIrs(), loadVIR, localOffsets, paramOffsets);
                if (vir instanceof MovVIR movVIR)
                    MIROpTrans.transMov(mFunc.getIrs(), movVIR);
                if (vir instanceof UnaryVIR unaryVIR)
                    MIROpTrans.transUnary(mFunc.getIrs(), unaryVIR);
                if (vir instanceof StoreVIR storeVIR)
                    MIROpTrans.transStore(mFunc.getIrs(), storeVIR, localOffsets, paramOffsets);
            }
            MIROpTrans.transBlockBranches(mFunc.getIrs(), block);
        }
        for (MIR mir : mFunc.getIrs())
            mir.replaceReg(replaceMap);
        return mFunc;
    }
}
