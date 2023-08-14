package compile.codegen.virgen;

import compile.codegen.virgen.pass.*;
import compile.symbol.GlobalSymbol;

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
        new RemoveUnreachableBlocks(globals, funcs).run();
        new MemToReg(globals, funcs).run();
        new ConstructSSA(globals, funcs).run();
        boolean toContinue;
        toContinue = new ParamToReg(globals, funcs).run();
        toContinue |= new ConstructSSA(globals, funcs).run();
        do {
            toContinue = new RemoveUnreachableBlocks(globals, funcs).run();
            toContinue |= new MemToReg(globals, funcs).run();
            toContinue |= new ConstructSSA(globals, funcs).run();
            toContinue |= new SplitGlobals(globals, funcs).run();
            toContinue |= new SplitLocals(globals, funcs).run();
            toContinue |= new GlobalToImm(globals, funcs).run();
            toContinue |= new RemoveUselessIRs(globals, funcs).run();
            toContinue |= new ConstantPropagation(globals, funcs).run();
            toContinue |= new AssignmentPropagation(globals, funcs).run();
            toContinue |= new ConstantFolding(globals, funcs).run();
            toContinue |= new BlockFusion(globals, funcs).run();
            toContinue |= new DegradePhi(globals, funcs).run();
            toContinue |= new CombineInstructions(globals, funcs).run();
            toContinue |= new MatchPatterns(globals, funcs).run();
            toContinue |= new PeekHole(globals, funcs).run();
        } while (toContinue);
        new FunctionInline(globals, funcs).run();
        new ConstructSSA(globals, funcs).run();
        do {
            toContinue = new RemoveUnreachableBlocks(globals, funcs).run();
            toContinue |= new MemToReg(globals, funcs).run();
            toContinue |= new ConstructSSA(globals, funcs).run();
            toContinue |= new SplitGlobals(globals, funcs).run();
            toContinue |= new SplitLocals(globals, funcs).run();
            toContinue |= new GlobalToImm(globals, funcs).run();
            toContinue |= new RemoveUselessIRs(globals, funcs).run();
            toContinue |= new ConstantPropagation(globals, funcs).run();
            toContinue |= new AssignmentPropagation(globals, funcs).run();
            toContinue |= new ConstantFolding(globals, funcs).run();
            toContinue |= new BlockFusion(globals, funcs).run();
            toContinue |= new DegradePhi(globals, funcs).run();
            toContinue |= new CombineInstructions(globals, funcs).run();
            toContinue |= new MatchPatterns(globals, funcs).run();
            toContinue |= new PeekHole(globals, funcs).run();
        } while (toContinue);
    }
}
