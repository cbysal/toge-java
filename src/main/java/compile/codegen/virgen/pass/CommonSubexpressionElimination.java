package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.FuncSymbol;
import compile.symbol.GlobalSymbol;

import java.util.*;

public class CommonSubexpressionElimination extends Pass {
    private record Item(Item.Op op, FuncSymbol func, List<VIRItem> sources) {
        public enum Op {
            ADD, SUB, MUL, DIV, MOD, EQ, NE, GE, GT, LE, LT, F2I, I2F, NEG, L_NOT, ABS, CALL
        }
    }

    public CommonSubexpressionElimination(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
        Set<FuncSymbol> pureFuncs = new HashSet<>();
        for (VirtualFunction func : funcs.values())
            pureFuncs.add(func.getSymbol());
        for (VirtualFunction func : funcs.values()) {
            boolean isPure = true;
            for (Block block : func.getBlocks()) {
                for (VIR ir : block) {
                    if (ir instanceof CallVIR callVIR) {
                        if (callVIR.func.getName().equals("memset"))
                            continue;
                        if (!pureFuncs.contains(callVIR.func)) {
                            isPure = false;
                            break;
                        }
                        continue;
                    }
                    if (ir instanceof LoadVIR loadVIR) {
                        if (loadVIR.symbol instanceof GlobalSymbol) {
                            isPure = false;
                            break;
                        }
                        continue;
                    }
                    if (ir instanceof StoreVIR storeVIR) {
                        if (storeVIR.symbol instanceof GlobalSymbol) {
                            isPure = false;
                            break;
                        }
                        continue;
                    }
                }
                if (!isPure)
                    break;
            }
            if (!isPure)
                pureFuncs.remove(func.getSymbol());
        }
        for (VirtualFunction func : funcs.values()) {
            for (Block block : func.getBlocks()) {
                Map<Item, VReg> replaceMap = new HashMap<>();
                for (int i = 0; i < block.size(); i++) {
                    VIR ir = block.get(i);
                    if (ir instanceof BinaryVIR binaryVIR) {
                        VReg target = binaryVIR.target;
                        Item item = new Item(switch (binaryVIR.type) {
                            case ADD -> Item.Op.ADD;
                            case SUB -> Item.Op.SUB;
                            case MUL -> Item.Op.MUL;
                            case DIV -> Item.Op.DIV;
                            case MOD -> Item.Op.MOD;
                            case EQ -> Item.Op.EQ;
                            case NE -> Item.Op.NE;
                            case GE -> Item.Op.GE;
                            case GT -> Item.Op.GT;
                            case LE -> Item.Op.LE;
                            case LT -> Item.Op.LT;
                        }, null, List.of(binaryVIR.left, binaryVIR.right));
                        if (replaceMap.containsKey(item)) {
                            block.set(i, new MovVIR(target, replaceMap.get(item)));
                            continue;
                        }
                        replaceMap.put(item, target);
                        continue;
                    }
                    if (ir instanceof CallVIR callVIR) {
                        if (pureFuncs.contains(callVIR.func) && callVIR.target != null) {
                            VReg target = callVIR.target;
                            Item item = new Item(Item.Op.CALL, callVIR.func, callVIR.params);
                            if (replaceMap.containsKey(item)) {
                                block.set(i, new MovVIR(target, replaceMap.get(item)));
                                continue;
                            }
                            replaceMap.put(item, target);
                            continue;
                        }
                        continue;
                    }
                    if (ir instanceof UnaryVIR unaryVIR) {
                        VReg target = unaryVIR.target;
                        Item item = new Item(switch (unaryVIR.type) {
                            case F2I -> Item.Op.F2I;
                            case I2F -> Item.Op.I2F;
                            case NEG -> Item.Op.NEG;
                            case L_NOT -> Item.Op.L_NOT;
                            case ABS -> Item.Op.ABS;
                        }, null, List.of(unaryVIR.source));
                        if (replaceMap.containsKey(item)) {
                            block.set(i, new MovVIR(target, replaceMap.get(item)));
                            continue;
                        }
                        replaceMap.put(item, target);
                        continue;
                    }
                }
            }
        }
        return false;
    }
}
