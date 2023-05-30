package compile.codegen;

import compile.codegen.machine.DataItem;
import compile.llvm.ir.Function;
import compile.llvm.ir.Global;
import compile.llvm.ir.Module;
import compile.llvm.ir.type.PointerType;

import java.util.HashMap;
import java.util.Map;

public class CodeGenerator {
    private boolean isProcessed;
    private final Module module;
    private final Map<String, DataItem> dataItems = new HashMap<>();
    private final Map<String, compile.codegen.machine.Function> functions = new HashMap<>();

    public CodeGenerator(Module module) {
        this.module = module;
    }

    private void checkIfIsProcessed() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        globalToData();
        for (Function function : module.getFunctions().values()) {
            CodeGeneratorForFunction codeGeneratorForFunction = new CodeGeneratorForFunction(module, dataItems, function);
            compile.codegen.machine.Function asmFunction = codeGeneratorForFunction.process();
            functions.put(asmFunction.getName(), asmFunction);
        }
    }

    private void globalToData() {
        Map<String, Global> globals = module.getGlobals();
        for (Map.Entry<String, Global> entry : globals.entrySet()) {
            String name = entry.getKey();
            Global global = entry.getValue();
            DataItem dataItem = new DataItem(name, ((PointerType) global.getType()).base().getSize());
            Map<Integer, Integer> values = global.flatten();
            values.forEach(dataItem::set);
            dataItems.put(name, dataItem);
        }
    }

    public Map<String, DataItem> getDataItems() {
        checkIfIsProcessed();
        return dataItems;
    }

    public Map<String, compile.codegen.machine.Function> getFunctions() {
        checkIfIsProcessed();
        return functions;
    }
}
