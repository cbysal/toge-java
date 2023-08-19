package compile.codegen.virgen;

import compile.codegen.virgen.pass.*;
import compile.symbol.GlobalSymbol;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class VIRPassManager {
    private final Set<GlobalSymbol> globals;
    private final Map<String, VirtualFunction> funcs;

    public VIRPassManager(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        this.globals = globals;
        this.funcs = funcs;
    }

    public void run() {
        boolean toContinue;
        new ParamToReg(globals, funcs).run();
        do {
            do {
                toContinue = new RemoveUnreachableBlocks(globals, funcs).run();
                toContinue |= new MemToReg(globals, funcs).run();
                toContinue |= new RemoveUselessIRs(globals, funcs).run();
                toContinue |= new SplitGlobals(globals, funcs).run();
                toContinue |= new SplitLocals(globals, funcs).run();
                toContinue |= new WeakSplitRegs(globals, funcs).run();
                toContinue |= new GlobalToImm(globals, funcs).run();
                toContinue |= new RemoveUselessIRs(globals, funcs).run();
                toContinue |= new ConstantPropagation(globals, funcs).run();
                toContinue |= new AssignmentPropagation(globals, funcs).run();
                toContinue |= new ConstantFolding(globals, funcs).run();
                toContinue |= new BlockFusion(globals, funcs).run();
                toContinue |= new CombineInstructions(globals, funcs).run();
                toContinue |= new MatchPatterns(globals, funcs).run();
                toContinue |= new PeekHole(globals, funcs).run();
            } while (toContinue);
            toContinue = new StrongSplitRegs(globals, funcs).run();
        } while (toContinue);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("b3.vir"))) {
            for (VirtualFunction func : funcs.values()) {
                writer.write(func.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new FunctionInline(globals, funcs).run();
        new ParamToReg(globals, funcs).run();
        do {
            do {
                toContinue = new RemoveUnreachableBlocks(globals, funcs).run();
                toContinue |= new MemToReg(globals, funcs).run();
                toContinue |= new RemoveUselessIRs(globals, funcs).run();
                toContinue |= new SplitGlobals(globals, funcs).run();
                toContinue |= new SplitLocals(globals, funcs).run();
                toContinue |= new WeakSplitRegs(globals, funcs).run();
                toContinue |= new GlobalToImm(globals, funcs).run();
                toContinue |= new RemoveUselessIRs(globals, funcs).run();
                toContinue |= new ConstantPropagation(globals, funcs).run();
                toContinue |= new AssignmentPropagation(globals, funcs).run();
                toContinue |= new ConstantFolding(globals, funcs).run();
                toContinue |= new BlockFusion(globals, funcs).run();
                toContinue |= new CombineInstructions(globals, funcs).run();
                toContinue |= new MatchPatterns(globals, funcs).run();
                toContinue |= new PeekHole(globals, funcs).run();
                toContinue |= new RemoveUnreachableBlocks(globals, funcs).run();
                toContinue |= new FullLoopUnrolling(globals, funcs).run();
            } while (toContinue);
            toContinue = new StrongSplitRegs(globals, funcs).run();
        } while (toContinue);
    }
}
