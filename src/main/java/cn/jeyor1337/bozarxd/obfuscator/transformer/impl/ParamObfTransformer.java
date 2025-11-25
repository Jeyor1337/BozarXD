package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Parameter Obfuscation Transformer
 *
 * Uses a bridge method pattern to maintain compatibility with external callers:
 * - Original method signature is preserved as a bridge that forwards to the obfuscated method
 * - A new private method with extended parameters contains the actual logic
 * - Magic values are used instead of zeros to increase obfuscation strength
 * - Opaque predicates using the extra parameters add complexity
 * - Parameter order is shuffled to further increase obfuscation
 *
 * Example: foo(int a, String b) -> foo$xxx(long m1, String b, int m2, int a, int m3)
 * The original parameters are mixed with fake parameters in random order.
 */
public class ParamObfTransformer extends ClassTransformer {

    private static final int MIN_EXTRA_PARAMS = 2;
    private static final int MAX_EXTRA_PARAMS = 4;

    // Represents a parameter in the shuffled list
    private static class ParamEntry {
        final Type type;
        final int originalIndex;  // -1 for fake params, >= 0 for original params
        final int magicValue;     // Only used for fake params

        // Constructor for original parameter
        ParamEntry(Type type, int originalIndex) {
            this.type = type;
            this.originalIndex = originalIndex;
            this.magicValue = 0;
        }

        // Constructor for fake parameter
        ParamEntry(Type type, int magicValue, boolean isFake) {
            this.type = type;
            this.originalIndex = -1;
            this.magicValue = magicValue;
        }

        boolean isFake() {
            return originalIndex < 0;
        }
    }

    // Data class to hold obfuscation info for each method
    private static class MethodObfData {
        final String obfMethodName;
        final String obfMethodDesc;
        final List<ParamEntry> shuffledParams;  // All params in shuffled order
        final int[] originalToShuffledIndex;    // Maps original param index -> shuffled index
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

    // Map: className -> (methodName + desc -> MethodObfData)
    private final Map<String, Map<String, MethodObfData>> classMethodMap = new HashMap<>();
    // Track methods to be processed in post phase
    private final Map<String, List<MethodNode>> methodsToProcess = new HashMap<>();

    public ParamObfTransformer(Bozar bozar) {
        super(bozar, "Parameter Obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void pre() {
        // First pass: analyze all methods and prepare obfuscation data
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
            // Still need to update calls to obfuscated methods in other classes
            updateMethodCalls(classNode, methodNode);
            return;
        }

        String methodKey = methodNode.name + methodNode.desc;
        MethodObfData obfData = methodMap.get(methodKey);

        if (obfData != null) {
            // This method will be converted to a bridge - mark for processing in post()
            methodsToProcess.computeIfAbsent(classNode.name, k -> new ArrayList<>()).add(methodNode);
        }

        // Update calls to obfuscated methods
        updateMethodCalls(classNode, methodNode);
    }

    @Override
    public void post() {
        // Process all marked methods: create obfuscated versions and convert originals to bridges
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

                // Create the new obfuscated method with actual logic
                MethodNode obfMethod = createObfuscatedMethod(classNode, originalMethod, obfData);
                newMethods.add(obfMethod);

                // Convert original method to bridge
                convertToBridge(classNode, originalMethod, obfData);

                this.getBozar().log("Created bridge method %s.%s -> %s",
                    classNode.name, originalMethod.name, obfData.obfMethodName);
            }

            // Add all new methods to the class
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

            // Check if this is an internal call that should use the obfuscated method directly
            if (isInternalCall(classNode, methodNode, methodInsn)) {
                // Reorder arguments on stack and add magic values
                reorderCallArguments(methodNode, methodInsn, obfData);
                methodInsn.name = obfData.obfMethodName;
                methodInsn.desc = obfData.obfMethodDesc;
            }
            // External calls will use the bridge method (original signature) - no change needed
        }
    }

    private void reorderCallArguments(MethodNode callerMethod, MethodInsnNode callInsn, MethodObfData obfData) {
        // We need to reorder arguments that are already on the stack
        // Original stack: [this?], arg0, arg1, arg2, ...
        // Target stack: [this?], shuffled_arg0, shuffled_arg1, ...

        Type[] originalArgs = Type.getArgumentTypes(callInsn.desc);
        int originalArgCount = originalArgs.length;

        if (originalArgCount == 0) {
            // No original args, just push magic values in shuffled order
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

        // For methods with original arguments, we need to:
        // 1. Store all original args to temp locals
        // 2. Push args in shuffled order (including magic values)

        InsnList preInsns = new InsnList();
        InsnList pushInsns = new InsnList();

        // Find a safe starting index for temp variables
        int tempVarBase = callerMethod.maxLocals;

        // Calculate size needed for original args
        int[] originalArgOffsets = new int[originalArgCount];
        int tempOffset = 0;
        for (int i = 0; i < originalArgCount; i++) {
            originalArgOffsets[i] = tempOffset;
            tempOffset += originalArgs[i].getSize();
        }

        // Store original args in reverse order (they're on stack in order)
        for (int i = originalArgCount - 1; i >= 0; i--) {
            Type argType = originalArgs[i];
            int storeIndex = tempVarBase + originalArgOffsets[i];
            preInsns.add(new VarInsnNode(argType.getOpcode(ISTORE), storeIndex));
        }

        // Push args in shuffled order
        for (ParamEntry entry : obfData.shuffledParams) {
            if (entry.isFake()) {
                // Push magic value
                pushInsns.add(pushIntValue(entry.magicValue));
                if (entry.type.getSort() == Type.LONG) {
                    pushInsns.add(new InsnNode(I2L));
                }
            } else {
                // Load original arg from temp storage
                int loadIndex = tempVarBase + originalArgOffsets[entry.originalIndex];
                pushInsns.add(new VarInsnNode(entry.type.getOpcode(ILOAD), loadIndex));
            }
        }

        // Insert instructions
        callerMethod.instructions.insertBefore(callInsn, preInsns);
        callerMethod.instructions.insertBefore(callInsn, pushInsns);

        // Update maxLocals if needed
        int tempVarsNeeded = tempOffset;
        if (callerMethod.maxLocals < tempVarBase + tempVarsNeeded) {
            callerMethod.maxLocals = tempVarBase + tempVarsNeeded;
        }
    }

    private boolean isInternalCall(ClassNode classNode, MethodNode callerMethod, MethodInsnNode callInsn) {
        // Same class call - definitely internal
        if (callInsn.owner.equals(classNode.name)) {
            return true;
        }

        // Cross-class call within obfuscated classes - also internal
        return classMethodMap.containsKey(callInsn.owner);
    }

    private MethodObfData createObfuscationData(ClassNode classNode, MethodNode methodNode) {
        // Generate unique obfuscated method name
        String obfMethodName = generateObfMethodName(classNode, methodNode.name);
        boolean isStatic = (methodNode.access & ACC_STATIC) != 0;

        Type[] originalArgs = Type.getArgumentTypes(methodNode.desc);
        int originalArgCount = originalArgs.length;

        // Random number of extra parameters
        int extraParamCount = MIN_EXTRA_PARAMS + random.nextInt(MAX_EXTRA_PARAMS - MIN_EXTRA_PARAMS + 1);

        // Create all param entries
        List<ParamEntry> allParams = new ArrayList<>();

        // Add original parameters
        for (int i = 0; i < originalArgCount; i++) {
            allParams.add(new ParamEntry(originalArgs[i], i));
        }

        // Add fake parameters with magic values
        int xorResult = 0;
        for (int i = 0; i < extraParamCount; i++) {
            Type type = random.nextBoolean() ? Type.INT_TYPE : Type.LONG_TYPE;
            int magicValue = random.nextInt(Integer.MAX_VALUE - 1000) + 1000;
            allParams.add(new ParamEntry(type, magicValue, true));
            xorResult ^= magicValue;
        }

        // Shuffle all parameters
        List<ParamEntry> shuffledParams = new ArrayList<>(allParams);
        Collections.shuffle(shuffledParams, random);

        // Build mapping from original index to shuffled index
        int[] originalToShuffledIndex = new int[originalArgCount];
        for (int shuffledIdx = 0; shuffledIdx < shuffledParams.size(); shuffledIdx++) {
            ParamEntry entry = shuffledParams.get(shuffledIdx);
            if (!entry.isFake()) {
                originalToShuffledIndex[entry.originalIndex] = shuffledIdx;
            }
        }

        // Build new descriptor
        String obfMethodDesc = buildShuffledDescriptor(shuffledParams, Type.getReturnType(methodNode.desc));

        return new MethodObfData(obfMethodName, obfMethodDesc, shuffledParams,
                originalToShuffledIndex, xorResult, isStatic);
    }

    private String generateObfMethodName(ClassNode classNode, String originalName) {
        String baseName = originalName + "$" + Integer.toHexString(random.nextInt(0xFFFF));

        // Ensure unique name
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
        // Create new method node with shuffled descriptor
        MethodNode obfMethod = new MethodNode(
            ACC_PRIVATE | ACC_SYNTHETIC | (originalMethod.access & ACC_STATIC),
            obfData.obfMethodName,
            obfData.obfMethodDesc,
            null,
            originalMethod.exceptions != null ? originalMethod.exceptions.toArray(new String[0]) : null
        );

        // Build variable index remapping
        // Original: [this?], param0, param1, ... , local0, local1, ...
        // New: [this?], shuffled0, shuffled1, ... , local0, local1, ...

        Type[] originalArgs = Type.getArgumentTypes(originalMethod.desc);
        int thisOffset = obfData.isStatic ? 0 : 1;

        // Calculate original param var indices
        int[] originalParamVarIndex = new int[originalArgs.length];
        int varIdx = thisOffset;
        for (int i = 0; i < originalArgs.length; i++) {
            originalParamVarIndex[i] = varIdx;
            varIdx += originalArgs[i].getSize();
        }
        int originalLocalsStart = varIdx;

        // Calculate new param var indices based on shuffled order
        int[] newParamVarIndex = new int[obfData.shuffledParams.size()];
        varIdx = thisOffset;
        for (int i = 0; i < obfData.shuffledParams.size(); i++) {
            newParamVarIndex[i] = varIdx;
            varIdx += obfData.shuffledParams.get(i).type.getSize();
        }
        int newLocalsStart = varIdx;

        // Build mapping: original var index -> new var index
        Map<Integer, Integer> varIndexMap = new HashMap<>();

        // Map 'this' if not static
        if (!obfData.isStatic) {
            varIndexMap.put(0, 0);
        }

        // Map original parameters to their new positions
        for (int origIdx = 0; origIdx < originalArgs.length; origIdx++) {
            int shuffledIdx = obfData.originalToShuffledIndex[origIdx];
            int oldVarIdx = originalParamVarIndex[origIdx];
            int newVarIdx = newParamVarIndex[shuffledIdx];
            varIndexMap.put(oldVarIdx, newVarIdx);

            // For wide types (long/double), also map the second slot
            if (originalArgs[origIdx].getSize() == 2) {
                varIndexMap.put(oldVarIdx + 1, newVarIdx + 1);
            }
        }

        // Map local variables (shift by the difference in locals start)
        int localsShift = newLocalsStart - originalLocalsStart;

        // Copy instructions with remapped variable indices
        InsnList newInsns = new InsnList();

        // Add opaque predicate at the beginning
        newInsns.add(generateOpaquePredicate(obfData, newParamVarIndex));

        // Clone labels first
        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : originalMethod.instructions) {
            if (insn instanceof LabelNode) {
                labelMap.put((LabelNode) insn, new LabelNode());
            }
        }

        // Copy and remap instructions
        for (AbstractInsnNode insn : originalMethod.instructions) {
            AbstractInsnNode cloned = insn.clone(labelMap);

            // Remap variable indices
            if (cloned instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) cloned;
                if (varIndexMap.containsKey(varInsn.var)) {
                    varInsn.var = varIndexMap.get(varInsn.var);
                } else if (varInsn.var >= originalLocalsStart) {
                    // Local variable - shift it
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

        // Update method attributes
        int fakeParamsSize = 0;
        for (ParamEntry entry : obfData.shuffledParams) {
            if (entry.isFake()) {
                fakeParamsSize += entry.type.getSize();
            }
        }
        obfMethod.maxStack = originalMethod.maxStack + 10;
        obfMethod.maxLocals = originalMethod.maxLocals + fakeParamsSize + localsShift;

        // Copy and remap local variables
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

        // Copy try-catch blocks
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

        // Load and XOR all fake (magic) parameters
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

        // Compare with expected XOR value
        insns.add(pushIntValue(obfData.expectedXor));
        insns.add(new JumpInsnNode(IF_ICMPEQ, continueLabel));

        // Generate dead code
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
        // Clear original instructions and convert to bridge
        originalMethod.instructions.clear();
        originalMethod.tryCatchBlocks = new ArrayList<>();
        originalMethod.localVariables = null;

        InsnList bridgeInsns = new InsnList();
        boolean isStatic = (originalMethod.access & ACC_STATIC) != 0;

        // Load 'this' for instance methods
        if (!isStatic) {
            bridgeInsns.add(new VarInsnNode(ALOAD, 0));
        }

        // Calculate original param var indices
        Type[] originalArgs = Type.getArgumentTypes(originalMethod.desc);
        int[] originalVarIndex = new int[originalArgs.length];
        int varIdx = isStatic ? 0 : 1;
        for (int i = 0; i < originalArgs.length; i++) {
            originalVarIndex[i] = varIdx;
            varIdx += originalArgs[i].getSize();
        }

        // Push arguments in shuffled order
        for (ParamEntry entry : obfData.shuffledParams) {
            if (entry.isFake()) {
                // Push magic value
                bridgeInsns.add(pushIntValue(entry.magicValue));
                if (entry.type.getSort() == Type.LONG) {
                    bridgeInsns.add(new InsnNode(I2L));
                }
            } else {
                // Load original argument
                int loadIdx = originalVarIndex[entry.originalIndex];
                bridgeInsns.add(new VarInsnNode(entry.type.getOpcode(ILOAD), loadIdx));
            }
        }

        // Call the obfuscated method
        int invokeOpcode = isStatic ? INVOKESTATIC : INVOKESPECIAL;
        bridgeInsns.add(new MethodInsnNode(invokeOpcode, classNode.name,
            obfData.obfMethodName, obfData.obfMethodDesc, false));

        // Return
        Type returnType = Type.getReturnType(originalMethod.desc);
        bridgeInsns.add(new InsnNode(returnType.getOpcode(IRETURN)));

        originalMethod.instructions = bridgeInsns;

        // Mark as synthetic bridge
        originalMethod.access |= ACC_BRIDGE | ACC_SYNTHETIC;

        // Update max stack/locals
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
        // Skip constructors, static initializers, and abstract methods
        if (methodNode.name.equals("<init>") ||
            methodNode.name.equals("<clinit>") ||
            (methodNode.access & ACC_ABSTRACT) != 0 ||
            (methodNode.access & ACC_NATIVE) != 0) {
            return false;
        }

        // Skip synthetic and bridge methods
        if ((methodNode.access & ACC_SYNTHETIC) != 0 ||
            (methodNode.access & ACC_BRIDGE) != 0) {
            return false;
        }

        // Skip main method
        if (methodNode.name.equals("main") && methodNode.desc.equals("([Ljava/lang/String;)V")) {
            return false;
        }

        // Skip interface methods
        if ((classNode.access & ACC_INTERFACE) != 0) {
            return false;
        }

        // Check exclusions
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

        // Check if method overrides a superclass/interface method
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
