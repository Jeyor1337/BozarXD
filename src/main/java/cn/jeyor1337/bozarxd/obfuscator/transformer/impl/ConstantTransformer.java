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

    // Per-class string encryption context (for SKIDOBF mode)
    private final Map<String, StringEncryptionContext> classContexts = new HashMap<>();

    public ConstantTransformer(Bozar bozar) {
        super(bozar, "Constant obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void pre() {
        classContexts.clear();
    }

    private void obfuscateNumbers(ClassNode classNode, MethodNode methodNode) {
        Arrays.stream(methodNode.instructions.toArray())
                .filter(insn -> ASMUtils.isPushInt(insn) || ASMUtils.isPushLong(insn)
                        || ASMUtils.isPushFloat(insn) || ASMUtils.isPushDouble(insn))
                .forEach(insn -> {
                    final InsnList insnList = new InsnList();
                    final ValueType valueType = this.getValueType(insn);

                    // Randomly selected number obfuscation type
                    int type = this.getBozar().getConfig().getOptions().getConstantObfuscation() == BozarConfig.BozarOptions.ConstantObfuscationOption.SUPER
                            ? 2 : random.nextInt(2);

                    switch (valueType) {
                        case INTEGER -> {
                            int value = ASMUtils.getPushedInt(insn);
                            // Bounds check for shift
                            final byte shift = 2;
                            boolean canShift = this.canShiftLeft(shift, value, Integer.MIN_VALUE);
                            if (!canShift && type == 1) type--;

                            switch (type) {
                                case 0 -> { // XOR
                                    int xor1 = random.nextInt(Short.MAX_VALUE);
                                    int xor2 = value ^ xor1;
                                    insnList.add(ASMUtils.pushInt(xor1));
                                    insnList.add(ASMUtils.pushInt(xor2));
                                    insnList.add(new InsnNode(IXOR));
                                }
                                case 1 -> { // Shift
                                    insnList.add(ASMUtils.pushInt(value << shift));
                                    insnList.add(ASMUtils.pushInt(shift));
                                    insnList.add(new InsnNode(IUSHR));
                                }
                                case 2 -> { // Triple XOR+ADD (SKIDOBF)
                                    int k1 = random.nextInt(Short.MAX_VALUE) + 1;
                                    int k2 = random.nextInt(Short.MAX_VALUE) + 1;
                                    int add = random.nextInt(1000) + 1;
                                    int obfuscated = ((value ^ k1) + add) ^ k2;
                                    insnList.add(ASMUtils.pushInt(obfuscated));
                                    insnList.add(ASMUtils.pushInt(k2));
                                    insnList.add(new InsnNode(IXOR));
                                    insnList.add(ASMUtils.pushInt(add));
                                    insnList.add(new InsnNode(ISUB));
                                    insnList.add(ASMUtils.pushInt(k1));
                                    insnList.add(new InsnNode(IXOR));
                                }
                            }
                        }
                        case LONG -> {
                            long value = ASMUtils.getPushedLong(insn);
                            final byte shift = 2;
                            boolean canShift = this.canShiftLeft(shift, value, Long.MIN_VALUE);
                            if (!canShift && type == 1) type--;

                            switch (type) {
                                case 0 -> { // XOR
                                    int xor1 = random.nextInt(Short.MAX_VALUE);
                                    long xor2 = value ^ xor1;
                                    insnList.add(ASMUtils.pushLong(xor1));
                                    insnList.add(ASMUtils.pushLong(xor2));
                                    insnList.add(new InsnNode(LXOR));
                                }
                                case 1 -> { // Shift
                                    insnList.add(ASMUtils.pushLong(value << shift));
                                    insnList.add(ASMUtils.pushInt(shift));
                                    insnList.add(new InsnNode(LUSHR));
                                }
                                case 2 -> { // Triple XOR+ADD (SKIDOBF)
                                    long k1 = random.nextInt(Short.MAX_VALUE) + 1;
                                    long k2 = random.nextInt(Short.MAX_VALUE) + 1;
                                    long add = random.nextInt(1000) + 1;
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
                        case FLOAT -> {
                            float value = ASMUtils.getPushedFloat(insn);
                            int bits = Float.floatToIntBits(value);
                            // Use triple XOR+ADD on the int bits
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
                            // Convert back to float
                            insnList.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false));
                        }
                        case DOUBLE -> {
                            double value = ASMUtils.getPushedDouble(insn);
                            long bits = Double.doubleToLongBits(value);
                            // Use triple XOR+ADD on the long bits
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
                            // Convert back to double
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

                    // Replace number instruction with our instructions
                    methodNode.instructions.insert(insn, insnList);
                    methodNode.instructions.remove(insn);
                });

        // Replace numbers between 0 - Byte.MAX_VALUE with "".length()
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
            // SUPER mode: collect strings for decrypt method architecture
            StringEncryptionContext ctx = classContexts.computeIfAbsent(classNode.name,
                k -> new StringEncryptionContext(classNode));

            Arrays.stream(methodNode.instructions.toArray())
                    .filter(insn -> insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String)
                    .map(insn -> (LdcInsnNode) insn)
                    .forEach(ldc -> {
                        String str = (String) ldc.cst;
                        int index = ctx.addString(str);

                        // Replace with decrypt method call
                        InsnList insnList = new InsnList();
                        // Pass (index ^ keyOfClass) split into high/low parts
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
            // LIGHT/FLOW mode: inline string obfuscation
            Arrays.stream(methodNode.instructions.toArray())
                    .filter(insn -> insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String)
                    .map(insn -> (LdcInsnNode) insn)
                    .forEach(ldc -> {
                        methodNode.instructions.insertBefore(ldc, this.convertString(methodNode, (String) ldc.cst));
                        methodNode.instructions.remove(ldc);
                    });
        }

        // Number obfuscation
        this.obfuscateNumbers(classNode, methodNode);
    }

    @Override
    public void transformField(ClassNode classNode, FieldNode fieldNode) {
        // Move field strings to initializer methods so we can obfuscate
        if (fieldNode.value instanceof String)
            if ((fieldNode.access & ACC_STATIC) != 0)
                this.addDirectInstructions(classNode, ASMUtils.findOrCreateClinit(classNode), fieldNode);
            else
                this.addDirectInstructions(classNode, ASMUtils.findOrCreateInit(classNode), fieldNode);
    }

    @Override
    public void post() {
        // Generate decrypt methods and initialize encrypted string arrays for SUPER mode
        if (this.getBozar().getConfig().getOptions().getConstantObfuscation() == BozarConfig.BozarOptions.ConstantObfuscationOption.SUPER) {
            for (StringEncryptionContext ctx : classContexts.values()) {
                if (ctx.strings.isEmpty()) continue;

                ClassNode classNode = ctx.classNode;

                // Add fields
                classNode.fields.add(ctx.encryptedField);
                classNode.fields.add(ctx.decryptedField);

                // Add decrypt method
                classNode.methods.add(createDecryptMethod(ctx));

                // Initialize in clinit
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

        // Variables: 0=high, 1=low, 2-3=keyLong, 4=index, 5=decrypted, 6=keyBytes, 7=keyBytesI,
        // 8=encrypted, 9=buffer, 10=i, 11=j, 12=xorKey
        int varHigh = 0, varLow = 1, varKeyLong = 2;
        int varIndex = 4, varDecrypted = 5, varKeyBytes = 6, varKeyBytesI = 7;
        int varEncrypted = 8, varBuffer = 9, varI = 10, varJ = 11, varXorKey = 12;

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

        InsnList insns = new InsnList();

        // Decrypt index: index = ((high << 16) | low) ^ keyOfClass
        insns.add(startLabel);
        insns.add(new VarInsnNode(ILOAD, varHigh));
        insns.add(ASMUtils.pushInt(16));
        insns.add(new InsnNode(ISHL));
        insns.add(new VarInsnNode(ILOAD, varLow));
        insns.add(new InsnNode(IOR));
        insns.add(ASMUtils.pushInt(ctx.keyOfClass));
        insns.add(new InsnNode(IXOR));
        insns.add(new InsnNode(DUP));
        insns.add(new VarInsnNode(ISTORE, varIndex));

        // Check cache: if (decrypted[index] != null) return it
        insns.add(new FieldInsnNode(GETSTATIC, ctx.classNode.name, ctx.decryptedField.name, ctx.decryptedField.desc));
        insns.add(new InsnNode(SWAP));
        insns.add(new InsnNode(AALOAD));
        insns.add(new InsnNode(DUP));
        insns.add(new VarInsnNode(ASTORE, varDecrypted));
        insns.add(new JumpInsnNode(IFNONNULL, returnLabel));

        // Convert keyLong to byte[8]
        insns.add(ASMUtils.pushInt(8));
        insns.add(new IntInsnNode(NEWARRAY, T_BYTE));
        insns.add(new VarInsnNode(ASTORE, varKeyBytes));
        insns.add(ASMUtils.pushInt(0));
        insns.add(new VarInsnNode(ISTORE, varKeyBytesI));

        // Loop: for (i = 0; i < 8; i++) keyBytes[i] = (byte)(keyLong >>> (64 - (i+1)*8))
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

        // Get encrypted string
        insns.add(new FieldInsnNode(GETSTATIC, ctx.classNode.name, ctx.encryptedField.name, ctx.encryptedField.desc));
        insns.add(new VarInsnNode(ILOAD, varIndex));
        insns.add(new InsnNode(AALOAD));
        insns.add(new VarInsnNode(ASTORE, varEncrypted));

        // char[] buffer = new char[encrypted.length()]
        insns.add(new VarInsnNode(ALOAD, varEncrypted));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        insns.add(new IntInsnNode(NEWARRAY, T_CHAR));
        insns.add(new VarInsnNode(ASTORE, varBuffer));

        // i = 0; j = 0
        insns.add(ASMUtils.pushInt(0));
        insns.add(new VarInsnNode(ISTORE, varI));
        insns.add(ASMUtils.pushInt(0));
        insns.add(new VarInsnNode(ISTORE, varJ));

        // Main decryption loop
        insns.add(charLoopLabel);
        insns.add(new VarInsnNode(ILOAD, varI));
        insns.add(new VarInsnNode(ALOAD, varEncrypted));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
        insns.add(new JumpInsnNode(IF_ICMPGE, charLoopEndLabel));

        // if (j == 8) j = 0
        insns.add(new VarInsnNode(ILOAD, varJ));
        insns.add(ASMUtils.pushInt(8));
        insns.add(new JumpInsnNode(IF_ICMPNE, resetJLabel));
        insns.add(ASMUtils.pushInt(0));
        insns.add(new VarInsnNode(ISTORE, varJ));

        // xorKey = keyMap[(i ^ index ^ keyOfClass) & 0xFF] via tableswitch
        insns.add(resetJLabel);
        insns.add(new VarInsnNode(ILOAD, varI));
        insns.add(new VarInsnNode(ILOAD, varIndex));
        insns.add(new InsnNode(IXOR));
        insns.add(ASMUtils.pushInt(ctx.keyOfClass));
        insns.add(new InsnNode(IXOR));
        insns.add(ASMUtils.pushInt(0xFF));
        insns.add(new InsnNode(IAND));

        // Build switch table
        int[] keys = new int[255];
        for (int i = 0; i < 255; i++) keys[i] = i;
        LabelNode[] labels = Arrays.copyOf(switchLabels, 255);
        insns.add(new TableSwitchInsnNode(0, 254, switchDefaultLabel, labels));

        // Switch cases
        for (int i = 0; i < 256; i++) {
            insns.add(switchLabels[i]);
            insns.add(ASMUtils.pushInt(ctx.keyMap[i]));
            insns.add(new VarInsnNode(ISTORE, varXorKey));
            if (i != 255) {
                insns.add(new JumpInsnNode(GOTO, afterSwitchLabel));
            }
        }

        // Decrypt core: buffer[i] = (char)((encrypted.charAt(i) ^ keyOfClass ^ xorKey ^ keyBytes[j]))
        insns.add(afterSwitchLabel);
        insns.add(new VarInsnNode(ALOAD, varBuffer));
        insns.add(new VarInsnNode(ILOAD, varI));
        insns.add(new VarInsnNode(ALOAD, varEncrypted));
        insns.add(new VarInsnNode(ILOAD, varI));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false));
        insns.add(ASMUtils.pushInt(ctx.keyOfClass));
        insns.add(new InsnNode(IXOR));
        insns.add(new VarInsnNode(ILOAD, varXorKey));
        insns.add(new InsnNode(IXOR));
        insns.add(new VarInsnNode(ALOAD, varKeyBytes));
        insns.add(new VarInsnNode(ILOAD, varJ));
        insns.add(new InsnNode(BALOAD));
        insns.add(new InsnNode(IXOR));
        insns.add(new InsnNode(I2C));
        insns.add(new InsnNode(CASTORE));

        // i++; j++
        insns.add(new IincInsnNode(varI, 1));
        insns.add(new IincInsnNode(varJ, 1));
        insns.add(new JumpInsnNode(GOTO, charLoopLabel));

        // Store decrypted string and return
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
        method.maxStack = 10;
        method.maxLocals = 13;

        return method;
    }

    private InsnList createInitInstructions(StringEncryptionContext ctx) {
        InsnList insns = new InsnList();
        insns.add(new LabelNode());

        // Create encrypted strings array
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

        // Create decrypted cache array
        insns.add(ASMUtils.pushInt(ctx.strings.size()));
        insns.add(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        insns.add(new FieldInsnNode(PUTSTATIC, ctx.classNode.name, ctx.decryptedField.name, ctx.decryptedField.desc));

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

    // Inner class for per-class string encryption context
    private static class StringEncryptionContext {
        final ClassNode classNode;
        final int keyOfClass;
        final int[] keyMap = new int[256];
        final List<String> strings = new ArrayList<>();
        final Map<String, Integer> stringIndex = new HashMap<>();
        final List<byte[]> keyArrays = new ArrayList<>();
        final FieldNode encryptedField;
        final FieldNode decryptedField;
        final String decryptMethodName;

        StringEncryptionContext(ClassNode classNode) {
            this.classNode = classNode;
            this.keyOfClass = ThreadLocalRandom.current().nextInt(0xFFFFFF, Integer.MAX_VALUE);
            for (int i = 0; i < 256; i++) {
                do {
                    keyMap[i] = ThreadLocalRandom.current().nextInt();
                } while (keyMap[i] == 0);
            }

            // Generate unique field/method names
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
            this.decryptMethodName = baseName + "X";
        }

        int addString(String s) {
            return stringIndex.computeIfAbsent(s, str -> {
                int idx = strings.size();
                strings.add(str);
                // Generate 8-byte key for this string
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

            int j = 0;
            for (int i = 0; i < plaintext.length(); i++) {
                if (j == 8) j = 0;
                int xorKey = keyMap[(i ^ index ^ keyOfClass) & 0xFF];
                result[i] = (char) (plaintext.charAt(i) ^ keyOfClass ^ xorKey ^ key[j]);
                j++;
            }

            return new String(result);
        }

        private boolean allZero(byte[] arr) {
            for (byte b : arr) if (b != 0) return false;
            return true;
        }

        private String generateRandomName() {
            StringBuilder sb = new StringBuilder();
            String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            return sb.toString();
        }
    }
}
