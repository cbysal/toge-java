package compile.codegen;

import compile.codegen.machine.Block;
import compile.codegen.machine.DataItem;
import compile.codegen.machine.TextItem;
import compile.codegen.machine.asm.*;
import compile.codegen.machine.reg.MReg;
import compile.codegen.machine.reg.Reg;
import compile.codegen.machine.reg.VReg;
import compile.llvm.ir.Module;
import compile.llvm.ir.*;
import compile.llvm.ir.constant.FloatConstant;
import compile.llvm.ir.constant.I32Constant;
import compile.llvm.ir.instr.*;
import compile.llvm.ir.type.ArrayType;
import compile.llvm.ir.type.BasicType;
import compile.llvm.ir.type.PointerType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO A BUNCH OF SHIT! DELETE THIS LATER!
public class CodeGenerator {
    private boolean isProcessed;
    private final Module module;
    private final Map<String, TextItem> textItems = new HashMap<>();
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
        llvmToVirtualAsm();
    }

    private void llvmToVirtualAsm() {
        Map<String, Global> globals = module.getGlobals();
        Map<String, compile.llvm.ir.Function> functions = module.getFunctions();
        for (Map.Entry<String, Global> global : globals.entrySet()) {
            globalToData(global.getKey(), global.getValue());
        }
        for (Map.Entry<String, compile.llvm.ir.Function> function : functions.entrySet()) {
            llvmFunctionToAsmFunction(function.getKey(), function.getValue());
        }
    }

    private void globalToData(String name, Global global) {
        DataItem dataItem = new DataItem(name, ((PointerType) global.getType()).base().getSize());
        Map<Integer, Integer> values = global.flatten();
        values.forEach(dataItem::set);
        dataItems.put(name, dataItem);
    }

    private void llvmFunctionToAsmFunction(String name, compile.llvm.ir.Function function) {
        compile.codegen.machine.Function asmFunction = new compile.codegen.machine.Function(name);
        int stackSize = 456;
        Map<Value, Integer> allocSizes = new HashMap<>();
        Map<Value, Integer> allocPosition = new HashMap<>();
        Map<Value, Reg> valueToReg = new HashMap<>();
        List<Param> paramList = function.getParams();
        int iParamSize = 0, fParamSize = 0, spillParamSize = 0;
        for (Param param : paramList) {
            if (param.getType() == BasicType.FLOAT) {
                if (fParamSize < MReg.F_CALLER_REGS.size()) {
                    valueToReg.put(param, MReg.F_CALLER_REGS.get(fParamSize));
                    fParamSize++;
                } else {
                    allocSizes.put(param, 8);
                    allocPosition.put(param, spillParamSize * 8);
                    spillParamSize++;
                }
            } else {
                if (iParamSize < MReg.CALLER_REGS.size()) {
                    valueToReg.put(param, MReg.CALLER_REGS.get(iParamSize));
                    iParamSize++;
                } else {
                    allocSizes.put(param, 8);
                    allocPosition.put(param, spillParamSize * 8);
                    spillParamSize++;
                }
            }
        }
        for (BasicBlock block = function.getFirst(); block != null; block = block.getNext()) {
            for (Instr instr = block.getFirst(); instr != null; instr = instr.getNext()) {
                if (instr instanceof AllocInstr allocInstr) {
                    int allocSize = ((PointerType) allocInstr.getType()).base().getSize();
                    allocSizes.put(allocInstr, allocSize);
                    stackSize += allocSize;
                    allocPosition.put(allocInstr, -stackSize);
                    continue;
                }
                break;
            }
        }
        int maxParamNum = 0;
        for (BasicBlock block = function.getFirst(); block != null; block = block.getNext()) {
            for (Instr instr = block.getFirst(); instr != null; instr = instr.getNext()) {
                if (instr instanceof CallInstr callInstr) {
                    maxParamNum = Integer.max(maxParamNum, callInstr.getParams().size());
                }
            }
        }
        if (maxParamNum > MReg.CALLER_REGS.size()) {
            stackSize += (maxParamNum - MReg.CALLER_REGS.size()) * 8;
        }
        int finalStackSize = stackSize;
        allocPosition = allocPosition.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue() + finalStackSize));
        Map<BasicBlock, Block> llvmToAsmBlockMap = new HashMap<>();
        for (BasicBlock block = function.getFirst(); block != null; block = block.getNext()) {
            Block asmBlock = new Block();
            asmFunction.addBlock(asmBlock);
            llvmToAsmBlockMap.put(block, asmBlock);
        }
        for (BasicBlock block = function.getFirst(); block != null; block = block.getNext()) {
            Block asmBlock = llvmToAsmBlockMap.get(block);
            for (Instr instr = block.getFirst(); instr != null; instr = instr.getNext()) {
                if (instr instanceof AllocInstr) {
                    continue;
                }
                if (instr instanceof LoadInstr loadInstr) {
                    Value src = loadInstr.getSrc();
                    if (loadInstr.getType() == BasicType.FLOAT) {
                        Reg destReg = new VReg(true);
                        if (src instanceof Global global) {
                            Reg srcReg = new VReg(false);
                            DataItem dataItem = dataItems.get(global.getName());
                            asmBlock.addAsm(new LlaAsm(srcReg, dataItem));
                            asmBlock.addAsm(new FlwAsm(destReg, srcReg));
                        } else if (allocPosition.containsKey(src)) {
                            int size = allocSizes.get(src);
                            int pos = allocPosition.get(src);
                            if (Math.abs(pos) >= 2048) {
                                asmBlock.addAsm(new LiAsm(MReg.T0, pos));
                                asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, MReg.T1, MReg.SP, MReg.T0));
                                if (size == 4) {
                                    asmBlock.addAsm(new FlwAsm(destReg, MReg.T1));
                                } else {
                                    asmBlock.addAsm(new FldAsm(destReg, MReg.T1));
                                }
                            } else {
                                if (size == 4) {
                                    asmBlock.addAsm(new FlwAsm(destReg, MReg.SP, pos));
                                } else {
                                    asmBlock.addAsm(new FldAsm(destReg, MReg.SP, pos));
                                }
                            }
                        } else {
                            Reg srcReg = valueToReg.get(src);
                            asmBlock.addAsm(new FlwAsm(destReg, srcReg));
                        }
                        valueToReg.put(loadInstr, destReg);
                    } else {
                        Reg destReg = new VReg(false);
                        if (src instanceof Global global) {
                            Reg srcReg = new VReg(false);
                            DataItem dataItem = dataItems.get(global.getName());
                            asmBlock.addAsm(new LlaAsm(srcReg, dataItem));
                            asmBlock.addAsm(new LwAsm(destReg, srcReg));
                        } else if (allocPosition.containsKey(src)) {
                            int size = allocSizes.get(src);
                            int pos = allocPosition.get(src);
                            if (Math.abs(pos) >= 2048) {
                                asmBlock.addAsm(new LiAsm(MReg.T0, pos));
                                asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, MReg.T1, MReg.SP, MReg.T0));
                                if (size == 4) {
                                    asmBlock.addAsm(new LwAsm(destReg, MReg.T1));
                                } else {
                                    asmBlock.addAsm(new LdAsm(destReg, MReg.T1));
                                }
                            } else {
                                if (size == 4) {
                                    asmBlock.addAsm(new LwAsm(destReg, MReg.SP, pos));
                                } else {
                                    asmBlock.addAsm(new LdAsm(destReg, MReg.SP, pos));
                                }
                            }
                        } else {
                            Reg srcReg = valueToReg.get(src);
                            asmBlock.addAsm(new LwAsm(destReg, srcReg));
                        }
                        valueToReg.put(loadInstr, destReg);
                    }
                    continue;
                }
                if (instr instanceof StoreInstr storeInstr) {
                    Value src = storeInstr.getSrc();
                    if (src.getType() == BasicType.FLOAT) {
                        Reg srcReg;
                        if (src instanceof FloatConstant floatConstant) {
                            srcReg = new VReg(true);
                            asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                            asmBlock.addAsm(new FmvWXAsm(srcReg, MReg.T0));
                        } else if (allocPosition.containsKey(src)) {
                            srcReg = new VReg(true);
                            asmBlock.addAsm(new FldAsm(srcReg, MReg.SP, allocPosition.get(src)));
                        } else {
                            srcReg = valueToReg.get(src);
                        }
                        Value dst = storeInstr.getDst();
                        if (dst instanceof Global global) {
                            Reg destReg = new VReg(false);
                            asmBlock.addAsm(new LlaAsm(destReg, dataItems.get(global.getName())));
                            asmBlock.addAsm(new FswAsm(srcReg, destReg));
                        } else if (allocPosition.containsKey(dst)) {
                            int size = allocSizes.get(dst);
                            int pos = allocPosition.get(dst);
                            if (Math.abs(pos) >= 2048) {
                                asmBlock.addAsm(new LiAsm(MReg.T0, pos));
                                asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, MReg.T1, MReg.SP, MReg.T0));
                                if (size == 4) {
                                    asmBlock.addAsm(new FswAsm(srcReg, MReg.T1));
                                } else {
                                    asmBlock.addAsm(new FsdAsm(srcReg, MReg.T1));
                                }
                            } else {
                                if (size == 4) {
                                    asmBlock.addAsm(new FswAsm(srcReg, MReg.SP, pos));
                                } else {
                                    asmBlock.addAsm(new FsdAsm(srcReg, MReg.SP, pos));
                                }
                            }
                        } else {
                            Reg destReg = valueToReg.get(dst);
                            asmBlock.addAsm(new FswAsm(srcReg, destReg));
                        }
                    } else {
                        Reg srcReg;
                        if (src instanceof I32Constant i32Constant) {
                            srcReg = new VReg(false);
                            asmBlock.addAsm(new LiAsm(srcReg, i32Constant.getValue()));
                        } else if (allocPosition.containsKey(src)) {
                            srcReg = new VReg(false);
                            asmBlock.addAsm(new LdAsm(srcReg, MReg.SP, allocPosition.get(src)));
                        } else {
                            srcReg = valueToReg.get(src);
                        }
                        Value dst = storeInstr.getDst();
                        if (dst instanceof Global global) {
                            Reg destReg = new VReg(false);
                            asmBlock.addAsm(new LlaAsm(destReg, dataItems.get(global.getName())));
                            asmBlock.addAsm(new SwAsm(srcReg, destReg));
                        } else if (allocPosition.containsKey(dst)) {
                            int size = allocSizes.get(dst);
                            int pos = allocPosition.get(dst);
                            if (Math.abs(pos) >= 2048) {
                                asmBlock.addAsm(new LiAsm(MReg.T0, pos));
                                asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, MReg.T1, MReg.SP, MReg.T0));
                                if (size == 4) {
                                    asmBlock.addAsm(new SwAsm(srcReg, MReg.T1));
                                } else {
                                    asmBlock.addAsm(new SdAsm(srcReg, MReg.T1));
                                }
                            } else {
                                if (size == 4) {
                                    asmBlock.addAsm(new SwAsm(srcReg, MReg.SP, pos));
                                } else {
                                    asmBlock.addAsm(new SdAsm(srcReg, MReg.SP, pos));
                                }
                            }
                        } else {
                            Reg destReg = valueToReg.get(dst);
                            asmBlock.addAsm(new SwAsm(srcReg, destReg));
                        }
                    }
                    continue;
                }
                if (instr instanceof GetElementPtrInstr getElementPtrInstr) {
                    Value base = getElementPtrInstr.getBase();
                    Value index = getElementPtrInstr.getIndex();
                    if (index != null) {
                        Reg src;
                        if (base instanceof Global global) {
                            src = new VReg(false);
                            asmBlock.addAsm(new LlaAsm(src, dataItems.get(global.getName())));
                        } else if (allocPosition.containsKey(base)) {
                            src = new VReg(false);
                            int pos = allocPosition.get(base);
                            if (Math.abs(pos) >= 2048) {
                                asmBlock.addAsm(new LiAsm(MReg.T0, pos));
                                asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, src, MReg.SP, MReg.T0));
                            } else {
                                asmBlock.addAsm(new RriAsm(RriAsm.Type.ADD, src, MReg.SP, allocPosition.get(base)));
                            }
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
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.MUL, offset, indexReg, sizeReg));
                        }
                        Reg dest = new VReg(false);
                        asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, dest, src, offset));
                        valueToReg.put(getElementPtrInstr, dest);
                    } else {
                        index = getElementPtrInstr.getOffset();
                        Reg src;
                        if (base instanceof Global global) {
                            src = new VReg(false);
                            asmBlock.addAsm(new LlaAsm(src, dataItems.get(global.getName())));
                        } else if (allocPosition.containsKey(base)) {
                            src = new VReg(false);
                            int pos = allocPosition.get(base);
                            if (Math.abs(pos) >= 2048) {
                                asmBlock.addAsm(new LiAsm(MReg.T0, pos));
                                asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, src, MReg.SP, MReg.T0));
                            } else {
                                asmBlock.addAsm(new RriAsm(RriAsm.Type.ADD, src, MReg.SP, allocPosition.get(base)));
                            }
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
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.MUL, offset, indexReg, sizeReg));
                        }
                        Reg dest = new VReg(false);
                        asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, dest, src, offset));
                        valueToReg.put(getElementPtrInstr, dest);
                    }
                    continue;
                }
                if (instr instanceof BinaryInstr binaryInstr) {
                    if (binaryInstr.getType() == BasicType.FLOAT) {
                        Reg dest = new VReg(true);
                        Reg src1, src2;
                        if (binaryInstr.getlVal() instanceof I32Constant i32Constant) {
                            src1 = new VReg(false);
                            asmBlock.addAsm(new LiAsm(src1, i32Constant.getValue()));
                        } else if (binaryInstr.getlVal() instanceof FloatConstant floatConstant) {
                            src1 = new VReg(true);
                            asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                            asmBlock.addAsm(new FmvWXAsm(src1, MReg.T0));
                        } else {
                            src1 = valueToReg.get(binaryInstr.getlVal());
                        }
                        if (binaryInstr.getrVal() instanceof I32Constant i32Constant) {
                            src2 = new VReg(false);
                            asmBlock.addAsm(new LiAsm(src2, i32Constant.getValue()));
                        } else if (binaryInstr.getrVal() instanceof FloatConstant floatConstant) {
                            src2 = new VReg(true);
                            asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                            asmBlock.addAsm(new FmvWXAsm(src2, MReg.T0));
                        } else {
                            src2 = valueToReg.get(binaryInstr.getrVal());
                        }
                        if (binaryInstr.getOp() == BinaryInstr.Op.XOR) {
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.SLTU, dest, src1, MReg.ZERO));
                        } else {
                            asmBlock.addAsm(new RrrAsm(switch (binaryInstr.getOp()) {
                                case ADD -> RrrAsm.Type.ADD;
                                case SUB -> RrrAsm.Type.SUB;
                                case MUL -> RrrAsm.Type.MUL;
                                case SDIV -> RrrAsm.Type.DIV;
                                case SREM -> RrrAsm.Type.REM;
                                case FADD -> RrrAsm.Type.FADD;
                                case FSUB -> RrrAsm.Type.FSUB;
                                case FMUL -> RrrAsm.Type.FMUL;
                                case FDIV -> RrrAsm.Type.FDIV;
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
                            asmBlock.addAsm(new FmvWXAsm(src1, MReg.T0));
                        } else {
                            src1 = valueToReg.get(binaryInstr.getlVal());
                        }
                        if (binaryInstr.getrVal() instanceof I32Constant i32Constant) {
                            src2 = new VReg(false);
                            asmBlock.addAsm(new LiAsm(src2, i32Constant.getValue()));
                        } else if (binaryInstr.getrVal() instanceof FloatConstant floatConstant) {
                            src2 = new VReg(true);
                            asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                            asmBlock.addAsm(new FmvWXAsm(src2, MReg.T0));
                        } else {
                            src2 = valueToReg.get(binaryInstr.getrVal());
                        }
                        if (binaryInstr.getOp() == BinaryInstr.Op.XOR) {
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.SLTU, dest, src1, MReg.ZERO));
                        } else {
                            asmBlock.addAsm(new RrrAsm(switch (binaryInstr.getOp()) {
                                case ADD -> RrrAsm.Type.ADD;
                                case SUB -> RrrAsm.Type.SUB;
                                case MUL -> RrrAsm.Type.MUL;
                                case SDIV -> RrrAsm.Type.DIV;
                                case SREM -> RrrAsm.Type.REM;
                                case FADD -> RrrAsm.Type.FADD;
                                case FSUB -> RrrAsm.Type.FSUB;
                                case FMUL -> RrrAsm.Type.FMUL;
                                case FDIV -> RrrAsm.Type.FDIV;
                                default -> throw new IllegalStateException("Unexpected value: " + binaryInstr.getOp());
                            }, dest, src1, src2));
                        }
                        valueToReg.put(binaryInstr, dest);
                    }
                    continue;
                }
                if (instr instanceof CmpInstr cmpInstr) {
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
                        asmBlock.addAsm(new FmvWXAsm(src1, MReg.T0));
                    } else {
                        src1 = valueToReg.get(lVal);
                    }
                    if (rVal instanceof I32Constant i32Constant) {
                        src2 = new VReg(false);
                        asmBlock.addAsm(new LiAsm(src2, i32Constant.getValue()));
                    } else if (rVal instanceof FloatConstant floatConstant) {
                        src2 = new VReg(true);
                        asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                        asmBlock.addAsm(new FmvWXAsm(src2, MReg.T0));
                    } else {
                        src2 = valueToReg.get(rVal);
                    }
                    switch (op) {
                        case EQ -> {
                            Reg tempReg = new VReg(false);
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.SUB, tempReg, src1, src2));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.SLTIU, dest, tempReg, 1));
                        }
                        case NE -> {
                            Reg tempReg1 = new VReg(false);
                            Reg tempReg2 = new VReg(false);
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.SUB, tempReg1, src1, src2));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.SLTIU, tempReg2, tempReg1, 1));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.SLTIU, dest, tempReg2, 1));
                        }
                        case SGE -> {
                            Reg tempReg = new VReg(false);
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.SUB, tempReg, src2, src1));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.SLTI, dest, tempReg, 1));
                        }
                        case SGT -> {
                            Reg tempReg = new VReg(false);
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.SUB, tempReg, src2, src1));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.SLTI, dest, tempReg, 0));
                        }
                        case SLE -> {
                            Reg tempReg = new VReg(false);
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.SUB, tempReg, src1, src2));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.SLTI, dest, tempReg, 1));
                        }
                        case SLT -> asmBlock.addAsm(new RrrAsm(RrrAsm.Type.SLT, dest, src1, src2));
                    }
                    valueToReg.put(cmpInstr, dest);
                    continue;
                }
                if (instr instanceof FcmpInstr fcmpInstr) {
                    FcmpInstr.Op op = fcmpInstr.getOp();
                    Value lVal = fcmpInstr.getlVal();
                    Value rVal = fcmpInstr.getrVal();
                    Reg src1, src2;
                    Reg dest = new VReg(false);
                    if (lVal instanceof I32Constant i32Constant) {
                        src1 = new VReg(false);
                        asmBlock.addAsm(new LiAsm(src1, i32Constant.getValue()));
                    } else if (lVal instanceof FloatConstant floatConstant) {
                        src1 = new VReg(true);
                        asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                        asmBlock.addAsm(new FmvWXAsm(src1, MReg.T0));
                    } else {
                        src1 = valueToReg.get(lVal);
                    }
                    if (rVal instanceof I32Constant i32Constant) {
                        src2 = new VReg(false);
                        asmBlock.addAsm(new LiAsm(src2, i32Constant.getValue()));
                    } else if (rVal instanceof FloatConstant floatConstant) {
                        src2 = new VReg(true);
                        asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                        asmBlock.addAsm(new FmvWXAsm(src2, MReg.T0));
                    } else {
                        src2 = valueToReg.get(rVal);
                    }
                    switch (op) {
                        case OEQ -> asmBlock.addAsm(new RrrAsm(RrrAsm.Type.FEQ, dest, src1, src2));
                        case UNE -> {
                            Reg tempReg = new VReg(false);
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.FEQ, tempReg, src1, src2));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.XORI, dest, tempReg, 1));
                        }
                        case OGE -> {
                            Reg tempReg = new VReg(false);
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.FLT, tempReg, src1, src2));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.XORI, dest, tempReg, 1));
                        }
                        case OGT -> {
                            Reg tempReg = new VReg(false);
                            asmBlock.addAsm(new RrrAsm(RrrAsm.Type.FLE, tempReg, src1, src2));
                            asmBlock.addAsm(new RriAsm(RriAsm.Type.XORI, dest, tempReg, 1));
                        }
                        case OLE -> asmBlock.addAsm(new RrrAsm(RrrAsm.Type.FLE, dest, src1, src2));
                        case OLT -> asmBlock.addAsm(new RrrAsm(RrrAsm.Type.FLT, dest, src1, src2));
                    }
                    valueToReg.put(fcmpInstr, dest);
                    continue;
                }
                if (instr instanceof BranchInstr branchInstr) {
                    if (branchInstr.getCond() == null) {
                        asmBlock.addAsm(new JAsm(llvmToAsmBlockMap.get(branchInstr.getTrueBlock())));
                    } else {
                        Reg cond = valueToReg.get(branchInstr.getCond());
                        asmBlock.addAsm(new BAsm(BAsm.Op.NE, cond, MReg.ZERO,
                                llvmToAsmBlockMap.get(branchInstr.getTrueBlock())));
                        asmBlock.addAsm(new JAsm(llvmToAsmBlockMap.get(branchInstr.getFalseBlock())));
                    }
                    continue;
                }
                if (instr instanceof FnegInstr fnegInstr) {
                    Reg src;
                    if (fnegInstr.getBase() instanceof FloatConstant floatConstant) {
                        src = new VReg(true);
                        asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                        asmBlock.addAsm(new FmvWXAsm(src, MReg.T0));
                    } else {
                        src = valueToReg.get(fnegInstr.getBase());
                    }
                    Reg dest = new VReg(true);
                    asmBlock.addAsm(new FnegASM(dest, src));
                    valueToReg.put(fnegInstr, dest);
                    continue;
                }
                if (instr instanceof ZextInstr zextInstr) {
                    Reg src = valueToReg.get(zextInstr.getValue());
                    Reg dest = new VReg(false);
                    asmBlock.addAsm(new MvAsm(dest, src));
                    valueToReg.put(zextInstr, dest);
                    continue;
                }
                if (instr instanceof SitofpInstr sitofpInstr) {
                    Reg src;
                    if (sitofpInstr.getBase() instanceof I32Constant i32Constant) {
                        src = new VReg(false);
                        asmBlock.addAsm(new LiAsm(src, i32Constant.getValue()));
                    } else {
                        src = valueToReg.get(sitofpInstr.getBase());
                    }
                    Reg dest = new VReg(true);
                    asmBlock.addAsm(new FcvtSWAsm(dest, src));
                    valueToReg.put(sitofpInstr, dest);
                    continue;
                }
                if (instr instanceof FptosiInstr fptosiInstr) {
                    Reg src = valueToReg.get(fptosiInstr.getBase());
                    Reg dest = new VReg(false);
                    asmBlock.addAsm(new FcvtWSAsm(dest, src));
                    valueToReg.put(fptosiInstr, dest);
                    continue;
                }
                if (instr instanceof CallInstr callInstr) {
                    List<Value> params = callInstr.getParams();
                    int iParamNum = 0, fParamNum = 0, spillParamNum = 0;
                    for (Value param : params) {
                        if (param.getType() == BasicType.FLOAT) {
                            if (fParamNum < MReg.F_CALLER_REGS.size()) {
                                if (param instanceof FloatConstant floatConstant) {
                                    asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                                    asmBlock.addAsm(new FmvWXAsm(MReg.F_CALLER_REGS.get(fParamNum), MReg.T0));
                                } else {
                                    asmBlock.addAsm(new FmvSAsm(MReg.F_CALLER_REGS.get(fParamNum),
                                            valueToReg.get(param)));
                                }
                                fParamNum++;
                            } else {
                                if (param instanceof FloatConstant floatConstant) {
                                    asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                                    asmBlock.addAsm(new SdAsm(MReg.T0, MReg.SP, spillParamNum * 8));
                                } else {
                                    asmBlock.addAsm(new FsdAsm(valueToReg.get(param), MReg.SP, spillParamNum * 8));
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
                                    asmBlock.addAsm(new SdAsm(MReg.T0, MReg.SP, spillParamNum * 8));
                                } else {
                                    asmBlock.addAsm(new SdAsm(valueToReg.get(param), MReg.SP, spillParamNum * 8));
                                }
                                spillParamNum++;
                            }
                        }
                    }
                    asmBlock.addAsm(new CallAsm(callInstr.getFunc()));
                    if (callInstr.getType() != BasicType.VOID) {
                        if (callInstr.getType() == BasicType.FLOAT) {
                            Reg retReg = new VReg(true);
                            asmBlock.addAsm(new FmvSAsm(retReg, MReg.FA0));
                            valueToReg.put(callInstr, retReg);
                        } else {
                            Reg retReg = new VReg(false);
                            asmBlock.addAsm(new MvAsm(retReg, MReg.A0));
                            valueToReg.put(callInstr, retReg);
                        }
                    }
                    continue;
                }
                if (instr instanceof RetInstr retInstr) {
                    if (retInstr.getRetValue() != null) {
                        if (retInstr.getRetValue().getType() == BasicType.FLOAT) {
                            if (retInstr.getRetValue() instanceof FloatConstant floatConstant) {
                                asmBlock.addAsm(new LiAsm(MReg.T0, Float.floatToIntBits(floatConstant.getValue())));
                                asmBlock.addAsm(new FmvWXAsm(MReg.FA0, MReg.T0));
                            } else {
                                Reg retReg = valueToReg.get(retInstr.getRetValue());
                                asmBlock.addAsm(new FmvSAsm(MReg.FA0, retReg));
                            }
                        } else {
                            if (retInstr.getRetValue() instanceof I32Constant i32Constant) {
                                asmBlock.addAsm(new LiAsm(MReg.A0, i32Constant.getValue()));
                            } else {
                                Reg retReg = valueToReg.get(retInstr.getRetValue());
                                asmBlock.addAsm(new MvAsm(MReg.A0, retReg));
                            }
                        }
                    }
                    if (Math.abs(stackSize) >= 2048) {
                        asmBlock.addAsm(new LiAsm(MReg.T0, stackSize));
                        asmBlock.addAsm(new RrrAsm(RrrAsm.Type.ADD, MReg.SP, MReg.SP, MReg.T0));
                    } else {
                        asmBlock.addAsm(new RriAsm(RriAsm.Type.ADD, MReg.SP, MReg.SP, stackSize));
                    }
                    for (int i = 0; i < MReg.F_CALLEE_REGS.size(); i++) {
                        asmBlock.addAsm(new FldAsm(MReg.F_CALLEE_REGS.get(i), MReg.SP, -(8 * (i + 2)) - 104));
                    }
                    for (int i = 0; i < MReg.CALLEE_REGS.size(); i++) {
                        asmBlock.addAsm(new LdAsm(MReg.CALLEE_REGS.get(i), MReg.SP, -(8 * (i + 2))));
                    }
                    asmBlock.addAsm(new LdAsm(MReg.RA, MReg.SP, -8));
                    asmBlock.addAsm(new RetAsm());
                    continue;
                }
                throw new RuntimeException("Unhandled instr: " + instr);
            }
        }
        List<Block> blocks = asmFunction.getBlocks();
        Block firstBlock = blocks.get(0);
        if (Math.abs(stackSize) >= 2048) {
            firstBlock.getAsms().add(0, new RrrAsm(RrrAsm.Type.ADD, MReg.SP, MReg.SP, MReg.T0));
            firstBlock.getAsms().add(0, new LiAsm(MReg.T0, -stackSize));
        } else {
            firstBlock.getAsms().add(0, new RriAsm(RriAsm.Type.ADD, MReg.SP, MReg.SP, -stackSize));
        }
        for (int i = 0; i < MReg.F_CALLEE_REGS.size(); i++) {
            firstBlock.getAsms().add(0, new FsdAsm(MReg.F_CALLEE_REGS.get(i), MReg.SP, -(8 * (i + 2)) - 104));
        }
        for (int i = 0; i < MReg.CALLEE_REGS.size(); i++) {
            firstBlock.getAsms().add(0, new SdAsm(MReg.CALLEE_REGS.get(i), MReg.SP, -(8 * (i + 2))));
        }
        firstBlock.getAsms().add(0, new SdAsm(MReg.RA, MReg.SP, -8));
        Map<VReg, MReg> vRegToMReg = new HashMap<>();
        Deque<MReg> iRegs = new ArrayDeque<>();
        MReg.CALLEE_REGS.forEach(iRegs::addLast);
        Deque<MReg> fRegs = new ArrayDeque<>();
        MReg.F_CALLEE_REGS.forEach(fRegs::addLast);
        Map<VReg, Integer> vRegToSpill = new HashMap<>();
        Deque<Integer> spills = new ArrayDeque<>(32);
        int finalStackSize1 = stackSize;
        IntStream.range(0, 32).map(i -> finalStackSize1 - 456 + i * 8).forEach(spills::addLast);
        Map<VReg, Asm> endAsm = new HashMap<>();
        for (Block block : asmFunction) {
            for (Asm asm : block) {
                List<VReg> vRegs = asm.getVRegs();
                for (VReg vReg : vRegs) {
                    endAsm.put(vReg, asm);
                }
            }
        }
        for (Block block : asmFunction) {
            for (Asm asm : block) {
                List<VReg> vRegs = asm.getVRegs();
                for (VReg vReg : vRegs) {
                    if (!vRegToMReg.containsKey(vReg) && !vRegToSpill.containsKey(vReg)) {
                        if (vReg.isFloat()) {
                            MReg mReg = fRegs.poll();
                            if (mReg != null) {
                                vRegToMReg.put(vReg, mReg);
                            } else {
                                int spill = spills.poll();
                                vRegToSpill.put(vReg, spill);
                            }
                        } else {
                            MReg mReg = iRegs.poll();
                            if (mReg != null) {
                                vRegToMReg.put(vReg, mReg);
                            } else {
                                int spill = spills.poll();
                                vRegToSpill.put(vReg, spill);
                            }
                        }
                    }
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
                        } else {
                            int spill = vRegToSpill.get(vReg);
                            spills.push(spill);
                        }
                    }
                }
            }
        }
        for (Block block : asmFunction) {
            for (int i = 0; i < block.getAsms().size(); i++) {
                Asm asm = block.getAsms().get(i);
                asm.replaceVRegs(vRegToMReg);
                List<Asm> newAsms = asm.spill(vRegToSpill);
                block.getAsms().remove(i);
                block.getAsms().addAll(i, newAsms);
                i += newAsms.size() - 1;
            }
        }
        functions.put(name, asmFunction);
    }

    public Map<String, TextItem> getTextItems() {
        checkIfIsProcessed();
        return textItems;
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
