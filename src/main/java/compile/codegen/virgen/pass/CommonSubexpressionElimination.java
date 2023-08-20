package compile.codegen.virgen.pass;

import compile.codegen.virgen.Block;
import compile.codegen.virgen.VReg;
import compile.codegen.virgen.VirtualFunction;
import compile.codegen.virgen.vir.*;
import compile.symbol.GlobalSymbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommonSubexpressionElimination extends Pass {
    private static int counter = 0;
    private record Item(Item.Op op, VIRItem source1, VIRItem source2) {
        public enum Op {
            ADD, SUB, MUL, DIV, MOD, EQ, NE, GE, GT, LE, LT, F2I, I2F, NEG, L_NOT, ABS
        }
    }

    public CommonSubexpressionElimination(Set<GlobalSymbol> globals, Map<String, VirtualFunction> funcs) {
        super(globals, funcs);
    }

    @Override
    public boolean run() {
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
                        }, binaryVIR.left, binaryVIR.right);
                        if (replaceMap.containsKey(item)) {
                            System.out.println(counter++);
                            block.set(i, new MovVIR(target, replaceMap.get(item)));
                            continue;
                        }
                        replaceMap.put(item, target);
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
                        }, unaryVIR.source, null);
                        if (replaceMap.containsKey(item)) {
                            System.out.println(counter++);
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
