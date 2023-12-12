package compile.codegen.mirgen;

import compile.codegen.mirgen.mir.MIR;
import compile.vir.VirtualFunction;

import java.util.ArrayList;
import java.util.List;

public class MachineFunction {
    private final VirtualFunction vFunc;
    private final List<MIR> irs = new ArrayList<>();
    private final int localSize, iCallerNum, fCallerNum;
    private int maxFuncParamNum = 0;

    public MachineFunction(VirtualFunction vFunc, int localSize, int iCallerNum, int fCallerNum) {
        this.vFunc = vFunc;
        this.localSize = localSize;
        this.iCallerNum = iCallerNum;
        this.fCallerNum = fCallerNum;
    }

    public void addIR(MIR ir) {
        this.irs.add(ir);
    }

    public int getFCallerNum() {
        return fCallerNum;
    }

    public int getICallerNum() {
        return iCallerNum;
    }

    public List<MIR> getIrs() {
        return irs;
    }

    public int getLocalSize() {
        return localSize;
    }

    public int getMaxFuncParamNum() {
        return maxFuncParamNum;
    }

    public void setMaxFuncParamNum(int maxFuncParamNum) {
        this.maxFuncParamNum = maxFuncParamNum;
    }

    public String getName() {
        return vFunc.getName();
    }

    public void print() {
        System.out.println(vFunc);
        System.out.println("-------- mir --------");
        irs.forEach(System.out::println);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(vFunc).append('\n');
        builder.append("-------- mir --------\n");
        irs.forEach(ir -> builder.append(ir).append('\n'));
        return builder.toString();
    }
}
