package compile.codegen.regalloc;

import compile.codegen.mirgen.MachineFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegAllocator {
    private boolean isProcessed = false;
    private final List<MachineFunction> funcs;

    public RegAllocator(Map<String, MachineFunction> funcs) {
        this.funcs = new ArrayList<>(funcs.values());
    }

    public void allocate() {
        checkIfIsProcessed();
    }

    private void checkIfIsProcessed() {
        if (isProcessed)
            return;
        isProcessed = true;
        funcs.forEach(func -> new RegAllocatorForSingleFunc(func).allocate());
    }
}
