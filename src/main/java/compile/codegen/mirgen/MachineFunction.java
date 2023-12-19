package compile.codegen.mirgen;

import compile.codegen.mirgen.mir.MIR;
import compile.llvm.Function;

import java.util.ArrayList;
import java.util.List;

public class MachineFunction {
    private final Function func;
    private final List<MIR> irs = new ArrayList<>();
    private final int localSize, iCallerNum, fCallerNum;
    private int maxFuncParamNum = 0;

    public MachineFunction(Function func, int localSize, int iCallerNum, int fCallerNum) {
        this.func = func;
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
        return func.getName();
    }

    public String getRawName() {
        return func.getRawName();
    }

    public void print() {
        System.out.println(func);
        System.out.println("-------- mir --------");
        irs.forEach(System.out::println);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(func).append('\n');
        builder.append("-------- mir --------\n");
        irs.forEach(ir -> builder.append(ir).append('\n'));
        return builder.toString();
    }
}
