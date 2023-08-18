package compile.codegen.mirgen.mir;

import compile.codegen.Reg;
import compile.codegen.mirgen.MReg;
import compile.codegen.virgen.VReg;
import compile.symbol.Type;

import java.util.List;
import java.util.Map;

public class RrMIR extends MIR {
    public final Op op;
    public final Reg dest, src;
    public RrMIR(Op op, Reg dest, Reg src) {
        this.op = op;
        this.dest = dest;
        this.src = src;
    }

    @Override
    public List<Reg> getRead() {
        return List.of(src);
    }

    @Override
    public List<Reg> getWrite() {
        return List.of(dest);
    }

    @Override
    public MIR replaceReg(Map<VReg, MReg> replaceMap) {
        Reg newDest = dest, newSrc = src;
        if (dest instanceof VReg && replaceMap.containsKey(dest))
            newDest = replaceMap.get(dest);
        if (src instanceof VReg && replaceMap.containsKey(src))
            newSrc = replaceMap.get(src);
        return new RrMIR(op, newDest, newSrc);
    }

    @Override
    public List<MIR> spill(Reg reg, int offset) {
        if (dest.equals(reg) && src.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            VReg source = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new RrMIR(op, target, source);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new RrMIR(op, target, src);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (src.equals(reg)) {
            VReg source = new VReg(reg.getType(), reg.getSize());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new RrMIR(op, dest, source);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        return switch (op) {
            case CVT -> switch (dest.getType()) {
                case FLOAT -> {
                    if (src.getType() != Type.INT)
                        throw new RuntimeException();
                    yield String.format("fcvt.s.w\t%s,%s", dest, src);
                }
                case INT -> {
                    if (src.getType() != Type.FLOAT)
                        throw new RuntimeException();
                    yield String.format("fcvt.w.s\t%s,%s,rtz", dest, src);
                }
                default -> throw new IllegalStateException("Unexpected value: " + dest.getType());
            };
            case FABS -> String.format("fabs.s\t%s,%s", dest, src);
            case NEG -> switch (dest.getType()) {
                case FLOAT -> {
                    if (src.getType() != Type.FLOAT)
                        throw new RuntimeException();
                    yield String.format("fneg.s\t%s,%s", dest, src);
                }
                case INT -> {
                    if (src.getType() != Type.INT)
                        throw new RuntimeException();
                    yield String.format("negw\t%s,%s", dest, src);
                }
                default -> throw new IllegalStateException("Unexpected value: " + dest.getType());
            };
            case MV -> String.format("%s\t%s,%s", switch (dest.getType()) {
                case FLOAT -> switch (src.getType()) {
                    case FLOAT -> "fmv.s";
                    case INT -> "fmv.w.x";
                    default -> throw new IllegalStateException("Unexpected value: " + src.getType());
                };
                case INT -> switch (src.getType()) {
                    case FLOAT -> "fmv.x.w";
                    case INT -> "mv";
                    default -> throw new IllegalStateException("Unexpected value: " + src.getType());
                };
                default -> throw new IllegalStateException("Unexpected value: " + dest.getType());
            }, dest, src);
        };
    }

    public enum Op {
        CVT, FABS, MV, NEG
    }
}
