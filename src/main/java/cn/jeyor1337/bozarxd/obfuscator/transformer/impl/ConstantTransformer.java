package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class ConstantTransformer extends ClassTransformer {

    private final Map<String, StringEncryptionContext> classContexts = new HashMap<>();

    public ConstantTransformer(Bozar bozar) {
        super(bozar, "Constant obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void pre() {
        classContexts.clear();
    }

    private enum NumberObfType {
        RANDOM_SEED,
        MULTI_XOR_SHIFT,
        BITWISE_ROTATE,
        HASH_CHAIN,
        MIXED_OPS
    }

    private void obfuscateNumbers(ClassNode classNode, MethodNode methodNode) {
        Arrays.stream(methodNode.instructions.toArray())
                .filter(insn -> ASMUtils.isPushInt(insn) || ASMUtils.isPushLong(insn)
                        || ASMUtils.isPushFloat(insn) || ASMUtils.isPushDouble(insn))
                .forEach(insn -> {
                    final InsnList insnList = new InsnList();
                    final ValueType valueType = this.getValueType(insn);

                    int type = this.getBozar().getConfig().getOptions().getConstantObfuscation() == BozarConfig.BozarOptions.ConstantObfuscationOption.SUPER
                            ? 2 : random.nextInt(2);

                    switch (valueType) {
                        case INTEGER -> {
                            int value = ASMUtils.getPushedInt(insn);

                            final byte shift = 2;
                            boolean canShift = this.canShiftLeft(shift, value, Integer.MIN_VALUE);
                            if (!canShift && type == 1) type--;

                            switch (type) {
                                case 0 -> {
                                    int xor1 = random.nextInt(Short.MAX_VALUE);
                                    int xor2 = value ^ xor1;
                                    insnList.add(ASMUtils.pushInt(xor1));
                                    insnList.add(ASMUtils.pushInt(xor2));
                                    insnList.add(new InsnNode(IXOR));
                                }
                                case 1 -> {
                                    insnList.add(ASMUtils.pushInt(value << shift));
                                    insnList.add(ASMUtils.pushInt(shift));
                                    insnList.add(new InsnNode(IUSHR));
                                }
                                case 2 -> {
                                    obfuscateIntSuper(insnList, value);
                                }
                            }
                        }
                        case LONG -> {
                            long value = ASMUtils.getPushedLong(insn);
                            final byte shift = 2;
                            boolean canShift = this.canShiftLeft(shift, value, Long.MIN_VALUE);
                            if (!canShift && type == 1) type--;

                            switch (type) {
                                case 0 -> {
                                    int xor1 = random.nextInt(Short.MAX_VALUE);
                                    long xor2 = value ^ xor1;
                                    insnList.add(ASMUtils.pushLong(xor1));
                                    insnList.add(ASMUtils.pushLong(xor2));
                                    insnList.add(new InsnNode(LXOR));
                                }
                                case 1 -> {
                                    insnList.add(ASMUtils.pushLong(value << shift));
                                    insnList.add(ASMUtils.pushInt(shift));
                                    insnList.add(new InsnNode(LUSHR));
                                }
                                case 2 -> {
                                    obfuscateLongSuper(insnList, value);
                                }
                            }
                        }
                        case FLOAT -> {
                            float value = ASMUtils.getPushedFloat(insn);
                            int bits = Float.floatToIntBits(value);

                            int k1 = random.nextInt(Short.MAX_VALUE) + 1;
                            int k2 = random.nextInt(Short.MAX_VALUE) + 1;
                            int add = random.nextInt(1000) + 1;
                            int obfuscated = ((bits ^ k1) + add) ^ k2;
                            insnList.add(ASMUtils.pushInt(obfuscated));
                            insnList.add(ASMUtils.pushInt(k2));
                            insnList.add(new InsnNode(IXOR));
                            insnList.add(ASMUtils.pushInt(add));
                            insnList.add(new InsnNode(ISUB));
                            insnList.add(ASMUtils.pushInt(k1));
                            insnList.add(new InsnNode(IXOR));

                            insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false));
                        }
                        case DOUBLE -> {
                            double value = ASMUtils.getPushedDouble(insn);
                            long bits = Double.doubleToLongBits(value);

                            long k1 = random.nextInt(Short.MAX_VALUE) + 1;
                            long k2 = random.nextInt(Short.MAX_VALUE) + 1;
                            long add = random.nextInt(1000) + 1;
                            long obfuscated = ((bits ^ k1) + add) ^ k2;
                            insnList.add(ASMUtils.pushLong(obfuscated));
                            insnList.add(ASMUtils.pushLong(k2));
                            insnList.add(new InsnNode(LXOR));
                            insnList.add(ASMUtils.pushLong(add));
                            insnList.add(new InsnNode(LSUB));
                            insnList.add(ASMUtils.pushLong(k1));
                            insnList.add(new InsnNode(LXOR));

                            insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false));
                        }
                    }

                    if (this.getBozar().getConfig().getOptions().getConstantObfuscation() == BozarConfig.BozarOptions.ConstantObfuscationOption.FLOW
                            && (valueType == ValueType.INTEGER || valueType == ValueType.LONG)) {
                        final InsnList flow = new InsnList(), afterFlow = new InsnList();
                        final LabelNode label0 = new LabelNode(), label1 = new LabelNode(), label2 = new LabelNode(), label3 = new LabelNode();
                        int index = methodNode.maxLocals + 2;
                        long rand0 = random.nextLong(), rand1 = random.nextLong();
                        while (rand0 == rand1)
                            rand1 = random.nextLong();

                        flow.add(ASMUtils.pushLong(rand0));
                        flow.add(ASMUtils.pushLong(rand1));
                        flow.add(new InsnNode(LCMP));
                        flow.add(new VarInsnNode(ISTORE, index));
                        flow.add(new VarInsnNode(ILOAD, index));
                        flow.add(new JumpInsnNode(IFNE, label0));
                        flow.add(label3);
                        flow.add(switch (valueType) {
                            case INTEGER -> ASMUtils.pushInt(random.nextInt());
                            case LONG -> ASMUtils.pushLong(random.nextLong());
                            default -> throw new IllegalStateException();
                        });
                        flow.add(new JumpInsnNode(GOTO, label1));
                        flow.add(label0);

                        int alwaysNegative = 0;
                        while (alwaysNegative >= 0) alwaysNegative = -random.nextInt(Integer.MAX_VALUE);

                        afterFlow.add(label1);
                        afterFlow.add(new VarInsnNode(ILOAD, index));
                        afterFlow.add(ASMUtils.pushInt(random.nextInt(Integer.MAX_VALUE)));
                        afterFlow.add(new InsnNode(IADD));
                        afterFlow.add(ASMUtils.pushInt(alwaysNegative));
                        afterFlow.add(new JumpInsnNode(IF_ICMPNE, label2));
                        afterFlow.add(switch (valueType) {
                            case INTEGER -> new InsnNode(POP);
                            case LONG -> new InsnNode(POP2);
                            default -> throw new IllegalStateException();
                        });
                        afterFlow.add(new JumpInsnNode(GOTO, label3));
                        afterFlow.add(label2);

                        methodNode.instructions.insertBefore(insn, flow);
                        methodNode.instructions.insert(insn, afterFlow);
                    }

                    methodNode.instructions.insert(insn, insnList);
                    methodNode.instructions.remove(insn);
                });

        Arrays.stream(methodNode.instructions.toArray())
                .filter(ASMUtils::isPushInt)
                .filter(insn -> {
                    int val = ASMUtils.getPushedInt(insn);
                    return val >= 0 && val <= Byte.MAX_VALUE;
                })
                .forEach(insn -> {
                    final InsnList insnList = new InsnList();
                    int value = ASMUtils.getPushedInt(insn);

                    insnList.add(new LdcInsnNode("\0".repeat(value)));
                    insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
                    methodNode.instructions.insert(insn, insnList);
                    methodNode.instructions.remove(insn);
                });
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if (this.getBozar().getConfig().getOptions().getConstantObfuscation() == BozarConfig.BozarOptions.ConstantObfuscationOption.SUPER) {

            StringEncryptionContext ctx = classContexts.computeIfAbsent(classNode.name,
                k -> new StringEncryptionContext(classNode));

            Arrays.stream(methodNode.instructions.toArray())
                    .filter(insn -> insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String)
                    .map(insn -> (LdcInsnNode) insn)
                    .forEach(ldc -> {
                        String str = (String) ldc.cst;
                        int index = ctx.addString(str);

                        InsnList insnList = new InsnList();

                        int encrypted = index ^ ctx.keyOfClass;
                        insnList.add(ASMUtils.pushInt(encrypted >>> 16));
                        insnList.add(ASMUtils.pushInt(encrypted & 0xFFFF));
                        insnList.add(ASMUtils.pushLong(ctx.getKeyLong(index)));
                        insnList.add(new MethodInsnNode(INVOKESTATIC, classNode.name,
                                ctx.decryptMethodName, "(IIJ)Ljava/lang/String;",
                                (classNode.access & ACC_INTERFACE) != 0));

                        methodNode.instructions.insertBefore(ldc, insnList);
                        methodNode.instructions.remove(ldc);
                    });
        } else {

            Arrays.stream(methodNode.instructions.toArray())
                    .filter(insn -> insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String)
                    .map(insn -> (LdcInsnNode) insn)
                    .forEach(ldc -> {
                        methodNode.instructions.insertBefore(ldc, this.convertString(methodNode, (String) ldc.cst));
                        methodNode.instructions.remove(ldc);
                    });
        }

        this.obfuscateNumbers(classNode, methodNode);
    }

    @Override
    public void transformField(ClassNode classNode, FieldNode fieldNode) {

        if (fieldNode.value instanceof String)
            if ((fieldNode.access & ACC_STATIC) != 0)
                this.addDirectInstructions(classNode, ASMUtils.findOrCreateClinit(classNode), fieldNode);
            else
                this.addDirectInstructions(classNode, ASMUtils.findOrCreateInit(classNode), fieldNode);
    }

    @Override
    public void post() {

        if (this.getBozar().getConfig().getOptions().getConstantObfuscation() == BozarConfig.BozarOptions.ConstantObfuscationOption.SUPER) {
            for (StringEncryptionContext ctx : classContexts.values()) {
                if (ctx.strings.isEmpty()) continue;

                ClassNode classNode = ctx.classNode;

                classNode.fields.add(ctx.encryptedField);
                classNode.fields.add(ctx.decryptedField);
                classNode.fields.add(ctx.shuffleField);

                classNode.methods.add(createDecryptMethod(ctx));

                MethodNode clinit = ASMUtils.findOrCreateClinit(classNode);
                clinit.instructions.insert(createInitInstructions(ctx));
            }
        }
        classContexts.clear();
    }

    private MethodNode createDecryptMethod(StringEncryptionContext ctx) {
        MethodNode method = new MethodNode(
                ACC_PRIVATE | ACC_STATIC,
                ctx.decryptMethodName,
                "(IIJ)Ljava/lang/String;",
                null, null
        );

        int varHigh = 0, varLow = 1, varKeyLong = 2;
        int varIndex = 4, varDecrypted = 5, varKeyBytes = 6, varKeyBytesI = 7;
        int varEncrypted = 8, varBuffer = 9, varI = 10, varJ = 11, varXorKey = 12;
        int varDynamicKey = 13, varCharVal = 14, varLowByte = 15;
        int varCallerHash = 16;

        LabelNode startLabel = new LabelNode();
        LabelNode convertLoopLabel = new LabelNode();
        LabelNode convertLoopEndLabel = new LabelNode();
        LabelNode charLoopLabel = new LabelNode();
        LabelNode charLoopEndLabel = new LabelNode();
        LabelNode resetJLabel = new LabelNode();
        LabelNode returnLabel = new LabelNode();
        LabelNode[] switchLabels = new LabelNode[256];
        for (int i = 0; i < 256; i++) switchLabels[i] = new LabelNode();
        LabelNode switchDefaultLabel = switchLabels[255];
        LabelNode afterSwitchLabel = new LabelNode();
        LabelNode antiCopyCheckLabel = new LabelNode();

        InsnList insns = new InsnList();

        insns.add(startLabel);

        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false));
        insns.add(ASMUtils.pushInt(2));
        insns.add(new InsnNode(AALOAD));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false));

        int expectedClassNameHash = ctx.classNode.name.replace('/', '.').hashCode();
        insns.add(ASMUtils.pushInt(expectedClassNameHash));
        insns.add(new InsnNode(IXOR));

        int varTempHash = 17;
        insns.add(new VarInsnNode(ISTORE, varTempHash));

        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode catchHandler = new LabelNode();
        LabelNode afterClassLoaderCheck = new LabelNode();

        insns.add(tryStart);

        insns.add(new LdcInsnNode(org.objectweb.asm.Type.getObjectType(ctx.classNode.name)));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false));

        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false));
        insns.add(ASMUtils.pushInt(2));
        insns.add(new InsnNode(AALOAD));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;", false));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false));

        insns.add(new InsnNode(IXOR));
        insns.add(tryEnd);
        insns.add(new JumpInsnNode(GOTO, afterClassLoaderCheck));

        insns.add(catchHandler);
        insns.add(new InsnNode(POP));
        insns.add(ASMUtils.pushInt(0x12345678));

        insns.add(afterClassLoaderCheck);

        insns.add(new VarInsnNode(ILOAD, varTempHash));

        insns.add(new InsnNode(IXOR));

        method.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, catchHandler, "java/lang/Throwable"));

        insns.add(new VarInsnNode(ISTORE, varCallerHash));

        insns.add(antiCopyCheckLabel);
        insns.add(new VarInsnNode(ILOAD, varHigh));
        insns.add(ASMUtils.pushInt(16));
        insns.add(new InsnNode(ISHL));
        insns.add(new VarInsnNode(ILOAD, varLow));
        insns.add(new InsnNode(IOR));
        insns.add(ASMUtils.pushInt(ctx.keyOfClass));
        insns.add(new InsnNode(IXOR));
        insns.add(new InsnNode(DUP));
        insns.add(new VarInsnNode(ISTORE, varIndex));

        insns.add(new FieldInsnNode(GETSTATIC, ctx.classNode.name, ctx.decryptedField.name, ctx.decryptedField.desc));
        insns.add(new InsnNode(SWAP));
        insns.add(new InsnNode(AALOAD));
        insns.add(new InsnNode(DUP));
        insns.add(new VarInsnNode(ASTORE, varDecrypted));
        insns.add(new JumpInsnNode(IFNONNULL, returnLabel));

        insns.add(new TypeInsnNode(NEW, "java/util/Random"));
        insns.add(new InsnNode(DUP));
        insns.add(ASMUtils.pushLong(ctx.magicSeed));
        insns.add(new VarInsnNode(ILOAD, varIndex));
        insns.add(new InsnNode(I2L));
        insns.add(new InsnNode(LXOR));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/util/Random", "<init>", "(J)V", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Random", "nextInt", "()I", false));
        insns.add(new VarInsnNode(ISTORE, varDynamicKey));

        insns.add(ASMUtils.pushInt(8));
        insns.add(new IntInsnNode(NEWARRAY, T_BYTE));
        insns.add(new VarInsnNode(ASTORE, varKeyBytes));
        insns.add(ASMUtils.pushInt(0));
        insns.add(new VarInsnNode(ISTORE, varKeyBytesI));

        insns.add(convertLoopLabel);
        insns.add(new VarInsnNode(ILOAD, varKeyBytesI));
        insns.add(ASMUtils.pushInt(8));
        insns.add(new JumpInsnNode(IF_ICMPGE, convertLoopEndLabel));
        insns.add(new VarInsnNode(ALOAD, varKeyBytes));
        insns.add(new VarInsnNode(ILOAD, varKeyBytesI));
        insns.add(new VarInsnNode(LLOAD, varKeyLong));
        insns.add(ASMUtils.pushInt(64));
        insns.add(new IincInsnNode(varKeyBytesI, 1));
        insns.add(new VarInsnNode(ILOAD, varKeyBytesI));
        insns.add(ASMUtils.pushInt(8));
        insns.add(new InsnNode(IMUL));
        insns.add(new InsnNode(ISUB));
        insns.add(new InsnNode(LUSHR));
        insns.add(new InsnNode(L2I));
        insns.add(new InsnNode(I2B));
        insns.add(new InsnNode(BASTORE));
        insns.add(new JumpInsnNode(GOTO, convertLoopLabel));
        insns.add(convertLoopEndLabel);

        insns.add(new FieldInsnNode(GETSTATIC, ctx.classNode.name, ctx.encryptedField.name, ctx.encryptedField.desc));
        insns.add(new VarInsnNode(ILOAD, varIndex));
        insns.add(new InsnNode(AALOAD));
        insns.add(new VarInsnNode(ASTORE, varEncrypted));

        insns.add(new VarInsnNode(ALOAD, varEncrypted));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        insns.add(new IntInsnNode(NEWARRAY, T_CHAR));
        insns.add(new VarInsnNode(ASTORE, varBuffer));

        insns.add(ASMUtils.pushInt(0));
        insns.add(new VarInsnNode(ISTORE, varI));
        insns.add(ASMUtils.pushInt(0));
        insns.add(new VarInsnNode(ISTORE, varJ));

        insns.add(charLoopLabel);
        insns.add(new VarInsnNode(ILOAD, varI));
        insns.add(new VarInsnNode(ALOAD, varEncrypted));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        insns.add(new JumpInsnNode(IF_ICMPGE, charLoopEndLabel));

        insns.add(new VarInsnNode(ILOAD, varJ));
        insns.add(ASMUtils.pushInt(8));
        insns.add(new JumpInsnNode(IF_ICMPNE, resetJLabel));
        insns.add(ASMUtils.pushInt(0));
        insns.add(new VarInsnNode(ISTORE, varJ));

        insns.add(resetJLabel);
        insns.add(new VarInsnNode(ILOAD, varI));
        insns.add(new VarInsnNode(ILOAD, varIndex));
        insns.add(new InsnNode(IXOR));
        insns.add(ASMUtils.pushInt(ctx.keyOfClass));
        insns.add(new InsnNode(IXOR));
        insns.add(ASMUtils.pushInt(0xFF));
        insns.add(new InsnNode(IAND));

        int[] keys = new int[255];
        for (int i = 0; i < 255; i++) keys[i] = i;
        LabelNode[] labels = Arrays.copyOf(switchLabels, 255);
        insns.add(new TableSwitchInsnNode(0, 254, switchDefaultLabel, labels));

        for (int i = 0; i < 256; i++) {
            insns.add(switchLabels[i]);
            insns.add(ASMUtils.pushInt(ctx.keyMap[i]));
            insns.add(new VarInsnNode(ISTORE, varXorKey));
            if (i != 255) {
                insns.add(new JumpInsnNode(GOTO, afterSwitchLabel));
            }
        }

        insns.add(afterSwitchLabel);
        insns.add(new VarInsnNode(ALOAD, varBuffer));
        insns.add(new VarInsnNode(ILOAD, varI));

        insns.add(new VarInsnNode(ALOAD, varEncrypted));
        insns.add(new VarInsnNode(ILOAD, varI));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false));
        insns.add(new VarInsnNode(ISTORE, varCharVal));

        insns.add(new FieldInsnNode(GETSTATIC, ctx.classNode.name, ctx.shuffleField.name, ctx.shuffleField.desc));
        insns.add(new VarInsnNode(ILOAD, varCharVal));
        insns.add(ASMUtils.pushInt(0xFF));
        insns.add(new InsnNode(IAND));
        insns.add(new InsnNode(IALOAD));
        insns.add(new VarInsnNode(ISTORE, varLowByte));

        insns.add(new VarInsnNode(ILOAD, varCharVal));
        insns.add(ASMUtils.pushInt(0xFF00));
        insns.add(new InsnNode(IAND));
        insns.add(new VarInsnNode(ILOAD, varLowByte));
        insns.add(new InsnNode(IOR));

        insns.add(new VarInsnNode(ILOAD, varDynamicKey));
        insns.add(new VarInsnNode(ILOAD, varI));
        insns.add(ASMUtils.pushInt(32));
        insns.add(new InsnNode(IREM));
        insns.add(new InsnNode(IUSHR));
        insns.add(ASMUtils.pushInt(0xFF));
        insns.add(new InsnNode(IAND));
        insns.add(new InsnNode(IXOR));

        insns.add(ASMUtils.pushInt(ctx.keyOfClass));
        insns.add(new InsnNode(IXOR));

        insns.add(new VarInsnNode(ILOAD, varXorKey));
        insns.add(new InsnNode(IXOR));

        insns.add(new VarInsnNode(ALOAD, varKeyBytes));
        insns.add(new VarInsnNode(ILOAD, varJ));
        insns.add(new InsnNode(BALOAD));
        insns.add(new InsnNode(IXOR));

        insns.add(new VarInsnNode(ILOAD, varCallerHash));
        insns.add(new InsnNode(IXOR));

        insns.add(new InsnNode(I2C));
        insns.add(new InsnNode(CASTORE));

        insns.add(new IincInsnNode(varI, 1));
        insns.add(new IincInsnNode(varJ, 1));
        insns.add(new JumpInsnNode(GOTO, charLoopLabel));

        insns.add(charLoopEndLabel);
        insns.add(new FieldInsnNode(GETSTATIC, ctx.classNode.name, ctx.decryptedField.name, ctx.decryptedField.desc));
        insns.add(new VarInsnNode(ILOAD, varIndex));
        insns.add(new TypeInsnNode(NEW, "java/lang/String"));
        insns.add(new InsnNode(DUP));
        insns.add(new VarInsnNode(ALOAD, varBuffer));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;", false));
        insns.add(new InsnNode(DUP));
        insns.add(new VarInsnNode(ASTORE, varDecrypted));
        insns.add(new InsnNode(AASTORE));

        insns.add(returnLabel);
        insns.add(new VarInsnNode(ALOAD, varDecrypted));
        insns.add(new InsnNode(ARETURN));

        method.instructions = insns;
        method.maxStack = 12;
        method.maxLocals = 18;

        return method;
    }

    private InsnList createInitInstructions(StringEncryptionContext ctx) {
        InsnList insns = new InsnList();
        insns.add(new LabelNode());

        insns.add(ASMUtils.pushInt(ctx.strings.size()));
        insns.add(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        insns.add(new InsnNode(DUP));

        for (int i = 0; i < ctx.strings.size(); i++) {
            insns.add(ASMUtils.pushInt(i));
            insns.add(new LdcInsnNode(ctx.encryptString(i)));
            insns.add(new InsnNode(AASTORE));
            if (i != ctx.strings.size() - 1) {
                insns.add(new InsnNode(DUP));
            }
        }
        insns.add(new FieldInsnNode(PUTSTATIC, ctx.classNode.name, ctx.encryptedField.name, ctx.encryptedField.desc));

        insns.add(ASMUtils.pushInt(ctx.strings.size()));
        insns.add(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        insns.add(new FieldInsnNode(PUTSTATIC, ctx.classNode.name, ctx.decryptedField.name, ctx.decryptedField.desc));

        insns.add(ASMUtils.pushInt(256));
        insns.add(new IntInsnNode(NEWARRAY, T_INT));
        insns.add(new InsnNode(DUP));
        for (int i = 0; i < 256; i++) {
            insns.add(ASMUtils.pushInt(i));
            insns.add(ASMUtils.pushInt(ctx.reverseShuffleMap[i]));
            insns.add(new InsnNode(IASTORE));
            if (i != 255) {
                insns.add(new InsnNode(DUP));
            }
        }
        insns.add(new FieldInsnNode(PUTSTATIC, ctx.classNode.name, ctx.shuffleField.name, ctx.shuffleField.desc));

        return insns;
    }

    private void addDirectInstructions(ClassNode classNode, MethodNode methodNode, FieldNode fieldNode) {
        final InsnList insnList = new InsnList();
        insnList.add(new LdcInsnNode(fieldNode.value));
        int opcode;
        if ((fieldNode.access & ACC_STATIC) != 0)
            opcode = PUTSTATIC;
        else
            opcode = PUTFIELD;
        insnList.add(new FieldInsnNode(opcode, classNode.name, fieldNode.name, fieldNode.desc));
        methodNode.instructions.insert(insnList);

        fieldNode.value = null;
    }

    private InsnList convertString(MethodNode methodNode, String str) {
        final InsnList insnList = new InsnList();
        final int varIndex = methodNode.maxLocals + 1;

        insnList.add(ASMUtils.pushInt(str.length()));
        insnList.add(new IntInsnNode(NEWARRAY, T_BYTE));
        insnList.add(new VarInsnNode(ASTORE, varIndex));

        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < str.length(); i++) indexes.add(i);
        Collections.shuffle(indexes);

        for (int i = 0; i < str.length(); i++) {
            int index = indexes.get(0);
            indexes.remove(0);
            char ch = str.toCharArray()[index];

            if (i == 0) {
                insnList.add(new VarInsnNode(ALOAD, varIndex));
                insnList.add(ASMUtils.pushInt(index));
                insnList.add(ASMUtils.pushInt((byte) random.nextInt(Character.MAX_VALUE)));
                insnList.add(new InsnNode(BASTORE));
            }

            insnList.add(new VarInsnNode(ALOAD, varIndex));
            insnList.add(ASMUtils.pushInt(index));
            insnList.add(ASMUtils.pushInt(ch));
            insnList.add(new InsnNode(BASTORE));
        }

        insnList.add(new TypeInsnNode(NEW, "java/lang/String"));
        insnList.add(new InsnNode(DUP));
        insnList.add(new VarInsnNode(ALOAD, varIndex));
        insnList.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false));
        return insnList;
    }

    private void obfuscateIntSuper(InsnList insnList, int value) {
        NumberObfType[] types = NumberObfType.values();
        NumberObfType obfType = types[random.nextInt(types.length)];

        switch (obfType) {
            case RANDOM_SEED -> {

                long seed = random.nextLong();
                int randomResult = new java.util.Random(seed).nextInt();
                int obfuscated = value ^ randomResult;

                insnList.add(new TypeInsnNode(NEW, "java/util/Random"));
                insnList.add(new InsnNode(DUP));
                insnList.add(ASMUtils.pushLong(seed));
                insnList.add(new MethodInsnNode(INVOKESPECIAL, "java/util/Random", "<init>", "(J)V", false));
                insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Random", "nextInt", "()I", false));
                insnList.add(ASMUtils.pushInt(obfuscated));
                insnList.add(new InsnNode(IXOR));
            }
            case MULTI_XOR_SHIFT -> {

                int k1 = random.nextInt();
                int k2 = random.nextInt();
                int s1 = random.nextInt(31) + 1;
                int temp = value ^ k1;
                int obfuscated = Integer.rotateLeft(temp, s1) ^ k2;

                insnList.add(ASMUtils.pushInt(obfuscated));
                insnList.add(ASMUtils.pushInt(k2));
                insnList.add(new InsnNode(IXOR));
                insnList.add(ASMUtils.pushInt(s1));
                insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "rotateRight", "(II)I", false));
                insnList.add(ASMUtils.pushInt(k1));
                insnList.add(new InsnNode(IXOR));
            }
            case BITWISE_ROTATE -> {

                int r = random.nextInt(31) + 1;
                int key = random.nextInt();
                int obfuscated = Integer.rotateLeft(value ^ key, r);

                insnList.add(ASMUtils.pushInt(obfuscated));
                insnList.add(ASMUtils.pushInt(r));
                insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "rotateRight", "(II)I", false));
                insnList.add(ASMUtils.pushInt(key));
                insnList.add(new InsnNode(IXOR));
            }
            case HASH_CHAIN -> {

                int k1 = random.nextInt();
                int k2 = random.nextInt();
                int obfuscated = bitwiseHash(value ^ k1) ^ k2;

                insnList.add(ASMUtils.pushInt(obfuscated));
                insnList.add(ASMUtils.pushInt(k2));
                insnList.add(new InsnNode(IXOR));

                insnList.add(new InsnNode(DUP));
                insnList.add(ASMUtils.pushInt(3));
                insnList.add(new InsnNode(IUSHR));
                insnList.add(new InsnNode(SWAP));
                insnList.add(ASMUtils.pushInt(7));
                insnList.add(new InsnNode(IAND));
                insnList.add(ASMUtils.pushInt(29));
                insnList.add(new InsnNode(ISHL));
                insnList.add(new InsnNode(IOR));
                insnList.add(ASMUtils.pushInt(k1));
                insnList.add(new InsnNode(IXOR));
            }
            case MIXED_OPS -> {

                int k1 = random.nextInt();
                int k2 = random.nextInt();
                int add = random.nextInt(10000) + 1;

                int mul = (random.nextInt(1000) * 2) + 1;
                int mulInverse = modInverse(mul);

                int obfuscated = (((value ^ k1) + add) * mul) ^ k2;

                insnList.add(ASMUtils.pushInt(obfuscated));
                insnList.add(ASMUtils.pushInt(k2));
                insnList.add(new InsnNode(IXOR));
                insnList.add(ASMUtils.pushInt(mulInverse));
                insnList.add(new InsnNode(IMUL));
                insnList.add(ASMUtils.pushInt(add));
                insnList.add(new InsnNode(ISUB));
                insnList.add(ASMUtils.pushInt(k1));
                insnList.add(new InsnNode(IXOR));
            }
        }
    }

    private void obfuscateLongSuper(InsnList insnList, long value) {
        NumberObfType[] types = NumberObfType.values();
        NumberObfType obfType = types[random.nextInt(types.length)];

        switch (obfType) {
            case RANDOM_SEED -> {

                long seed = random.nextLong();
                long randomResult = new java.util.Random(seed).nextLong();
                long obfuscated = value ^ randomResult;

                insnList.add(new TypeInsnNode(NEW, "java/util/Random"));
                insnList.add(new InsnNode(DUP));
                insnList.add(ASMUtils.pushLong(seed));
                insnList.add(new MethodInsnNode(INVOKESPECIAL, "java/util/Random", "<init>", "(J)V", false));
                insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Random", "nextLong", "()J", false));
                insnList.add(ASMUtils.pushLong(obfuscated));
                insnList.add(new InsnNode(LXOR));
            }
            case MULTI_XOR_SHIFT, HASH_CHAIN -> {

                long k1 = random.nextLong();
                long k2 = random.nextLong();
                int s1 = random.nextInt(63) + 1;
                long temp = value ^ k1;
                long obfuscated = Long.rotateLeft(temp, s1) ^ k2;

                insnList.add(ASMUtils.pushLong(obfuscated));
                insnList.add(ASMUtils.pushLong(k2));
                insnList.add(new InsnNode(LXOR));
                insnList.add(ASMUtils.pushInt(s1));
                insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "rotateRight", "(JI)J", false));
                insnList.add(ASMUtils.pushLong(k1));
                insnList.add(new InsnNode(LXOR));
            }
            case BITWISE_ROTATE -> {

                int r = random.nextInt(63) + 1;
                long key = random.nextLong();
                long obfuscated = Long.rotateLeft(value ^ key, r);

                insnList.add(ASMUtils.pushLong(obfuscated));
                insnList.add(ASMUtils.pushInt(r));
                insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "rotateRight", "(JI)J", false));
                insnList.add(ASMUtils.pushLong(key));
                insnList.add(new InsnNode(LXOR));
            }
            case MIXED_OPS -> {

                long k1 = random.nextLong();
                long k2 = random.nextLong();
                long add = random.nextInt(10000) + 1;
                long obfuscated = ((value ^ k1) + add) ^ k2;

                insnList.add(ASMUtils.pushLong(obfuscated));
                insnList.add(ASMUtils.pushLong(k2));
                insnList.add(new InsnNode(LXOR));
                insnList.add(ASMUtils.pushLong(add));
                insnList.add(new InsnNode(LSUB));
                insnList.add(ASMUtils.pushLong(k1));
                insnList.add(new InsnNode(LXOR));
            }
        }
    }

    private int bitwiseHash(int value) {
        return ((value & (7 << 29)) >>> 29) | (value << 3);
    }

    private int modInverse(int a) {
        int x = a;
        for (int i = 0; i < 5; i++) {
            x = x * (2 - a * x);
        }
        return x;
    }

    private boolean canShiftLeft(byte shift, long value, final long minValue) {
        int power = (int) (Math.log(-(minValue >> 1)) / Math.log(2)) + 1;
        return IntStream.range(0, shift).allMatch(i -> (value >> power - i) == 0);
    }

    private enum ValueType {
        INTEGER, LONG, FLOAT, DOUBLE
    }

    private ValueType getValueType(AbstractInsnNode insn) {
        if (ASMUtils.isPushInt(insn)) return ValueType.INTEGER;
        else if (ASMUtils.isPushLong(insn)) return ValueType.LONG;
        else if (ASMUtils.isPushFloat(insn)) return ValueType.FLOAT;
        else if (ASMUtils.isPushDouble(insn)) return ValueType.DOUBLE;
        throw new IllegalArgumentException("Insn is not a push int/long/float/double instruction");
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> ((List<?>) this.getEnableType().type()).contains(this.getBozar().getConfig().getOptions().getConstantObfuscation()),
                List.of(BozarConfig.BozarOptions.ConstantObfuscationOption.LIGHT, BozarConfig.BozarOptions.ConstantObfuscationOption.FLOW, BozarConfig.BozarOptions.ConstantObfuscationOption.SUPER));
    }

    private static class StringEncryptionContext {
        final ClassNode classNode;
        final int keyOfClass;
        final int[] keyMap = new int[256];
        final int[] shuffleMap = new int[256];
        final int[] reverseShuffleMap = new int[256];
        final List<String> strings = new ArrayList<>();
        final Map<String, Integer> stringIndex = new HashMap<>();
        final List<byte[]> keyArrays = new ArrayList<>();
        final FieldNode encryptedField;
        final FieldNode decryptedField;
        final FieldNode shuffleField;
        final String decryptMethodName;
        final long magicSeed;

        StringEncryptionContext(ClassNode classNode) {
            this.classNode = classNode;
            this.keyOfClass = ThreadLocalRandom.current().nextInt(0xFFFFFF, Integer.MAX_VALUE);
            this.magicSeed = ThreadLocalRandom.current().nextLong();

            for (int i = 0; i < 256; i++) {
                do {
                    keyMap[i] = ThreadLocalRandom.current().nextInt();
                } while (keyMap[i] == 0);
            }

            for (int i = 0; i < 256; i++) {
                shuffleMap[i] = i;
            }
            for (int i = 255; i > 0; i--) {
                int j = ThreadLocalRandom.current().nextInt(i + 1);
                int temp = shuffleMap[i];
                shuffleMap[i] = shuffleMap[j];
                shuffleMap[j] = temp;
            }

            for (int i = 0; i < 256; i++) {
                reverseShuffleMap[shuffleMap[i]] = i;
            }

            String baseName = generateRandomName();
            this.encryptedField = new FieldNode(
                    ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                    baseName + "E",
                    "[Ljava/lang/String;",
                    null, null
            );
            this.decryptedField = new FieldNode(
                    ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                    baseName + "D",
                    "[Ljava/lang/String;",
                    null, null
            );
            this.shuffleField = new FieldNode(
                    ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                    baseName + "S",
                    "[I",
                    null, null
            );
            this.decryptMethodName = baseName + "X";
        }

        int addString(String s) {
            return stringIndex.computeIfAbsent(s, str -> {
                int idx = strings.size();
                strings.add(str);

                byte[] key = new byte[8];
                do {
                    ThreadLocalRandom.current().nextBytes(key);
                } while (allZero(key));
                keyArrays.add(key);
                return idx;
            });
        }

        long getKeyLong(int index) {
            byte[] key = keyArrays.get(index);
            return ((key[0] & 0xFFL) << 56) |
                   ((key[1] & 0xFFL) << 48) |
                   ((key[2] & 0xFFL) << 40) |
                   ((key[3] & 0xFFL) << 32) |
                   ((key[4] & 0xFFL) << 24) |
                   ((key[5] & 0xFFL) << 16) |
                   ((key[6] & 0xFFL) << 8) |
                   (key[7] & 0xFFL);
        }

        String encryptString(int index) {
            String plaintext = strings.get(index);
            byte[] key = keyArrays.get(index);
            char[] result = new char[plaintext.length()];

            java.util.Random seedRandom = new java.util.Random(magicSeed ^ index);
            int dynamicKey = seedRandom.nextInt();

            int j = 0;
            for (int i = 0; i < plaintext.length(); i++) {
                if (j == 8) j = 0;
                int xorKey = keyMap[(i ^ index ^ keyOfClass) & 0xFF];

                int encrypted = plaintext.charAt(i) ^ keyOfClass ^ xorKey ^ key[j] ^ ((dynamicKey >>> (i % 32)) & 0xFF);

                int lowByte = encrypted & 0xFF;
                int highBits = encrypted & 0xFF00;
                result[i] = (char) (highBits | shuffleMap[lowByte]);
                j++;
            }

            return new String(result);
        }

        private boolean allZero(byte[] arr) {
            for (byte b : arr) if (b != 0) return false;
            return true;
        }

        private String generateRandomName() {

            String[] illegalPrefixes = {
                "goto",
                "const",
                "null",
                "true",
                "false",
                "class",
                "void",
                "if",
                "for",
                "while",
                "\u200b",
                "\u200c",
                "\u200d",
                "\ufeff",
                " ",
                "\t",
                "\n",
                "1start",
            };

            StringBuilder sb = new StringBuilder();

            sb.append(illegalPrefixes[ThreadLocalRandom.current().nextInt(illegalPrefixes.length)]);

            String chars = "abcdefghijklmnopqrstuvwxyz\u200b\u200c\u200d";
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            return sb.toString();
        }
    }
}
