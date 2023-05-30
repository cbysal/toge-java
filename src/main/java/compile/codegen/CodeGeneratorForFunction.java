package compile.codegen;

import common.Pair;
import compile.codegen.machine.Block;
import compile.codegen.machine.DataItem;
import compile.codegen.machine.asm.*;
import compile.codegen.machine.asm.virtual.*;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;
import compile.llvm.ir.*;
import compile.llvm.ir.Module;
import compile.llvm.ir.constant.FloatConstant;
import compile.llvm.ir.constant.I32Constant;
import compile.llvm.ir.instr.*;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.PointerType;

import java.util.*;

public class CodeGeneratorForFunction {
    private boolean isProcessed;
    private final Module module;
    private final Map<String, DataItem> dataItems;
    private final Function llvmFunction;
    private final Map<Integer, Integer> allocSizes = new HashMap<>();
    private final compile.codegen.machine.Function asmFunction;
    private int iCalleeNum, fCalleeNum, spillNum = 0;

    public CodeGeneratorForFunction(Module module, Map<String, DataItem> dataItems, Function llvmFunction) {
        this.module = module;
        this.dataItems = dataItems;
        this.llvmFunction = llvmFunction;
        this.asmFunction = new compile.codegen.machine.Function(llvmFunction.getName());
    }

    public compile.codegen.machine.Function process() {
        if (isProcessed) {
            return asmFunction;
        }
        isProcessed = true;
        llvmToVirtualAsm();
        virtualToMachine();
        return asmFunction;
    }

    private void llvmToVirtualAsm() {
        Map<Value, Integer> allocIds = new HashMap<>();
        Map<Value, Reg> valueToReg = new HashMap<>();
        List<Param> params = llvmFunction.getParams();
        int iParamSize = 0, fParamSize = 0, spillParamSize = 0;
        List<VStoreParamAsm> storeSpilledParams = new ArrayList<>();
        for (Param param : params) {
            if (param.getType() == BasicType.FLOAT) {
                if (fParamSize < MReg.F_CALLER_REGS.size()) {
                    valueToReg.put(param, MReg.F_CALLER_REGS.get(fParamSize));
                    fParamSize++;
                } else {
                    int allocId = allocIds.size();
                    allocIds.put(param, allocId);
                    allocSizes.put(allocId, 8);
                    storeSpilledParams.add(new VStoreParamAsm(spillParamSize, allocId));
                    spillParamSize++;
                }
            } else {
                if (iParamSize < MReg.CALLER_REGS.size()) {
                    valueToReg.put(param, MReg.CALLER_REGS.get(iParamSize));
                    iParamSize++;
                } else {
                    int allocId = allocIds.size();
                    allocIds.put(param, allocId);
                    allocSizes.put(allocId, 8);
                    storeSpilledParams.add(new VStoreParamAsm(spillParamSize, allocId));
                    spillParamSize++;
                }
            }
        }
        parseAllocInstrs(allocIds);
        int maxParamNum = 0;
        for (BasicBlock block = llvmFunction.getFirst(); block != null; block = block.getNext()) {
            for (Instr instr = block.getFirst(); instr != null; instr = instr.getNext()) {
                if (instr instanceof CallInstr callInstr) {
                    maxParamNum = Integer.max(maxParamNum, callInstr.getParams().size());
                }
            }
        }
        Map<BasicBlock, Block> llvmToAsmBlockMap = new HashMap<>();
        for (BasicBlock block = llvmFunction.getFirst(); block != null; block = block.getNext()) {
            Block asmBlock = new Block();
            asmFunction.addBlock(asmBlock);
            llvmToAsmBlockMap.put(block, asmBlock);
        }
        for (BasicBlock block = llvmFunction.getFirst(); block != null; block = block.getNext()) {
            Block asmBlock = llvmToAsmBlockMap.get(block);
            for (Instr instr = block.getFirst(); instr != null; instr = instr.getNext()) {
                if (instr instanceof AllocInstr) {
                    continue;
                }
                if (instr instanceof LoadInstr loadInstr) {
                    parseLoad(allocIds, valueToReg, asmBlock, loadInstr);
                    continue;
                }
                if (instr instanceof StoreInstr storeInstr) {
                    parseStore(allocIds, valueToReg, asmBlock, storeInstr);
                    continue;
                }
                if (instr instanceof GetElementPtrInstr getElementPtrInstr) {
                    parseGetElementPtr(allocIds, valueToReg, asmBlock, getElementPtrInstr);
                    continue;
                }
                if (instr instanceof BinaryInstr binaryInstr) {
                    parseBinary(valueToReg, asmBlock, binaryInstr);
                    continue;
                }
                if (instr instanceof CmpInstr cmpInstr) {
                    parseCmp(valueToReg, asmBlock, cmpInstr);
                    continue;
                }
                if (instr instanceof BranchInstr branchInstr) {
                    parseBranch(valueToReg, llvmToAsmBlockMap, asmBlock, branchInstr);
                    continue;
                }
                if (instr instanceof FnegInstr fnegInstr) {
                    parseFneg(valueToReg, asmBlock, fnegInstr);
                    continue;
                }
                if (instr instanceof ZextInstr zextInstr) {
                    parseZext(valueToReg, asmBlock, zextInstr);
                    continue;
                }
                if (instr instanceof SitofpInstr sitofpInstr) {
                    parseSitofp(valueToReg, asmBlock, sitofpInstr);
                    continue;
                }
                if (instr instanceof FptosiInstr fptosiInstr) {
                    parseFptosi(valueToReg, asmBlock, fptosiInstr);
                    continue;
                }
                if (instr instanceof CallInstr callInstr) {
                    parseCall(valueToReg, asmBlock, callInstr);
                    continue;
                }
                if (instr instanceof RetInstr retInstr) {
                    parseRet(valueToReg, asmBlock, retInstr);
                    continue;
                }
                if (instr instanceof BitCastInstr bitCastInstr) {
                    parseBitCast(valueToReg, asmBlock, bitCastInstr);
                    continue;
                }
                throw new RuntimeException("Unhandled instr: " + instr);
            }
        }
        asmFunction.getBlocks().get(0).getAsms().addAll(0, storeSpilledParams);
    }

    private void parseAllocInstrs(Map<Value, Integer> allocIds) {
        for (BasicBlock block = llvmFunction.getFirst(); block != null; block = block.getNext()) {
            for (Instr instr = block.getFirst(); instr != null; instr = instr.getNext()) {
                if (instr instanceof AllocInstr allocInstr) {
                    int allocId = allocIds.size();
                    int allocSize = ((PointerType) allocInstr.getType()).base().getSize();
                    allocIds.put(allocInstr, allocId);
                    allocSizes.put(allocId, allocSize);
                    continue;
                }
                break;
            }
        }
    }

    private void parseLoad(Map<Value, Integer> allocIds, Map<Value, Reg> valueToReg, Block asmBlock, LoadInstr loadInstr) {
        Value src = loadInstr.getSrc();
        if (loadInstr.getType() == BasicType.FLOAT) {
            Reg destReg = new VReg(true);
            if (src instanceof Global global) {
                Reg srcReg = new VReg(false);
                DataItem dataItem = dataItems.get(global.getName());
                asmBlock.addAsm(new LlaAsm(srcReg, dataItem));
                asmBlock.addAsm(new LoadAsm(destReg, srcReg, 0, 4));
            } else if (allocIds.containsKey(src)) {
                int allocId = allocIds.get(src);
                asmBlock.addAsm(new VLoadLocalAsm(destReg, allocId));
            } else {
                Reg srcReg = valueToReg.get(src);
                asmBlock.addAsm(new LoadAsm(destReg, srcReg, 0, 4));
            }
            valueToReg.put(loadInstr, destReg);
        } else {
            Reg destReg = new VReg(false);
            if (src instanceof Global global) {
                asmBlock.addAsm(new VLoadGlobalAsm(destReg, global.getName()));
            } else if (allocIds.containsKey(src)) {
                int allocId = allocIds.get(src);
                asmBlock.addAsm(new VLoadLocalAsm(destReg, allocId));
            } else {
                Reg srcReg = valueToReg.get(src);
                asmBlock.addAsm(new LoadAsm(destReg, srcReg, 0, 4));
            }
            valueToReg.put(loadInstr, destReg);
        }
    }

    private void parseStore(Map<Value, Integer> allocIds, Map<Value, Reg> valueToReg, Block asmBlock, StoreInstr storeInstr) {
        Value src = storeInstr.getSrc();
        if (src.getType() == BasicType.FLOAT) {
            Reg srcReg;
            if (src instanceof FloatConstant floatConstant) {
                srcReg = new VReg(true);
                asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                asmBlock.addAsm(new MvAsm(srcReg, MReg.T0));
            } else if (allocIds.containsKey(src)) {
                int allocId = allocIds.get(src);
                srcReg = new VReg(true);
                asmBlock.addAsm(new VLoadLocalAsm(srcReg, allocId));
            } else {
                srcReg = valueToReg.get(src);
            }
            Value dst = storeInstr.getDst();
            if (dst instanceof Global global) {
                Reg destReg = new VReg(false);
                asmBlock.addAsm(new LlaAsm(destReg, dataItems.get(global.getName())));
                asmBlock.addAsm(new StoreAsm(srcReg, destReg, 0, 4));
            } else if (allocIds.containsKey(dst)) {
                int allocId = allocIds.get(dst);
                asmBlock.addAsm(new VStoreLocalAsm(srcReg, allocId));
            } else {
                Reg destReg = valueToReg.get(dst);
                asmBlock.addAsm(new StoreAsm(srcReg, destReg, 0, 4));
            }
        } else {
            Reg srcReg;
            if (src instanceof I32Constant i32Constant) {
                srcReg = new VReg(false);
                asmBlock.addAsm(new LiAsm(srcReg, i32Constant.getValue()));
            } else if (allocIds.containsKey(src)) {
                int allocId = allocIds.get(src);
                srcReg = new VReg(false);
                asmBlock.addAsm(new VLoadLocalAsm(srcReg, allocId));
            } else {
                srcReg = valueToReg.get(src);
            }
            Value dst = storeInstr.getDst();
            if (dst instanceof Global global) {
                Reg destReg = new VReg(false);
                asmBlock.addAsm(new LlaAsm(destReg, dataItems.get(global.getName())));
                asmBlock.addAsm(new StoreAsm(srcReg, destReg, 0, 4));
            } else if (allocIds.containsKey(dst)) {
                int allocId = allocIds.get(dst);
                asmBlock.addAsm(new VStoreLocalAsm(srcReg, allocId));
            } else {
                Reg destReg = valueToReg.get(dst);
                asmBlock.addAsm(new StoreAsm(srcReg, destReg, 0, 4));
            }
        }
    }

    private void parseGetElementPtr(Map<Value, Integer> allocIds, Map<Value, Reg> valueToReg, Block asmBlock, GetElementPtrInstr getElementPtrInstr) {
        Value base = getElementPtrInstr.getBase();
        Value index = getElementPtrInstr.getIndex();
        if (index != null) {
            Reg src;
            if (base instanceof Global global) {
                src = new VReg(false);
                asmBlock.addAsm(new LlaAsm(src, dataItems.get(global.getName())));
            } else if (allocIds.containsKey(base)) {
                int allocId = allocIds.get(base);
                src = new VReg(false);
                asmBlock.addAsm(new VAddLocalAsm(src, MReg.SP, allocId));
            } else {
                src = valueToReg.get(base);
            }
            int size = ((ArrayType) ((PointerType) base.getType()).base()).base().getSize();
            Reg offset = new VReg(false);
            if (index instanceof I32Constant i32Constant) {
                asmBlock.addAsm(new LiAsm(offset, i32Constant.getValue() * size));
            } else {
                Reg indexReg = valueToReg.get(index);
                Reg sizeReg = new VReg(false);
                asmBlock.addAsm(new LiAsm(sizeReg, size));
                asmBlock.addAsm(new RrrAsm(RrrAsm.Op.MULW, offset, indexReg, sizeReg));
            }
            Reg dest = new VReg(false);
            asmBlock.addAsm(new RrrAsm(RrrAsm.Op.ADD, dest, src, offset));
            valueToReg.put(getElementPtrInstr, dest);
        } else {
            index = getElementPtrInstr.getOffset();
            Reg src;
            if (base instanceof Global global) {
                src = new VReg(false);
                asmBlock.addAsm(new LlaAsm(src, dataItems.get(global.getName())));
            } else if (allocIds.containsKey(base)) {
                int allocId = allocIds.get(base);
                src = new VReg(false);
                asmBlock.addAsm(new VAddLocalAsm(src, MReg.SP, allocId));
            } else {
                src = valueToReg.get(base);
            }
            int size = ((PointerType) base.getType()).base().getSize();
            Reg offset = new VReg(false);
            if (index instanceof I32Constant i32Constant) {
                asmBlock.addAsm(new LiAsm(offset, i32Constant.getValue() * size));
            } else {
                Reg indexReg = valueToReg.get(index);
                Reg sizeReg = new VReg(false);
                asmBlock.addAsm(new LiAsm(sizeReg, size));
                asmBlock.addAsm(new RrrAsm(RrrAsm.Op.MULW, offset, indexReg, sizeReg));
            }
            Reg dest = new VReg(false);
            asmBlock.addAsm(new RrrAsm(RrrAsm.Op.ADD, dest, src, offset));
            valueToReg.put(getElementPtrInstr, dest);
        }
    }

    private static void parseBinary(Map<Value, Reg> valueToReg, Block asmBlock, BinaryInstr binaryInstr) {
        if (binaryInstr.getType() == BasicType.FLOAT) {
            Reg dest = new VReg(true);
            Reg src1, src2;
            if (binaryInstr.getlVal() instanceof I32Constant i32Constant) {
                src1 = new VReg(false);
                asmBlock.addAsm(new LiAsm(src1, i32Constant.getValue()));
            } else if (binaryInstr.getlVal() instanceof FloatConstant floatConstant) {
                src1 = new VReg(true);
                asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                asmBlock.addAsm(new MvAsm(src1, MReg.T0));
            } else {
                src1 = valueToReg.get(binaryInstr.getlVal());
            }
            if (binaryInstr.getrVal() instanceof I32Constant i32Constant) {
                src2 = new VReg(false);
                asmBlock.addAsm(new LiAsm(src2, i32Constant.getValue()));
            } else if (binaryInstr.getrVal() instanceof FloatConstant floatConstant) {
                src2 = new VReg(true);
                asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                asmBlock.addAsm(new MvAsm(src2, MReg.T0));
            } else {
                src2 = valueToReg.get(binaryInstr.getrVal());
            }
            if (binaryInstr.getOp() == BinaryInstr.Op.XOR) {
                asmBlock.addAsm(new RrrAsm(RrrAsm.Op.SLTU, dest, src1, MReg.ZERO));
            } else {
                asmBlock.addAsm(new RrrAsm(switch (binaryInstr.getOp()) {
                    case ADD -> RrrAsm.Op.ADD;
                    case SUB -> RrrAsm.Op.SUB;
                    case MUL -> RrrAsm.Op.MULW;
                    case SDIV -> RrrAsm.Op.DIV;
                    case SREM -> RrrAsm.Op.REMW;
                    case FADD -> RrrAsm.Op.FADD;
                    case FSUB -> RrrAsm.Op.FSUB;
                    case FMUL -> RrrAsm.Op.FMUL;
                    case FDIV -> RrrAsm.Op.FDIV;
                    default -> throw new IllegalStateException("Unexpected value: " + binaryInstr.getOp());
                }, dest, src1, src2));
            }
            valueToReg.put(binaryInstr, dest);
        } else {
            Reg dest = new VReg(false);
            Reg src1, src2;
            if (binaryInstr.getlVal() instanceof I32Constant i32Constant) {
                src1 = new VReg(false);
                asmBlock.addAsm(new LiAsm(src1, i32Constant.getValue()));
            } else if (binaryInstr.getlVal() instanceof FloatConstant floatConstant) {
                src1 = new VReg(true);
                asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                asmBlock.addAsm(new MvAsm(src1, MReg.T0));
            } else {
                src1 = valueToReg.get(binaryInstr.getlVal());
            }
            if (binaryInstr.getrVal() instanceof I32Constant i32Constant) {
                src2 = new VReg(false);
                asmBlock.addAsm(new LiAsm(src2, i32Constant.getValue()));
            } else if (binaryInstr.getrVal() instanceof FloatConstant floatConstant) {
                src2 = new VReg(true);
                asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                asmBlock.addAsm(new MvAsm(src2, MReg.T0));
            } else {
                src2 = valueToReg.get(binaryInstr.getrVal());
            }
            if (binaryInstr.getOp() == BinaryInstr.Op.XOR) {
                asmBlock.addAsm(new RrrAsm(RrrAsm.Op.SLTU, dest, src1, MReg.ZERO));
            } else {
                asmBlock.addAsm(new RrrAsm(switch (binaryInstr.getOp()) {
                    case ADD -> RrrAsm.Op.ADD;
                    case SUB -> RrrAsm.Op.SUB;
                    case MUL -> RrrAsm.Op.MULW;
                    case SDIV -> RrrAsm.Op.DIV;
                    case SREM -> RrrAsm.Op.REMW;
                    case FADD -> RrrAsm.Op.FADD;
                    case FSUB -> RrrAsm.Op.FSUB;
                    case FMUL -> RrrAsm.Op.FMUL;
                    case FDIV -> RrrAsm.Op.FDIV;
                    default -> throw new IllegalStateException("Unexpected value: " + binaryInstr.getOp());
                }, dest, src1, src2));
            }
            valueToReg.put(binaryInstr, dest);
        }
    }

    private static void parseCmp(Map<Value, Reg> valueToReg, Block asmBlock, CmpInstr cmpInstr) {
        CmpInstr.Op op = cmpInstr.getOp();
        Value lVal = cmpInstr.getlVal();
        Value rVal = cmpInstr.getrVal();
        Reg src1, src2;
        Reg dest = new VReg(false);
        if (lVal instanceof I32Constant i32Constant) {
            src1 = new VReg(false);
            asmBlock.addAsm(new LiAsm(src1, i32Constant.getValue()));
        } else if (lVal instanceof FloatConstant floatConstant) {
            src1 = new VReg(true);
            asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
            asmBlock.addAsm(new MvAsm(src1, MReg.T0));
        } else {
            src1 = valueToReg.get(lVal);
        }
        if (rVal instanceof I32Constant i32Constant) {
            src2 = new VReg(false);
            asmBlock.addAsm(new LiAsm(src2, i32Constant.getValue()));
        } else if (rVal instanceof FloatConstant floatConstant) {
            src2 = new VReg(true);
            asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
            asmBlock.addAsm(new MvAsm(src2, MReg.T0));
        } else {
            src2 = valueToReg.get(rVal);
        }
        if (cmpInstr instanceof ICmpInstr) {
            switch (op) {
                case EQ -> {
                    Reg tempReg = new VReg(false);
                    asmBlock.addAsm(new RrrAsm(RrrAsm.Op.SUB, tempReg, src1, src2));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.SLTIU, dest, tempReg, 1));
                }
                case NE -> {
                    Reg tempReg1 = new VReg(false);
                    Reg tempReg2 = new VReg(false);
                    asmBlock.addAsm(new RrrAsm(RrrAsm.Op.SUB, tempReg1, src1, src2));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.SLTIU, tempReg2, tempReg1, 1));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.SLTIU, dest, tempReg2, 1));
                }
                case GE -> {
                    Reg tempReg = new VReg(false);
                    asmBlock.addAsm(new RrrAsm(RrrAsm.Op.SUB, tempReg, src2, src1));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.SLTI, dest, tempReg, 1));
                }
                case GT -> {
                    Reg tempReg = new VReg(false);
                    asmBlock.addAsm(new RrrAsm(RrrAsm.Op.SUB, tempReg, src2, src1));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.SLTI, dest, tempReg, 0));
                }
                case LE -> {
                    Reg tempReg = new VReg(false);
                    asmBlock.addAsm(new RrrAsm(RrrAsm.Op.SUB, tempReg, src1, src2));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.SLTI, dest, tempReg, 1));
                }
                case LT -> asmBlock.addAsm(new RrrAsm(RrrAsm.Op.SLT, dest, src1, src2));
            }
        } else if (cmpInstr instanceof FCmpInstr) {
            switch (op) {
                case EQ -> asmBlock.addAsm(new RrrAsm(RrrAsm.Op.FEQ, dest, src1, src2));
                case NE -> {
                    Reg tempReg = new VReg(false);
                    asmBlock.addAsm(new RrrAsm(RrrAsm.Op.FEQ, tempReg, src1, src2));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.XORI, dest, tempReg, 1));
                }
                case GE -> {
                    Reg tempReg = new VReg(false);
                    asmBlock.addAsm(new RrrAsm(RrrAsm.Op.FLT, tempReg, src1, src2));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.XORI, dest, tempReg, 1));
                }
                case GT -> {
                    Reg tempReg = new VReg(false);
                    asmBlock.addAsm(new RrrAsm(RrrAsm.Op.FLE, tempReg, src1, src2));
                    asmBlock.addAsm(new RriAsm(RriAsm.Op.XORI, dest, tempReg, 1));
                }
                case LE -> asmBlock.addAsm(new RrrAsm(RrrAsm.Op.FLE, dest, src1, src2));
                case LT -> asmBlock.addAsm(new RrrAsm(RrrAsm.Op.FLT, dest, src1, src2));
            }
        } else {
            throw new RuntimeException();
        }
        valueToReg.put(cmpInstr, dest);
    }

    private static void parseBranch(Map<Value, Reg> valueToReg, Map<BasicBlock, Block> llvmToAsmBlockMap, Block asmBlock, BranchInstr branchInstr) {
        if (branchInstr.getCond() == null) {
            asmBlock.addAsm(new JAsm(llvmToAsmBlockMap.get(branchInstr.getTrueBlock())));
        } else {
            Reg cond = valueToReg.get(branchInstr.getCond());
            asmBlock.addAsm(new BAsm(BAsm.Op.NE, cond, MReg.ZERO, llvmToAsmBlockMap.get(branchInstr.getTrueBlock())));
            asmBlock.addAsm(new JAsm(llvmToAsmBlockMap.get(branchInstr.getFalseBlock())));
        }
    }

    private static void parseFneg(Map<Value, Reg> valueToReg, Block asmBlock, FnegInstr fnegInstr) {
        Reg src;
        if (fnegInstr.getBase() instanceof FloatConstant floatConstant) {
            src = new VReg(true);
            asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
            asmBlock.addAsm(new MvAsm(src, MReg.T0));
        } else {
            src = valueToReg.get(fnegInstr.getBase());
        }
        Reg dest = new VReg(true);
        asmBlock.addAsm(new FnegASM(dest, src));
        valueToReg.put(fnegInstr, dest);
    }

    private static void parseZext(Map<Value, Reg> valueToReg, Block asmBlock, ZextInstr zextInstr) {
        Reg src = valueToReg.get(zextInstr.getValue());
        Reg dest = new VReg(false);
        asmBlock.addAsm(new MvAsm(dest, src));
        valueToReg.put(zextInstr, dest);
    }

    private static void parseSitofp(Map<Value, Reg> valueToReg, Block asmBlock, SitofpInstr sitofpInstr) {
        Reg src;
        if (sitofpInstr.getBase() instanceof I32Constant i32Constant) {
            src = new VReg(false);
            asmBlock.addAsm(new LiAsm(src, i32Constant.getValue()));
        } else {
            src = valueToReg.get(sitofpInstr.getBase());
        }
        Reg dest = new VReg(true);
        asmBlock.addAsm(new CvtAsm(dest, src));
        valueToReg.put(sitofpInstr, dest);
    }

    private static void parseFptosi(Map<Value, Reg> valueToReg, Block asmBlock, FptosiInstr fptosiInstr) {
        Reg src = valueToReg.get(fptosiInstr.getBase());
        Reg dest = new VReg(false);
        asmBlock.addAsm(new CvtAsm(dest, src));
        valueToReg.put(fptosiInstr, dest);
    }

    private static void parseCall(Map<Value, Reg> valueToReg, Block asmBlock, CallInstr callInstr) {
        List<Value> params = callInstr.getParams();
        int iParamNum = 0, fParamNum = 0, spillParamNum = 0;
        for (Value param : params) {
            if (param.getType() == BasicType.FLOAT) {
                if (fParamNum < MReg.F_CALLER_REGS.size()) {
                    if (param instanceof FloatConstant floatConstant) {
                        asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                        asmBlock.addAsm(new MvAsm(MReg.F_CALLER_REGS.get(fParamNum), MReg.T0));
                    } else {
                        asmBlock.addAsm(new MvAsm(MReg.F_CALLER_REGS.get(fParamNum), valueToReg.get(param)));
                    }
                    fParamNum++;
                } else {
                    if (param instanceof FloatConstant floatConstant) {
                        asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                        asmBlock.addAsm(new StoreAsm(MReg.T0, MReg.SP, spillParamNum * 8, 8));
                    } else {
                        asmBlock.addAsm(new StoreAsm(valueToReg.get(param), MReg.SP, spillParamNum * 8, 4));
                    }
                    spillParamNum++;
                }
            } else {
                if (iParamNum < MReg.CALLER_REGS.size()) {
                    if (param instanceof I32Constant i32Constant) {
                        asmBlock.addAsm(new LiAsm(MReg.CALLER_REGS.get(iParamNum), i32Constant.getValue()));
                    } else {
                        asmBlock.addAsm(new MvAsm(MReg.CALLER_REGS.get(iParamNum), valueToReg.get(param)));
                    }
                    iParamNum++;
                } else {
                    if (param instanceof I32Constant i32Constant) {
                        asmBlock.addAsm(new LiAsm(MReg.T0, i32Constant.getValue()));
                        asmBlock.addAsm(new StoreAsm(MReg.T0, MReg.SP, spillParamNum * 8, 8));
                    } else {
                        asmBlock.addAsm(new StoreAsm(valueToReg.get(param), MReg.SP, spillParamNum * 8, 8));
                    }
                    spillParamNum++;
                }
            }
        }
        asmBlock.addAsm(new CallAsm(callInstr.getFunc()));
        if (callInstr.getType() != BasicType.VOID) {
            if (callInstr.getType() == BasicType.FLOAT) {
                Reg retReg = new VReg(true);
                asmBlock.addAsm(new MvAsm(retReg, MReg.FA0));
                valueToReg.put(callInstr, retReg);
            } else {
                Reg retReg = new VReg(false);
                asmBlock.addAsm(new MvAsm(retReg, MReg.A0));
                valueToReg.put(callInstr, retReg);
            }
        }
    }

    private static void parseRet(Map<Value, Reg> valueToReg, Block asmBlock, RetInstr retInstr) {
        if (retInstr.getRetValue() != null) {
            if (retInstr.getRetValue().getType() == BasicType.FLOAT) {
                if (retInstr.getRetValue() instanceof FloatConstant floatConstant) {
                    asmBlock.addAsm(new VRetImmAsm(floatConstant.getValue()));
                } else {
                    Reg retReg = valueToReg.get(retInstr.getRetValue());
                    asmBlock.addAsm(new VRetRegAsm(retReg));
                }
            } else {
                if (retInstr.getRetValue() instanceof I32Constant i32Constant) {
                    asmBlock.addAsm(new VRetImmAsm(i32Constant.getValue()));
                } else {
                    Reg retReg = valueToReg.get(retInstr.getRetValue());
                    asmBlock.addAsm(new VRetRegAsm(retReg));
                }
            }
        }
        asmBlock.addAsm(new VRetVoidAsm());
    }

    private static void parseBitCast(Map<Value, Reg> valueToReg, Block asmBlock, BitCastInstr bitCastInstr) {
        Reg dest = new VReg(false);
        asmBlock.addAsm(new MvAsm(dest, valueToReg.get(bitCastInstr.getBase())));
        valueToReg.put(bitCastInstr, dest);
    }

    private void virtualToMachine() {
        int outerParamNum = calcOuterParamNum(asmFunction);
        Map<Integer, Integer> allocPositions = new HashMap<>();
        int localSize = 0, callerSavedSize = 8;
        for (Map.Entry<Integer, Integer> entry : allocSizes.entrySet()) {
            allocPositions.put(entry.getKey(), outerParamNum * 8 + localSize);
            localSize += entry.getValue();
        }
        Map<VReg, MReg> vRegToMReg = allocRegs(outerParamNum, localSize);
        for (Block block : asmFunction) {
            block.getAsms().replaceAll(asm -> asm.replaceVRegs(vRegToMReg));
        }
        int totalStackSize = outerParamNum * 8 + localSize + callerSavedSize + (iCalleeNum + fCalleeNum + spillNum) * 8;
        replaceVirtualAsms(asmFunction, outerParamNum, allocSizes, allocPositions, localSize, callerSavedSize, iCalleeNum, fCalleeNum, spillNum, totalStackSize);
        List<Block> blocks = asmFunction.getBlocks();
        for (Block block : blocks) {
            List<Asm> asms = block.getAsms();
            List<Asm> newAsms = new ArrayList<>();
            for (Asm asm : asms) {
                if (asm instanceof StoreAsm storeAsm && Math.abs(storeAsm.offset()) > 2040) {
                    newAsms.add(new LiAsm(MReg.T4, storeAsm.offset()));
                    newAsms.add(new RrrAsm(RrrAsm.Op.ADD, MReg.T4, storeAsm.dest(), MReg.T4));
                    newAsms.add(new StoreAsm(storeAsm.src(), MReg.T4, 0, storeAsm.size()));
                    continue;
                }
                if (asm instanceof LoadAsm loadAsm) {
                    newAsms.add(new LiAsm(MReg.T4, loadAsm.offset()));
                    newAsms.add(new RrrAsm(RrrAsm.Op.ADD, MReg.T4, loadAsm.src(), MReg.T4));
                    newAsms.add(new LoadAsm(loadAsm.dest(), MReg.T4, 0, loadAsm.size()));
                    continue;
                }
                newAsms.add(asm);
            }
            asms.clear();
            asms.addAll(newAsms);
        }
        Block firstBlock = blocks.get(0);
        int pos = 0;
        while (pos < totalStackSize) {
            firstBlock.getAsms().add(0, new RriAsm(RriAsm.Op.ADD, MReg.SP, MReg.SP, -Integer.min(totalStackSize - pos, 2040)));
            pos += Integer.min(totalStackSize - pos, 2040);
        }
        for (int i = fCalleeNum - 1; i >= 0; i--) {
            firstBlock.getAsms().add(0, new StoreAsm(MReg.F_CALLEE_REGS.get(i), MReg.SP, -(i + iCalleeNum + 2) * 8, 8));
        }
        for (int i = iCalleeNum - 1; i >= 0; i--) {
            firstBlock.getAsms().add(0, new StoreAsm(MReg.CALLEE_REGS.get(i), MReg.SP, -(i + 2) * 8, 8));
        }
        firstBlock.getAsms().add(0, new StoreAsm(MReg.RA, MReg.SP, -8, 8));
    }

    private Map<VReg, MReg> allocRegs(int outerParamNum, int localSize) {
        Map<VReg, MReg> vRegToMReg;
        boolean toSpill;
        do {
            toSpill = false;
            Deque<MReg> iRegs = new ArrayDeque<>();
            MReg.CALLEE_REGS.forEach(iRegs::addLast);
            Deque<MReg> fRegs = new ArrayDeque<>();
            MReg.F_CALLEE_REGS.forEach(fRegs::addLast);
            Map<VReg, Asm> endAsm = calcEndAsm();
            iCalleeNum = 0;
            fCalleeNum = 0;
            vRegToMReg = new HashMap<>();
            for (Block block : asmFunction) {
                for (Asm asm : block) {
                    List<VReg> vRegs = asm.getVRegs();
                    for (VReg vReg : vRegs) {
                        if (!vRegToMReg.containsKey(vReg)) {
                            MReg mReg;
                            if (vReg.isFloat()) {
                                mReg = fRegs.poll();
                                fCalleeNum = Integer.max(fCalleeNum, MReg.F_CALLEE_REGS.size() - fRegs.size());
                            } else {
                                mReg = iRegs.poll();
                                iCalleeNum = Integer.max(iCalleeNum, MReg.CALLEE_REGS.size() - iRegs.size());
                            }
                            if (mReg != null) {
                                vRegToMReg.put(vReg, mReg);
                            } else {
                                toSpill = true;
                                break;
                            }
                        }
                    }
                    if (toSpill) {
                        break;
                    }
                    for (VReg vReg : vRegs) {
                        if (endAsm.get(vReg) == asm) {
                            MReg mReg = vRegToMReg.get(vReg);
                            if (mReg != null) {
                                if (mReg.isFloat()) {
                                    fRegs.push(mReg);
                                } else {
                                    iRegs.push(mReg);
                                }
                            }
                        }
                    }
                }
                if (toSpill) {
                    break;
                }
            }
            if (toSpill) {
                List<Asm> asms = flattenAsms();
                Pair<Map<VReg, Integer>, Map<VReg, Integer>> beginEndIds = calcBeginEnd(asms);
                Map<VReg, Integer> beginIds = beginEndIds.first();
                Map<VReg, Integer> endIds = beginEndIds.second();
                VReg spillVReg = calcSpillVReg(beginIds, endIds);
                spillVReg(spillVReg, outerParamNum * 8 + localSize + spillNum * 8);
                spillNum++;
            }
        } while (toSpill);
        return vRegToMReg;
    }

    private List<Asm> flattenAsms() {
        List<Asm> asmList = new ArrayList<>();
        for (Block block : asmFunction) {
            for (Asm asm : block) {
                asmList.add(asm);
            }
        }
        return asmList;
    }

    private void spillVReg(VReg vReg, int offset) {
        Map<VReg, Integer> vRegToSpill = Map.of(vReg, offset);
        for (Block block : asmFunction) {
            for (int i = 0; i < block.getAsms().size(); i++) {
                List<Asm> newAsms = block.getAsms().get(i).spill(vRegToSpill);
                block.getAsms().remove(i);
                block.getAsms().addAll(i, newAsms);
                i += newAsms.size() - 1;
            }
        }
    }

    private static VReg calcSpillVReg(Map<VReg, Integer> beginIds, Map<VReg, Integer> endIds) {
        VReg spillVReg = null;
        int maxSpin = 0;
        for (Map.Entry<VReg, Integer> entry : beginIds.entrySet()) {
            int spin = endIds.get(entry.getKey()) - entry.getValue();
            if (spin > maxSpin) {
                maxSpin = spin;
                spillVReg = entry.getKey();
            }
        }
        return spillVReg;
    }

    private static Pair<Map<VReg, Integer>, Map<VReg, Integer>> calcBeginEnd(List<Asm> asms) {
        Map<VReg, Integer> beginIds = new HashMap<>();
        Map<VReg, Integer> endIds = new HashMap<>();
        for (int i = 0; i < asms.size(); i++) {
            Asm asm = asms.get(i);
            for (VReg vReg : asm.getVRegs()) {
                if (!beginIds.containsKey(vReg)) {
                    beginIds.put(vReg, i);
                }
                endIds.put(vReg, i);
            }
        }
        return new Pair<>(beginIds, endIds);
    }

    private Map<VReg, Asm> calcEndAsm() {
        Map<VReg, Asm> endAsm = new HashMap<>();
        for (Block block : asmFunction) {
            for (Asm asm : block) {
                List<VReg> vRegs = asm.getVRegs();
                for (VReg vReg : vRegs) {
                    endAsm.put(vReg, asm);
                }
            }
        }
        return endAsm;
    }

    private void replaceVirtualAsms(compile.codegen.machine.Function function, int outerParamNum, Map<Integer, Integer> allocSizes, Map<Integer, Integer> allocPositions, int localSize, int callerSavedSize, int iCalleeNum, int fCalleeNum, int spillNum, int totalStackSize) {
        List<Block> blocks = function.getBlocks();
        for (Block block : blocks) {
            List<Asm> asms = block.getAsms();
            List<Asm> newAsms = new ArrayList<>();
            for (Asm asm : asms) {
                if (asm instanceof VStoreParamAsm vStoreParamAsm) {
                    newAsms.add(new LoadAsm(MReg.T1, MReg.SP, outerParamNum * 8 + localSize + callerSavedSize + (iCalleeNum + fCalleeNum + spillNum + vStoreParamAsm.src()) * 8, 8));
                    newAsms.add(new StoreAsm(MReg.T1, MReg.SP, allocPositions.get(vStoreParamAsm.dest()), 8));
                    continue;
                }
                if (asm instanceof VAddLocalAsm vAddLocalAsm) {
                    int imm = allocPositions.get(vAddLocalAsm.local());
                    if (imm > 2040) {
                        newAsms.add(new LiAsm(MReg.T1, imm));
                        newAsms.add(new RrrAsm(RrrAsm.Op.ADD, vAddLocalAsm.dest(), vAddLocalAsm.src(), MReg.T1));
                    } else {
                        newAsms.add(new RriAsm(RriAsm.Op.ADD, vAddLocalAsm.dest(), vAddLocalAsm.src(), imm));
                    }
                    continue;
                }
                if (asm instanceof VLoadLocalAsm vLoadLocalAsm) {
                    int offset = allocPositions.get(vLoadLocalAsm.src());
                    int size = allocSizes.get(vLoadLocalAsm.src());
                    if (offset > 2040) {
                        newAsms.add(new LiAsm(MReg.T1, offset));
                        newAsms.add(new RrrAsm(RrrAsm.Op.ADD, MReg.T2, MReg.SP, MReg.T1));
                        newAsms.add(new LoadAsm(vLoadLocalAsm.dest(), MReg.T2, 0, size));
                    } else {
                        newAsms.add(new LoadAsm(vLoadLocalAsm.dest(), MReg.SP, offset, size));
                    }
                    continue;
                }
                if (asm instanceof VLoadGlobalAsm vLoadGlobalAsm) {
                    newAsms.add(new LlaAsm(MReg.T1, dataItems.get(vLoadGlobalAsm.src())));
                    newAsms.add(new LoadAsm(vLoadGlobalAsm.dest(), MReg.T1, 0, 4));
                    continue;
                }
                if (asm instanceof VRetImmAsm vRetImmAsm) {
                    if (vRetImmAsm.imm() instanceof Float) {
                        newAsms.add(new LiAsm(MReg.A0, Float.floatToIntBits(vRetImmAsm.imm().floatValue())));
                        newAsms.add(new MvAsm(MReg.FA0, MReg.A0));
                    } else {
                        newAsms.add(new LiAsm(MReg.A0, vRetImmAsm.imm().intValue()));
                    }
                    restoreContext(newAsms, iCalleeNum, fCalleeNum, totalStackSize);
                    continue;
                }
                if (asm instanceof VRetRegAsm vRetRegAsm) {
                    if (vRetRegAsm.reg().isFloat()) {
                        newAsms.add(new MvAsm(MReg.FA0, vRetRegAsm.reg()));
                    } else {
                        newAsms.add(new MvAsm(MReg.A0, vRetRegAsm.reg()));
                    }
                    restoreContext(newAsms, iCalleeNum, fCalleeNum, totalStackSize);
                    continue;
                }
                if (asm instanceof VRetVoidAsm) {
                    restoreContext(newAsms, iCalleeNum, fCalleeNum, totalStackSize);
                    continue;
                }
                if (asm instanceof VStoreLocalAsm vStoreLocalAsm) {
                    int offset = allocPositions.get(vStoreLocalAsm.dest());
                    int size = allocSizes.get(vStoreLocalAsm.dest());
                    if (offset > 2040) {
                        newAsms.add(new LiAsm(MReg.T1, offset));
                        newAsms.add(new RrrAsm(RrrAsm.Op.ADD, MReg.T2, MReg.SP, MReg.T1));
                        newAsms.add(new StoreAsm(vStoreLocalAsm.src(), MReg.T2, 0, size));
                    } else {
                        newAsms.add(new StoreAsm(vStoreLocalAsm.src(), MReg.SP, offset, size));
                    }
                    continue;
                }
                newAsms.add(asm);
            }
            asms.clear();
            asms.addAll(newAsms);
        }
    }

    private void restoreContext(List<Asm> newAsms, int iCalleeNum, int fCalleeNum, int totalStackSize) {
        int pos = 0;
        while (pos < totalStackSize) {
            newAsms.add(new RriAsm(RriAsm.Op.ADD, MReg.SP, MReg.SP, Integer.min(totalStackSize - pos, 2040)));
            pos += Integer.min(totalStackSize - pos, 2040);
        }
        for (int i = 0; i < fCalleeNum; i++) {
            newAsms.add(new LoadAsm(MReg.F_CALLEE_REGS.get(i), MReg.SP, -(i + iCalleeNum + 2) * 8, 8));
        }
        for (int i = 0; i < iCalleeNum; i++) {
            newAsms.add(new LoadAsm(MReg.CALLEE_REGS.get(i), MReg.SP, -(i + 2) * 8, 8));
        }
        newAsms.add(new LoadAsm(MReg.RA, MReg.SP, -8, 8));
        newAsms.add(new RetAsm());
    }

    private int calcOuterParamNum(compile.codegen.machine.Function function) {
        int outerParamNum = 0;
        for (Block block : function) {
            for (Asm asm : block) {
                if (asm instanceof CallAsm callAsm) {
                    if (module.getFunctions().containsKey(callAsm.name())) {
                        List<Param> params = module.getFunctions().get(callAsm.name()).getParams();
                        int iRegNum = 0, fRegNum = 0, spill = 0;
                        for (Param param : params) {
                            if (param.getType() == BasicType.FLOAT) {
                                if (fRegNum < MReg.F_CALLER_REGS.size()) {
                                    fRegNum++;
                                } else {
                                    spill++;
                                }
                            } else {
                                if (iRegNum < MReg.CALLER_REGS.size()) {
                                    iRegNum++;
                                } else {
                                    spill++;
                                }
                            }
                        }
                        outerParamNum = Integer.max(outerParamNum, spill);
                    }
                }
            }
        }
        return outerParamNum;
    }
}
