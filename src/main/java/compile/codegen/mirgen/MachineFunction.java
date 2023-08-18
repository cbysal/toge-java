package compile.codegen.mirgen;

import compile.codegen.mirgen.mir.MIR;
import compile.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public class MachineFunction {
    private final Symbol symbol;
    private final List<MIR> irs = new ArrayList<>();
    private final int localSize, iCallerNum, fCallerNum;
    private int maxFuncParamNum = 0;

    public MachineFunction(Symbol symbol, int localSize, int iCallerNum, int fCallerNum) {
        this.symbol = symbol;
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
        return symbol.getName();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void print() {
        System.out.println(symbol);
        System.out.println("-------- mir --------");
        irs.forEach(System.out::println);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(symbol).append('\n');
        builder.append("-------- mir --------\n");
        irs.forEach(ir -> builder.append(ir).append('\n'));
        return builder.toString();
    }
}
