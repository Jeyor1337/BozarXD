package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ParamObfTransformer extends ClassTransformer {

    private static final int MIN_EXTRA_PARAMS = 2;
    private static final int MAX_EXTRA_PARAMS = 4;

    private static class ParamEntry {
        final Type type;
        final int originalIndex;
        final int magicValue;

        ParamEntry(Type type, int originalIndex) {
            this.type = type;
            this.originalIndex = originalIndex;
            this.magicValue = 0;
        }

        ParamEntry(Type type, int magicValue, boolean isFake) {
            this.type = type;
            this.originalIndex = -1;
            this.magicValue = magicValue;
        }

        boolean isFake() {
            return originalIndex < 0;
        }
    }

    private static class MethodObfData {
        final String obfMethodName;
        final String obfMethodDesc;
        final List<ParamEntry> shuffledParams;
        final int[] originalToShuffledIndex;
        final int expectedXor;
        final boolean isStatic;

        MethodObfData(String obfMethodName, String obfMethodDesc, List<ParamEntry> shuffledParams,
                      int[] originalToShuffledIndex, int expectedXor, boolean isStatic) {
            this.obfMethodName = obfMethodName;
            this.obfMethodDesc = obfMethodDesc;
            this.shuffledParams = shuffledParams;
            this.originalToShuffledIndex = originalToShuffledIndex;
            this.expectedXor = expectedXor;
            this.isStatic = isStatic;
        }
    }

    private final Map<String, Map<String, MethodObfData>> classMethodMap = new HashMap<>();

    private final Map<String, List<MethodNode>> methodsToProcess = new HashMap<>();

    public ParamObfTransformer(Bozar bozar) {
        super(bozar, "Parameter Obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void pre() {

        for (ClassNode classNode : this.getBozar().getClasses()) {
            Map<String, MethodObfData> methodMap = new HashMap<>();

            for (MethodNode methodNode : classNode.methods) {
                if (shouldObfuscate(classNode, methodNode)) {
                    String methodKey = methodNode.name + methodNode.desc;
                    MethodObfData obfData = createObfuscationData(classNode, methodNode);
                    methodMap.put(methodKey, obfData);

                    this.getBozar().log("Preparing param obfuscation for %s.%s%s -> %s%s (shuffled)",
                        classNode.name, methodNode.name, methodNode.desc,
                        obfData.obfMethodName, obfData.obfMethodDesc);
                }
            }

            if (!methodMap.isEmpty()) {
                classMethodMap.put(classNode.name, methodMap);
            }
        }
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        Map<String, MethodObfData> methodMap = classMethodMap.get(classNode.name);
        if (methodMap == null) {

            updateMethodCalls(classNode, methodNode);
            return;
        }

        String methodKey = methodNode.name + methodNode.desc;
        MethodObfData obfData = methodMap.get(methodKey);

        if (obfData != null) {

            methodsToProcess.computeIfAbsent(classNode.name, k -> new ArrayList<>()).add(methodNode);
        }

        updateMethodCalls(classNode, methodNode);
    }

    @Override
    public void post() {

        for (ClassNode classNode : this.getBozar().getClasses()) {
            List<MethodNode> methods = methodsToProcess.get(classNode.name);
            if (methods == null) continue;

            Map<String, MethodObfData> methodMap = classMethodMap.get(classNode.name);
            if (methodMap == null) continue;

            List<MethodNode> newMethods = new ArrayList<>();

            for (MethodNode originalMethod : methods) {
                String methodKey = originalMethod.name + originalMethod.desc;
                MethodObfData obfData = methodMap.get(methodKey);
                if (obfData == null) continue;

                MethodNode obfMethod = createObfuscatedMethod(classNode, originalMethod, obfData);
                newMethods.add(obfMethod);

                convertToBridge(classNode, originalMethod, obfData);

                this.getBozar().log("Created bridge method %s.%s -> %s",
                    classNode.name, originalMethod.name, obfData.obfMethodName);
            }

            classNode.methods.addAll(newMethods);
        }
    }

    private void updateMethodCalls(ClassNode classNode, MethodNode methodNode) {
        AbstractInsnNode[] instructions = methodNode.instructions.toArray();

        for (AbstractInsnNode insn : instructions) {
            if (!(insn instanceof MethodInsnNode)) continue;

            MethodInsnNode methodInsn = (MethodInsnNode) insn;
            Map<String, MethodObfData> targetMethodMap = classMethodMap.get(methodInsn.owner);
            if (targetMethodMap == null) continue;

            String callKey = methodInsn.name + methodInsn.desc;
            MethodObfData obfData = targetMethodMap.get(callKey);
            if (obfData == null) continue;

            if (isInternalCall(classNode, methodNode, methodInsn)) {

                reorderCallArguments(methodNode, methodInsn, obfData);
                methodInsn.name = obfData.obfMethodName;
                methodInsn.desc = obfData.obfMethodDesc;
            }

        }
    }

    private void reorderCallArguments(MethodNode callerMethod, MethodInsnNode callInsn, MethodObfData obfData) {

        Type[] originalArgs = Type.getArgumentTypes(callInsn.desc);
        int originalArgCount = originalArgs.length;

        if (originalArgCount == 0) {

            InsnList insertList = new InsnList();
            for (ParamEntry entry : obfData.shuffledParams) {
                if (entry.isFake()) {
                    insertList.add(pushIntValue(entry.magicValue));
                    if (entry.type.getSort() == Type.LONG) {
                        insertList.add(new InsnNode(I2L));
                    }
                }
            }
            callerMethod.instructions.insertBefore(callInsn, insertList);
            return;
        }

        InsnList preInsns = new InsnList();
        InsnList pushInsns = new InsnList();

        int tempVarBase = callerMethod.maxLocals;

        int[] originalArgOffsets = new int[originalArgCount];
        int tempOffset = 0;
        for (int i = 0; i < originalArgCount; i++) {
            originalArgOffsets[i] = tempOffset;
            tempOffset += originalArgs[i].getSize();
        }

        for (int i = originalArgCount - 1; i >= 0; i--) {
            Type argType = originalArgs[i];
            int storeIndex = tempVarBase + originalArgOffsets[i];
            preInsns.add(new VarInsnNode(argType.getOpcode(ISTORE), storeIndex));
        }

        for (ParamEntry entry : obfData.shuffledParams) {
            if (entry.isFake()) {

                pushInsns.add(pushIntValue(entry.magicValue));
                if (entry.type.getSort() == Type.LONG) {
                    pushInsns.add(new InsnNode(I2L));
                }
            } else {

                int loadIndex = tempVarBase + originalArgOffsets[entry.originalIndex];
                pushInsns.add(new VarInsnNode(entry.type.getOpcode(ILOAD), loadIndex));
            }
        }

        callerMethod.instructions.insertBefore(callInsn, preInsns);
        callerMethod.instructions.insertBefore(callInsn, pushInsns);

        int tempVarsNeeded = tempOffset;
        if (callerMethod.maxLocals < tempVarBase + tempVarsNeeded) {
            callerMethod.maxLocals = tempVarBase + tempVarsNeeded;
        }
    }

    private boolean isInternalCall(ClassNode classNode, MethodNode callerMethod, MethodInsnNode callInsn) {

        if (callInsn.owner.equals(classNode.name)) {
            return true;
        }

        return classMethodMap.containsKey(callInsn.owner);
    }

    private MethodObfData createObfuscationData(ClassNode classNode, MethodNode methodNode) {

        String obfMethodName = generateObfMethodName(classNode, methodNode.name);
        boolean isStatic = (methodNode.access & ACC_STATIC) != 0;

        Type[] originalArgs = Type.getArgumentTypes(methodNode.desc);
        int originalArgCount = originalArgs.length;

        int extraParamCount = MIN_EXTRA_PARAMS + random.nextInt(MAX_EXTRA_PARAMS - MIN_EXTRA_PARAMS + 1);

        List<ParamEntry> allParams = new ArrayList<>();

        for (int i = 0; i < originalArgCount; i++) {
            allParams.add(new ParamEntry(originalArgs[i], i));
        }

        int xorResult = 0;
        for (int i = 0; i < extraParamCount; i++) {
            Type type = random.nextBoolean() ? Type.INT_TYPE : Type.LONG_TYPE;
            int magicValue = random.nextInt(Integer.MAX_VALUE - 1000) + 1000;
            allParams.add(new ParamEntry(type, magicValue, true));
            xorResult ^= magicValue;
        }

        List<ParamEntry> shuffledParams = new ArrayList<>(allParams);
        Collections.shuffle(shuffledParams, random);

        int[] originalToShuffledIndex = new int[originalArgCount];
        for (int shuffledIdx = 0; shuffledIdx < shuffledParams.size(); shuffledIdx++) {
            ParamEntry entry = shuffledParams.get(shuffledIdx);
            if (!entry.isFake()) {
                originalToShuffledIndex[entry.originalIndex] = shuffledIdx;
            }
        }

        String obfMethodDesc = buildShuffledDescriptor(shuffledParams, Type.getReturnType(methodNode.desc));

        return new MethodObfData(obfMethodName, obfMethodDesc, shuffledParams,
                originalToShuffledIndex, xorResult, isStatic);
    }

    private String generateObfMethodName(ClassNode classNode, String originalName) {
        String baseName = originalName + "$" + Integer.toHexString(random.nextInt(0xFFFF));

        Set<String> existingNames = new HashSet<>();
        for (MethodNode m : classNode.methods) {
            existingNames.add(m.name);
        }

        String name = baseName;
        int counter = 0;
        while (existingNames.contains(name)) {
            name = baseName + "_" + counter++;
        }

        return name;
    }

    private String buildShuffledDescriptor(List<ParamEntry> shuffledParams, Type returnType) {
        StringBuilder sb = new StringBuilder("(");
        for (ParamEntry entry : shuffledParams) {
            sb.append(entry.type.getDescriptor());
        }
        sb.append(")").append(returnType.getDescriptor());
        return sb.toString();
    }

    private MethodNode createObfuscatedMethod(ClassNode classNode, MethodNode originalMethod, MethodObfData obfData) {

        MethodNode obfMethod = new MethodNode(
            ACC_PRIVATE | ACC_SYNTHETIC | (originalMethod.access & ACC_STATIC),
            obfData.obfMethodName,
            obfData.obfMethodDesc,
            null,
            originalMethod.exceptions != null ? originalMethod.exceptions.toArray(new String[0]) : null
        );

        Type[] originalArgs = Type.getArgumentTypes(originalMethod.desc);
        int thisOffset = obfData.isStatic ? 0 : 1;

        int[] originalParamVarIndex = new int[originalArgs.length];
        int varIdx = thisOffset;
        for (int i = 0; i < originalArgs.length; i++) {
            originalParamVarIndex[i] = varIdx;
            varIdx += originalArgs[i].getSize();
        }
        int originalLocalsStart = varIdx;

        int[] newParamVarIndex = new int[obfData.shuffledParams.size()];
        varIdx = thisOffset;
        for (int i = 0; i < obfData.shuffledParams.size(); i++) {
            newParamVarIndex[i] = varIdx;
            varIdx += obfData.shuffledParams.get(i).type.getSize();
        }
        int newLocalsStart = varIdx;

        Map<Integer, Integer> varIndexMap = new HashMap<>();

        if (!obfData.isStatic) {
            varIndexMap.put(0, 0);
        }

        for (int origIdx = 0; origIdx < originalArgs.length; origIdx++) {
            int shuffledIdx = obfData.originalToShuffledIndex[origIdx];
            int oldVarIdx = originalParamVarIndex[origIdx];
            int newVarIdx = newParamVarIndex[shuffledIdx];
            varIndexMap.put(oldVarIdx, newVarIdx);

            if (originalArgs[origIdx].getSize() == 2) {
                varIndexMap.put(oldVarIdx + 1, newVarIdx + 1);
            }
        }

        int localsShift = newLocalsStart - originalLocalsStart;

        InsnList newInsns = new InsnList();

        newInsns.add(generateOpaquePredicate(obfData, newParamVarIndex));

        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : originalMethod.instructions) {
            if (insn instanceof LabelNode) {
                labelMap.put((LabelNode) insn, new LabelNode());
            }
        }

        for (AbstractInsnNode insn : originalMethod.instructions) {
            AbstractInsnNode cloned = insn.clone(labelMap);

            if (cloned instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) cloned;
                if (varIndexMap.containsKey(varInsn.var)) {
                    varInsn.var = varIndexMap.get(varInsn.var);
                } else if (varInsn.var >= originalLocalsStart) {

                    varInsn.var += localsShift;
                }
            } else if (cloned instanceof IincInsnNode) {
                IincInsnNode iincInsn = (IincInsnNode) cloned;
                if (varIndexMap.containsKey(iincInsn.var)) {
                    iincInsn.var = varIndexMap.get(iincInsn.var);
                } else if (iincInsn.var >= originalLocalsStart) {
                    iincInsn.var += localsShift;
                }
            }

            newInsns.add(cloned);
        }

        obfMethod.instructions = newInsns;

        int fakeParamsSize = 0;
        for (ParamEntry entry : obfData.shuffledParams) {
            if (entry.isFake()) {
                fakeParamsSize += entry.type.getSize();
            }
        }
        obfMethod.maxStack = originalMethod.maxStack + 10;
        obfMethod.maxLocals = originalMethod.maxLocals + fakeParamsSize + localsShift;

        if (originalMethod.localVariables != null) {
            obfMethod.localVariables = new ArrayList<>();
            for (LocalVariableNode lvn : originalMethod.localVariables) {
                LabelNode newStart = labelMap.getOrDefault(lvn.start, lvn.start);
                LabelNode newEnd = labelMap.getOrDefault(lvn.end, lvn.end);

                int newIndex = lvn.index;
                if (varIndexMap.containsKey(lvn.index)) {
                    newIndex = varIndexMap.get(lvn.index);
                } else if (lvn.index >= originalLocalsStart) {
                    newIndex = lvn.index + localsShift;
                }

                obfMethod.localVariables.add(new LocalVariableNode(
                    lvn.name, lvn.desc, lvn.signature, newStart, newEnd, newIndex
                ));
            }
        }

        if (originalMethod.tryCatchBlocks != null) {
            obfMethod.tryCatchBlocks = new ArrayList<>();
            for (TryCatchBlockNode tcb : originalMethod.tryCatchBlocks) {
                obfMethod.tryCatchBlocks.add(new TryCatchBlockNode(
                    labelMap.getOrDefault(tcb.start, tcb.start),
                    labelMap.getOrDefault(tcb.end, tcb.end),
                    labelMap.getOrDefault(tcb.handler, tcb.handler),
                    tcb.type
                ));
            }
        }

        return obfMethod;
    }

    private InsnList generateOpaquePredicate(MethodObfData obfData, int[] newParamVarIndex) {
        InsnList insns = new InsnList();

        LabelNode continueLabel = new LabelNode();

        boolean first = true;
        for (int i = 0; i < obfData.shuffledParams.size(); i++) {
            ParamEntry entry = obfData.shuffledParams.get(i);
            if (!entry.isFake()) continue;

            int varIndex = newParamVarIndex[i];

            if (entry.type.getSort() == Type.LONG) {
                insns.add(new VarInsnNode(LLOAD, varIndex));
                insns.add(new InsnNode(L2I));
            } else {
                insns.add(new VarInsnNode(ILOAD, varIndex));
            }

            if (!first) {
                insns.add(new InsnNode(IXOR));
            }
            first = false;
        }

        insns.add(pushIntValue(obfData.expectedXor));
        insns.add(new JumpInsnNode(IF_ICMPEQ, continueLabel));

        insns.add(generateDeadCode());

        insns.add(continueLabel);
        insns.add(new FrameNode(F_SAME, 0, null, 0, null));

        return insns;
    }

    private InsnList generateDeadCode() {
        InsnList insns = new InsnList();

        int pattern = random.nextInt(4);

        switch (pattern) {
            case 0:
                insns.add(new TypeInsnNode(NEW, "java/lang/RuntimeException"));
                insns.add(new InsnNode(DUP));
                insns.add(new LdcInsnNode("Verification failed"));
                insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/RuntimeException",
                    "<init>", "(Ljava/lang/String;)V", false));
                insns.add(new InsnNode(ATHROW));
                break;

            case 1:
                insns.add(new TypeInsnNode(NEW, "java/lang/Error"));
                insns.add(new InsnNode(DUP));
                insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Error",
                    "<init>", "()V", false));
                insns.add(new InsnNode(ATHROW));
                break;

            case 2:
                LabelNode loopLabel = new LabelNode();
                insns.add(loopLabel);
                insns.add(new FrameNode(F_SAME, 0, null, 0, null));
                insns.add(new JumpInsnNode(GOTO, loopLabel));
                break;

            case 3:
                insns.add(new InsnNode(ACONST_NULL));
                insns.add(new InsnNode(ATHROW));
                break;
        }

        return insns;
    }

    private void convertToBridge(ClassNode classNode, MethodNode originalMethod, MethodObfData obfData) {

        originalMethod.instructions.clear();
        originalMethod.tryCatchBlocks = new ArrayList<>();
        originalMethod.localVariables = null;

        InsnList bridgeInsns = new InsnList();
        boolean isStatic = (originalMethod.access & ACC_STATIC) != 0;

        if (!isStatic) {
            bridgeInsns.add(new VarInsnNode(ALOAD, 0));
        }

        Type[] originalArgs = Type.getArgumentTypes(originalMethod.desc);
        int[] originalVarIndex = new int[originalArgs.length];
        int varIdx = isStatic ? 0 : 1;
        for (int i = 0; i < originalArgs.length; i++) {
            originalVarIndex[i] = varIdx;
            varIdx += originalArgs[i].getSize();
        }

        for (ParamEntry entry : obfData.shuffledParams) {
            if (entry.isFake()) {

                bridgeInsns.add(pushIntValue(entry.magicValue));
                if (entry.type.getSort() == Type.LONG) {
                    bridgeInsns.add(new InsnNode(I2L));
                }
            } else {

                int loadIdx = originalVarIndex[entry.originalIndex];
                bridgeInsns.add(new VarInsnNode(entry.type.getOpcode(ILOAD), loadIdx));
            }
        }

        int invokeOpcode = isStatic ? INVOKESTATIC : INVOKESPECIAL;
        bridgeInsns.add(new MethodInsnNode(invokeOpcode, classNode.name,
            obfData.obfMethodName, obfData.obfMethodDesc, false));

        Type returnType = Type.getReturnType(originalMethod.desc);
        bridgeInsns.add(new InsnNode(returnType.getOpcode(IRETURN)));

        originalMethod.instructions = bridgeInsns;

        originalMethod.access |= ACC_BRIDGE | ACC_SYNTHETIC;

        int stackSize = (isStatic ? 0 : 1);
        for (ParamEntry entry : obfData.shuffledParams) {
            stackSize += entry.type.getSize();
        }
        originalMethod.maxStack = stackSize + 2;
        originalMethod.maxLocals = varIdx;
    }

    private AbstractInsnNode pushIntValue(int value) {
        if (value >= -1 && value <= 5) {
            return new InsnNode(ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return new IntInsnNode(BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return new IntInsnNode(SIPUSH, value);
        } else {
            return new LdcInsnNode(value);
        }
    }

    private boolean shouldObfuscate(ClassNode classNode, MethodNode methodNode) {

        if (methodNode.name.equals("<init>") ||
            methodNode.name.equals("<clinit>") ||
            (methodNode.access & ACC_ABSTRACT) != 0 ||
            (methodNode.access & ACC_NATIVE) != 0) {
            return false;
        }

        if ((methodNode.access & ACC_SYNTHETIC) != 0 ||
            (methodNode.access & ACC_BRIDGE) != 0) {
            return false;
        }

        if (methodNode.name.equals("main") && methodNode.desc.equals("([Ljava/lang/String;)V")) {
            return false;
        }

        if ((classNode.access & ACC_INTERFACE) != 0) {
            return false;
        }

        if (this.getBozar().isExcluded(this, classNode.name + "." + methodNode.name + "()")) {
            return false;
        }

        BozarConfig.BozarOptions.ParamObfuscationOption level =
            this.getBozar().getConfig().getOptions().getParamObfuscation();

        boolean isPrivate = (methodNode.access & ACC_PRIVATE) != 0;
        boolean isStatic = (methodNode.access & ACC_STATIC) != 0;
        boolean isPublic = (methodNode.access & ACC_PUBLIC) != 0;
        boolean isProtected = (methodNode.access & ACC_PROTECTED) != 0;
        boolean isPackagePrivate = !isPrivate && !isPublic && !isProtected;

        if (!isPrivate && !isStatic) {
            if (overridesExternalMethod(classNode, methodNode)) {
                return false;
            }
        }

        switch (level) {
            case OFF:
                return false;

            case LIGHT:
                return isPrivate;

            case BASIC:
                return isPrivate || isPackagePrivate;

            case HEAVY:
                return !isPublic;

            default:
                return false;
        }
    }

    private boolean overridesExternalMethod(ClassNode classNode, MethodNode methodNode) {
        if (classNode.superName != null && !classNode.superName.equals("java/lang/Object")) {
            ClassNode superClass = findClass(classNode.superName);
            if (superClass == null) {
                return true;
            }

            for (MethodNode superMethod : superClass.methods) {
                if (superMethod.name.equals(methodNode.name) &&
                    superMethod.desc.equals(methodNode.desc) &&
                    (superMethod.access & ACC_PRIVATE) == 0) {
                    return true;
                }
            }
        }

        for (String iface : classNode.interfaces) {
            ClassNode ifaceClass = findClass(iface);
            if (ifaceClass == null) {
                return true;
            }

            for (MethodNode ifaceMethod : ifaceClass.methods) {
                if (ifaceMethod.name.equals(methodNode.name) &&
                    ifaceMethod.desc.equals(methodNode.desc)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getParamObfuscation() != BozarConfig.BozarOptions.ParamObfuscationOption.OFF,
            BozarConfig.BozarOptions.ParamObfuscationOption.class
        );
    }
}
