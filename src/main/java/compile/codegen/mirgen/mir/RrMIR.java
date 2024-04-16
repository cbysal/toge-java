package compile.codegen.mirgen.mir;

import compile.codegen.MReg;
import compile.codegen.Reg;
import compile.codegen.VReg;
import compile.llvm.type.BasicType;

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
            VReg target = new VReg(reg.getType());
            VReg source = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new RrMIR(op, target, source);
            MIR ir3 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2, ir3);
        }
        if (dest.equals(reg)) {
            VReg target = new VReg(reg.getType());
            MIR ir1 = new RrMIR(op, target, src);
            MIR ir2 = new StoreItemMIR(StoreItemMIR.Item.SPILL, target, offset);
            return List.of(ir1, ir2);
        }
        if (src.equals(reg)) {
            VReg source = new VReg(reg.getType());
            MIR ir1 = new LoadItemMIR(LoadItemMIR.Item.SPILL, source, offset);
            MIR ir2 = new RrMIR(op, dest, source);
            return List.of(ir1, ir2);
        }
        return List.of(this);
    }

    @Override
    public String toString() {
        switch (op) {
            case CVT: {
                String s;
                if (dest.getType().equals(BasicType.FLOAT)) {
                    if (src.getType() != BasicType.I32)
                        throw new RuntimeException();
                    s = String.format("fcvt.s.w\t%s,%s", dest, src);
                } else if (dest.getType().equals(BasicType.I32)) {
                    if (src.getType() != BasicType.FLOAT)
                        throw new RuntimeException();
                    s = String.format("fcvt.w.s\t%s,%s,rtz", dest, src);
                } else {
                    throw new IllegalStateException("Unexpected value: " + dest.getType());
                }
                return s;
            }
            case FABS:
                return String.format("fabs.s\t%s,%s", dest, src);
            case NEG: {
                String s;
                if (dest.getType().equals(BasicType.FLOAT)) {
                    if (src.getType() != BasicType.FLOAT)
                        throw new RuntimeException();
                    s = String.format("fneg.s\t%s,%s", dest, src);
                } else if (dest.getType().equals(BasicType.I32)) {
                    if (src.getType() != BasicType.I32)
                        throw new RuntimeException();
                    s = String.format("negw\t%s,%s", dest, src);
                } else {
                    throw new IllegalStateException("Unexpected value: " + dest.getType());
                }
                return s;
            }
            case MV: {
                String s;
                if (dest.getType().equals(BasicType.FLOAT)) {
                    if (src.getType().equals(BasicType.FLOAT)) {
                        s = "fmv.s";
                    } else if (src.getType().equals(BasicType.I32)) {
                        s = "fmv.w.x";
                    } else {
                        throw new IllegalStateException("Unexpected value: " + src.getType());
                    }
                } else if (dest.getType().equals(BasicType.I32)) {
                    if (src.getType().equals(BasicType.FLOAT)) {
                        s = "fmv.x.w";
                    } else if (src.getType().equals(BasicType.I32)) {
                        s = "mv";
                    } else {
                        throw new IllegalStateException("Unexpected value: " + src.getType());
                    }
                } else {
                    throw new IllegalStateException("Unexpected value: " + dest.getType());
                }
                return String.format("%s\t%s,%s", s, dest, src);
            }
            case SEQZ:
            case SNEZ:
                return String.format("%s\t%s,%s", op.toString().toLowerCase(), dest, src);
            default:
                throw new IllegalArgumentException();
        }
    }

    public enum Op {
        CVT, FABS, MV, NEG, SEQZ, SNEZ
    }
}
