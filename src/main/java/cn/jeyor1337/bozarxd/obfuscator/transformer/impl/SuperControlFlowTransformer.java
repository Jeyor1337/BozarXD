package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ControlFlowTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.*;

import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Super Control Flow Obfuscator - Complete Rewrite
 *
 * Integrates advanced techniques from multiple obfuscators:
 * 1. Proxy Block Pattern - Intermediate blocks between jumps with seed loaders
 * 2. Hash Verification System - Each block has unique hash, verify before jump
 * 3. XOR Field Chain Encryption - Multi-layer XOR with fake decryption paths
 * 4. Lookup Switch Dispatch - Switch with 1 real + 3 fake cases
 * 5. Multi-level Seeding - Block/Method/Class level seed system
 *
 * This is the most aggressive control flow obfuscation available.
 */
public class SuperControlFlowTransformer extends ControlFlowTransformer {

    public SuperControlFlowTransformer(Bozar bozar) {
        super(bozar, "Super Control Flow", BozarCategory.ADVANCED);
    }

    // ==================== Configuration Constants ====================

    // Proxy block configuration
    private static final double PROXY_BLOCK_PROBABILITY = 0.65;
    private static final int SEED_LOAD_OPERATIONS = 3;

    // Hash verification configuration
    private static final double HASH_VERIFY_PROBABILITY = 0.50;
    private static final int HASH_PRIME_1 = 0x9E3779B1;
    private static final int HASH_PRIME_2 = 0x85EBCA6B;

    // XOR chain configuration
    private static final int XOR_CHAIN_DEPTH = 2;
    private static final double XOR_CHAIN_PROBABILITY = 0.30;

    // Lookup switch configuration
    private static final int SWITCH_FAKE_CASES = 3;
    private static final double SWITCH_DISPATCH_PROBABILITY = 0.40;

    // Exception trap configuration
    private static final double EXCEPTION_TRAP_PROBABILITY = 0.35;

    // Conditional branch obfuscation configuration
    private static final double CONDITIONAL_BRANCH_PROBABILITY = 0.50;
    private static final int CONDITIONAL_FAKE_CASES = 2;

    // Field hash verification configuration
    private static final String HASH_FIELD_NAME = String.valueOf((char)5098);
    private static final double FIELD_HASH_PROBABILITY = 0.40;

    // ==================== Class-Level State ====================

    // Class-level seed (shared across all methods in class)
    private int classSeed = 0;

    // XOR keys (used inline, no field creation)
    private int xorKey1 = 0;
    private int xorKey2 = 0;
    private int hashKey = 0;

    // Current class being processed
    private ClassNode currentClass = null;

    // Method counter for unique seeds
    private int methodCounter = 0;

    // Hash field value for verification
    private int hashFieldValue = 0;
    private boolean hashFieldCreated = false;

    // ==================== Method-Level State ====================

    // Method-level seed
    private int methodSeed = 0;

    // Block seeds map (label -> seed)
    private Map<LabelNode, Integer> blockSeeds = new HashMap<>();

    // Block hashes map (label -> hash)
    private Map<LabelNode, Integer> blockHashes = new HashMap<>();

    // Track original jump instructions to avoid processing newly created ones
    private Set<AbstractInsnNode> originalJumps = new HashSet<>();

    // Frame analysis for stack-aware transformations
    private Frame<BasicValue>[] frames = null;
    private Map<AbstractInsnNode, Integer> insnIndexMap = new HashMap<>();

    // ==================== Lifecycle Methods ====================

    @Override
    public void transformClass(ClassNode classNode) {
        if (!ASMUtils.isClassEligibleToModify(classNode)) return;

        // Skip inner classes entirely - they have complex relationships with outer classes
        if (classNode.name.contains("$")) return;

        // Skip classes that use reflection heavily - field counts matter
        if (classUsesReflection(classNode)) return;

        // Skip classes that extend SecurityManager or ClassLoader
        if (classExtendsSensitiveType(classNode)) return;

        this.currentClass = classNode;
        this.methodCounter = 0;
        this.hashFieldCreated = false;

        // Generate class-level seed
        this.classSeed = ThreadLocalRandom.current().nextInt();

        // Generate XOR keys (no field creation)
        this.xorKey1 = ThreadLocalRandom.current().nextInt();
        this.xorKey2 = ThreadLocalRandom.current().nextInt();
        this.hashKey = ThreadLocalRandom.current().nextInt();

        // Create hash verification field only if we have transformable methods
        if (hasTransformableMethods(classNode)) {
            this.hashFieldValue = ThreadLocalRandom.current().nextInt();
            FieldNode hashField = new FieldNode(
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                HASH_FIELD_NAME,
                "Ljava/lang/String;",
                null,
                null
            );
            classNode.fields.add(hashField);

            // Initialize field in <clinit>
            MethodNode clinit = ASMUtils.findOrCreateClinit(classNode);
            InsnList initInsns = new InsnList();
            initInsns.add(new LdcInsnNode(String.valueOf(hashFieldValue)));
            initInsns.add(new FieldInsnNode(PUTSTATIC, classNode.name, HASH_FIELD_NAME, "Ljava/lang/String;"));
            clinit.instructions.insert(initInsns);

            this.hashFieldCreated = true;
        }
    }

    /**
     * Check if class uses reflection or resource loading (methods that count fields/methods)
     */
    private boolean classUsesReflection(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode min) {
                    // Check for reflection that counts fields/methods
                    if (min.owner.equals("java/lang/Class")) {
                        if (min.name.equals("getFields") || min.name.equals("getDeclaredFields") ||
                            min.name.equals("getMethods") || min.name.equals("getDeclaredMethods") ||
                            min.name.equals("getConstructors") || min.name.equals("getDeclaredConstructors") ||
                            min.name.equals("getResourceAsStream") || min.name.equals("getResource")) {
                            return true;
                        }
                    }
                    // Check for stack trace usage
                    if (min.owner.equals("java/lang/Throwable") && min.name.equals("getStackTrace")) {
                        return true;
                    }
                    // Check for resource loading
                    if (min.name.equals("getResourceAsStream") || min.name.equals("getResource")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if class extends sensitive types like SecurityManager or ClassLoader
     */
    private boolean classExtendsSensitiveType(ClassNode classNode) {
        if (classNode.superName == null) return false;
        return classNode.superName.equals("java/lang/SecurityManager") ||
               classNode.superName.equals("java/lang/ClassLoader") ||
               classNode.superName.contains("SecurityManager") ||
               classNode.superName.contains("ClassLoader");
    }

    /**
     * Check if class has methods that can be transformed
     */
    private boolean hasTransformableMethods(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if (ASMUtils.isMethodEligibleToModify(classNode, method) && !shouldSkipMethod(method)) {
                if (method.instructions.size() >= 3) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void post() {
        // Clean up class-level state
        this.currentClass = null;
        this.methodCounter = 0;
        this.classSeed = 0;
        this.xorKey1 = 0;
        this.xorKey2 = 0;
        this.hashKey = 0;
        this.blockSeeds.clear();
        this.blockHashes.clear();
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        // Skip if class was skipped (currentClass is null or different)
        if (this.currentClass == null || !this.currentClass.name.equals(classNode.name)) return;

        if (!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) return;
        if (shouldSkipMethod(methodNode)) return;

        // Initialize method-level state
        this.methodCounter++;
        this.methodSeed = classSeed ^ (methodCounter * HASH_PRIME_1);
        this.blockSeeds.clear();
        this.blockHashes.clear();
        this.originalJumps.clear();
        this.frames = null;
        this.insnIndexMap.clear();

        // Perform frame analysis for stack-aware transformations
        try {
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            frames = analyzer.analyze(classNode.name, methodNode);

            // Build instruction index map for quick lookup
            int index = 0;
            for (AbstractInsnNode insn : methodNode.instructions) {
                insnIndexMap.put(insn, index++);
            }
        } catch (AnalyzerException e) {
            // If analysis fails, proceed without stack awareness
            frames = null;
        }

        // Collect original jump instructions BEFORE any transformation
        // This prevents processing newly created jumps which can cause infinite loops
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof JumpInsnNode) {
                originalJumps.add(insn);
            }
        }

        // Collect and seed all labels (blocks)
        initializeBlockSeeds(methodNode);

        // Apply transformation phases in order
        // Phase 1: Proxy Block insertion (most impactful, runs first)
        applyProxyBlockTransformation(classNode, methodNode);

        // Phase 2: Hash Verification dispatch
        applyHashVerificationTransformation(classNode, methodNode);

        // Phase 3: XOR Chain encryption on constants
        applyXORChainTransformation(classNode, methodNode);

        // Phase 4: Lookup Switch dispatch
        applyLookupSwitchTransformation(classNode, methodNode);

        // Phase 5: Exception trap insertion
        applyExceptionTrapTransformation(classNode, methodNode);

        // Phase 6: Conditional branch obfuscation (new)
        applyConditionalBranchTransformation(classNode, methodNode);
    }

    // ==================== Initialization Methods ====================

    private void initializeBlockSeeds(MethodNode methodNode) {
        int seedCounter = 0;
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof LabelNode label) {
                // Generate unique seed for this block
                int blockSeed = computeBlockSeed(seedCounter++);
                blockSeeds.put(label, blockSeed);

                // Compute hash for verification
                int blockHash = computeBlockHash(blockSeed, seedCounter);
                blockHashes.put(label, blockHash);
            }
        }
    }

    private int computeBlockSeed(int index) {
        // Multi-level seed computation
        int seed = methodSeed;
        seed ^= (index * HASH_PRIME_1);
        seed = Integer.rotateLeft(seed, 13);
        seed *= HASH_PRIME_2;
        return seed;
    }

    private int computeBlockHash(int seed, int index) {
        // Hash computation for verification
        int hash = seed;
        hash ^= (hash >>> 16);
        hash *= HASH_PRIME_1;
        hash ^= (hash >>> 13);
        hash *= HASH_PRIME_2;
        hash ^= (hash >>> 16);
        hash ^= (index * 31);
        return hash;
    }

    private boolean shouldSkipMethod(MethodNode methodNode) {
        // Skip very small methods (less than 3 instructions can't be meaningfully obfuscated)
        if (methodNode.instructions.size() < 3) return true;

        // Skip special methods
        if ((methodNode.access & (ACC_BRIDGE | ACC_SYNTHETIC | ACC_ABSTRACT | ACC_NATIVE)) != 0) return true;

        // Skip reflection-heavy methods
        if (methodUsesReflection(methodNode)) return true;

        // Skip methods with precision-sensitive floating point operations
        if (methodHasPrecisionSensitiveCode(methodNode)) return true;

        // Skip recursive methods - they are sensitive to control flow changes
        if (methodHasSelfCall(methodNode)) return true;

        // Skip methods using lambdas, method references, or threading
        if (methodUsesLambdaOrThreading(methodNode)) return true;

        return false;
    }

    /**
     * Check if method uses lambdas, method references, or threading
     */
    private boolean methodUsesLambdaOrThreading(MethodNode methodNode) {
        for (AbstractInsnNode insn : methodNode.instructions) {
            // Check for invokedynamic (used by lambdas and method references)
            if (insn instanceof InvokeDynamicInsnNode) {
                return true;
            }

            // Check for thread-related and classloader-related calls
            if (insn instanceof MethodInsnNode min) {
                if (min.owner.contains("Thread") ||
                    min.owner.contains("Executor") ||
                    min.owner.contains("Runnable") ||
                    min.owner.contains("Callable") ||
                    min.owner.contains("Future") ||
                    min.owner.contains("ClassLoader") ||
                    min.owner.contains("Loader") ||
                    min.name.equals("submit") ||
                    min.name.equals("execute") ||
                    min.name.equals("sleep") ||
                    min.name.equals("wait") ||
                    min.name.equals("notify") ||
                    min.name.equals("findClass") ||
                    min.name.equals("loadClass") ||
                    min.name.equals("defineClass") ||
                    min.name.equals("newInstance") ||
                    min.name.equals("getResourceAsStream")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if method calls itself (recursive method)
     */
    private boolean methodHasSelfCall(MethodNode methodNode) {
        String methodName = methodNode.name;
        String methodDesc = methodNode.desc;
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof MethodInsnNode min) {
                // Check if it's a call to a method with the same name
                // (We don't have class info here, but same name+desc is a good indicator)
                if (min.name.equals(methodName) && min.desc.equals(methodDesc)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if method has precision-sensitive floating point code
     * Skip any method with floating point operations to be safe
     */
    private boolean methodHasPrecisionSensitiveCode(MethodNode methodNode) {
        for (AbstractInsnNode insn : methodNode.instructions) {
            int opcode = insn.getOpcode();

            // Check for any float/double operations
            if (opcode == FADD || opcode == FSUB || opcode == FMUL || opcode == FDIV ||
                opcode == DADD || opcode == DSUB || opcode == DMUL || opcode == DDIV ||
                opcode == FCMPL || opcode == FCMPG || opcode == DCMPL || opcode == DCMPG ||
                opcode == F2D || opcode == D2F || opcode == I2F || opcode == I2D ||
                opcode == F2I || opcode == D2I || opcode == F2L || opcode == D2L ||
                opcode == FLOAD || opcode == DLOAD || opcode == FSTORE || opcode == DSTORE ||
                opcode == FRETURN || opcode == DRETURN) {
                return true;
            }

            // Check for float/double constants
            if (insn instanceof LdcInsnNode ldc) {
                if (ldc.cst instanceof Float || ldc.cst instanceof Double) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean methodHasTryCatch(MethodNode methodNode) {
        return methodNode.tryCatchBlocks != null && !methodNode.tryCatchBlocks.isEmpty();
    }

    private boolean methodHasAnnotations(MethodNode methodNode) {
        return methodNode.visibleAnnotations != null || methodNode.invisibleAnnotations != null;
    }

    private boolean methodUsesReflection(MethodNode methodNode) {
        return Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn instanceof MethodInsnNode)
            .map(insn -> (MethodInsnNode) insn)
            .anyMatch(min -> min.owner.startsWith("java/lang/reflect/") ||
                (min.owner.equals("java/lang/Class") &&
                 (min.name.contains("Field") || min.name.contains("Method") ||
                  min.name.equals("forName") || min.name.contains("Annotation"))));
    }

    /**
     * Check if the stack is empty at the given instruction index.
     * Returns true if frames are available and stack is empty, or if frames are not available.
     */
    private boolean isStackEmpty(AbstractInsnNode insn) {
        if (frames == null) return true; // Assume safe if no frame analysis
        Integer index = insnIndexMap.get(insn);
        if (index == null || index >= frames.length) return true;
        Frame<BasicValue> frame = frames[index];
        return frame == null || frame.getStackSize() == 0;
    }

    // ==================== Phase 1: Proxy Block Transformation ====================

    /**
     * Proxy Block Pattern: Insert intermediate blocks between jumps
     *
     * Original: IF_XX -> target
     *
     * After:    IF_XX -> proxy_block
     *           proxy_block:
     *             seed_load_operations
     *             GOTO target
     */
    private void applyProxyBlockTransformation(ClassNode classNode, MethodNode methodNode) {
        if (methodNode.instructions.size() > 2500) return;

        // Collect all jump instructions
        List<JumpInsnNode> jumps = collectJumpInstructions(methodNode);

        for (JumpInsnNode jump : jumps) {
            if (ThreadLocalRandom.current().nextDouble() >= PROXY_BLOCK_PROBABILITY) continue;
            if (isBackwardJump(methodNode, jump)) continue;

            insertProxyBlock(classNode, methodNode, jump);
        }
    }

    private List<JumpInsnNode> collectJumpInstructions(MethodNode methodNode) {
        return Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn instanceof JumpInsnNode)
            .filter(insn -> originalJumps.contains(insn))  // Only process original jumps
            .map(insn -> (JumpInsnNode) insn)
            .filter(jump -> jump.getOpcode() != JSR)
            .collect(Collectors.toList());
    }

    private boolean isBackwardJump(MethodNode methodNode, JumpInsnNode jump) {
        int jumpIndex = methodNode.instructions.indexOf(jump);
        int targetIndex = methodNode.instructions.indexOf(jump.label);
        return targetIndex < jumpIndex;
    }

    private void insertProxyBlock(ClassNode classNode, MethodNode methodNode, JumpInsnNode jump) {
        LabelNode originalTarget = jump.label;
        LabelNode proxyLabel = new LabelNode();

        // Get or generate seed for target block
        int targetSeed = blockSeeds.getOrDefault(originalTarget, ThreadLocalRandom.current().nextInt());

        // Build proxy block
        InsnList proxyBlock = new InsnList();
        proxyBlock.add(proxyLabel);

        // Add seed loading operations
        for (int i = 0; i < SEED_LOAD_OPERATIONS; i++) {
            proxyBlock.add(createSeedLoadOperation(classNode, targetSeed, i));
        }

        // Jump to original target
        proxyBlock.add(new JumpInsnNode(GOTO, originalTarget));

        // Insert proxy block after the jump
        methodNode.instructions.insert(jump, proxyBlock);

        // Redirect jump to proxy
        jump.label = proxyLabel;

        // Register proxy block seed
        int proxySeed = computeBlockSeed(blockSeeds.size());
        blockSeeds.put(proxyLabel, proxySeed);
    }

    private InsnList createSeedLoadOperation(ClassNode classNode, int targetSeed, int opIndex) {
        InsnList ops = new InsnList();

        int pattern = (opIndex + targetSeed) % 7;

        switch (pattern) {
            case 0 -> {
                // XOR inline constant and discard
                ops.add(ASMUtils.pushInt(xorKey1));
                ops.add(ASMUtils.pushInt(targetSeed ^ xorKey1));
                ops.add(new InsnNode(IXOR));
                ops.add(new InsnNode(POP));
            }
            case 1 -> {
                // Hash computation
                ops.add(ASMUtils.pushInt(targetSeed));
                ops.add(ASMUtils.pushInt(HASH_PRIME_1));
                ops.add(new InsnNode(IMUL));
                ops.add(ASMUtils.pushInt(13));
                ops.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false));
                ops.add(new InsnNode(POP));
            }
            case 2 -> {
                // String hashCode
                ops.add(new LdcInsnNode("proxy_" + Integer.toHexString(targetSeed)));
                ops.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false));
                ops.add(new InsnNode(POP));
            }
            case 3 -> {
                // Bitcount operation
                ops.add(ASMUtils.pushInt(targetSeed));
                ops.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "bitCount", "(I)I", false));
                ops.add(new InsnNode(POP));
            }
            case 4 -> {
                // Reverse bits
                ops.add(ASMUtils.pushInt(targetSeed));
                ops.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "reverse", "(I)I", false));
                ops.add(new InsnNode(POP));
            }
            case 5 -> {
                // Number of leading zeros
                ops.add(ASMUtils.pushInt(targetSeed | 1));
                ops.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "numberOfLeadingZeros", "(I)I", false));
                ops.add(new InsnNode(POP));
            }
            case 6 -> {
                // System identity hash
                ops.add(new LdcInsnNode("seed"));
                ops.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false));
                ops.add(new InsnNode(POP));
            }
        }

        return ops;
    }

    // ==================== Phase 2: Hash Verification Transformation ====================

    /**
     * Hash Verification: Add hash check after GOTO jumps (not conditional jumps!)
     *
     * IMPORTANT: We only transform GOTO instructions, not conditional jumps (IF_XX),
     * because conditional jumps consume stack values. Inserting code before them
     * would corrupt the stack state and cause VerifyError.
     *
     * Original: GOTO target
     *
     * After:    GOTO verify_block
     *           verify_block:
     *             push computed_hash
     *             push expected_hash
     *             IF_ICMPEQ target
     *             ATHROW (exception path - dead code)
     */
    private void applyHashVerificationTransformation(ClassNode classNode, MethodNode methodNode) {
        if (methodNode.instructions.size() > 3000) return;

        // Only transform GOTO instructions (safe - no stack values consumed)
        // DO NOT transform conditional jumps as they consume stack values
        List<JumpInsnNode> gotos = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn.getOpcode() == GOTO)
            .filter(insn -> originalJumps.contains(insn))  // Only process original jumps
            .map(insn -> (JumpInsnNode) insn)
            .filter(jump -> !isBackwardJump(methodNode, jump))
            .filter(jump -> ThreadLocalRandom.current().nextDouble() < HASH_VERIFY_PROBABILITY)
            .collect(Collectors.toList());

        for (JumpInsnNode jump : gotos) {
            insertHashVerification(classNode, methodNode, jump);
        }
    }

    private boolean isConditionalJump(int opcode) {
        return (opcode >= IFEQ && opcode <= IF_ACMPNE) || opcode == IFNULL || opcode == IFNONNULL;
    }

    private void insertHashVerification(ClassNode classNode, MethodNode methodNode, JumpInsnNode gotoInsn) {
        LabelNode originalTarget = gotoInsn.label;
        Integer targetHash = blockHashes.get(originalTarget);
        if (targetHash == null) {
            targetHash = computeBlockHash(methodSeed, ThreadLocalRandom.current().nextInt(1000));
        }

        LabelNode verifyBlockLabel = new LabelNode();
        LabelNode exceptionLabel = new LabelNode();

        InsnList verifyBlock = new InsnList();
        verifyBlock.add(verifyBlockLabel);

        // Compute hash expression that equals targetHash
        int seed = blockSeeds.getOrDefault(originalTarget, methodSeed);
        verifyBlock.add(createHashExpression(classNode, seed, targetHash));

        // Compare with expected hash - if equal, jump to original target
        verifyBlock.add(ASMUtils.pushInt(targetHash));
        verifyBlock.add(new JumpInsnNode(IF_ICMPEQ, originalTarget));

        // Exception path (dead code - hash always matches)
        verifyBlock.add(exceptionLabel);
        verifyBlock.add(createExceptionThrow());

        // Insert verify block after the GOTO
        methodNode.instructions.insert(gotoInsn, verifyBlock);

        // Redirect GOTO to verify block
        gotoInsn.label = verifyBlockLabel;
    }

    private InsnList createHashExpression(ClassNode classNode, int seed, int expectedHash) {
        InsnList expr = new InsnList();

        int pattern = ThreadLocalRandom.current().nextInt(6);

        switch (pattern) {
            case 0 -> {
                // XOR pattern: (seed ^ magic) == expectedHash
                int magic = seed ^ expectedHash;
                expr.add(ASMUtils.pushInt(seed));
                expr.add(ASMUtils.pushInt(magic));
                expr.add(new InsnNode(IXOR));
            }
            case 1 -> {
                // Add pattern: (seed + offset) == expectedHash
                int offset = expectedHash - seed;
                expr.add(ASMUtils.pushInt(seed));
                expr.add(ASMUtils.pushInt(offset));
                expr.add(new InsnNode(IADD));
            }
            case 2 -> {
                // XOR + Add: ((seed ^ v1) + v2) == expectedHash
                int v1 = ThreadLocalRandom.current().nextInt();
                int v2 = expectedHash - (seed ^ v1);
                expr.add(ASMUtils.pushInt(seed));
                expr.add(ASMUtils.pushInt(v1));
                expr.add(new InsnNode(IXOR));
                expr.add(ASMUtils.pushInt(v2));
                expr.add(new InsnNode(IADD));
            }
            case 3 -> {
                // Inline constant XOR: (key ^ value) == expectedHash
                int value = xorKey1 ^ expectedHash;
                expr.add(ASMUtils.pushInt(xorKey1));
                expr.add(ASMUtils.pushInt(value));
                expr.add(new InsnNode(IXOR));
            }
            case 4 -> {
                // Rotate + XOR
                int v1 = ThreadLocalRandom.current().nextInt();
                int rotated = Integer.rotateLeft(seed, 7);
                int v2 = expectedHash - (rotated ^ v1);
                expr.add(ASMUtils.pushInt(seed));
                expr.add(ASMUtils.pushInt(7));
                expr.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false));
                expr.add(ASMUtils.pushInt(v1));
                expr.add(new InsnNode(IXOR));
                expr.add(ASMUtils.pushInt(v2));
                expr.add(new InsnNode(IADD));
            }
            case 5 -> {
                // Double key XOR
                int value = xorKey1 ^ xorKey2 ^ expectedHash;
                expr.add(ASMUtils.pushInt(xorKey1));
                expr.add(ASMUtils.pushInt(xorKey2));
                expr.add(new InsnNode(IXOR));
                expr.add(ASMUtils.pushInt(value));
                expr.add(new InsnNode(IXOR));
            }
        }

        return expr;
    }

    private InsnList createExceptionThrow() {
        InsnList insns = new InsnList();
        String[] exceptions = {
            "java/lang/RuntimeException",
            "java/lang/IllegalStateException",
            "java/lang/Error"
        };
        String exType = exceptions[ThreadLocalRandom.current().nextInt(exceptions.length)];

        int pattern = ThreadLocalRandom.current().nextInt(3);
        switch (pattern) {
            case 0 -> {
                insns.add(new TypeInsnNode(NEW, exType));
                insns.add(new InsnNode(DUP));
                insns.add(new MethodInsnNode(INVOKESPECIAL, exType, "<init>", "()V", false));
                insns.add(new InsnNode(ATHROW));
            }
            case 1 -> {
                insns.add(new InsnNode(ACONST_NULL));
                insns.add(new InsnNode(ATHROW));
            }
            case 2 -> {
                insns.add(new TypeInsnNode(NEW, exType));
                insns.add(new InsnNode(DUP));
                insns.add(new LdcInsnNode(String.valueOf(ThreadLocalRandom.current().nextLong())));
                insns.add(new MethodInsnNode(INVOKESPECIAL, exType, "<init>", "(Ljava/lang/String;)V", false));
                insns.add(new InsnNode(ATHROW));
            }
        }
        return insns;
    }

    // ==================== Phase 3: XOR Chain Transformation ====================

    /**
     * XOR Chain Encryption: Multi-layer XOR on integer constants
     *
     * WARNING: Skip small constants (-1, 0, 1, 2) as they are often used in:
     * - Array index calculations
     * - Loop counters
     * - Boolean operations
     * - Precision-sensitive calculations
     *
     * Original: LDC value
     *
     * After:    LDC encrypted_value
     *           push xorKey1
     *           IXOR
     *           push xorKey2
     *           IXOR
     *           ... (chain depth)
     */
    private void applyXORChainTransformation(ClassNode classNode, MethodNode methodNode) {
        if (methodNode.instructions.size() > 3000) return;

        // Collect integer constants, excluding small values that are precision-sensitive
        List<AbstractInsnNode> intConstants = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> isIntegerConstant(insn))
            .filter(insn -> !isSmallConstant(insn)) // Skip small constants
            .filter(insn -> ThreadLocalRandom.current().nextDouble() < XOR_CHAIN_PROBABILITY)
            .collect(Collectors.toList());

        for (AbstractInsnNode insn : intConstants) {
            transformIntegerConstant(classNode, methodNode, insn);
        }
    }

    private boolean isIntegerConstant(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        if (opcode >= ICONST_M1 && opcode <= ICONST_5) return true;
        if (opcode == BIPUSH || opcode == SIPUSH) return true;
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer) return true;
        return false;
    }

    private boolean isSmallConstant(AbstractInsnNode insn) {
        int value = getIntegerValue(insn);
        // Skip constants that are commonly used in precision-sensitive operations
        // and common loop bounds/array sizes
        // -1, 0, 1, 2, 3 are often used in loop counters, array indices, boolean ops
        // Small positive values up to 10 are common loop bounds
        // Also skip common comparison values and powers of 2
        if (value >= -1 && value <= 10) return true;
        if (value == 100 || value == 1000 || value == 10000) return true; // Common loop bounds
        if ((value & (value - 1)) == 0 && value > 0) return true; // Powers of 2
        return false;
    }

    private int getIntegerValue(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        if (opcode >= ICONST_M1 && opcode <= ICONST_5) return opcode - ICONST_0;
        if (opcode == BIPUSH || opcode == SIPUSH) return ((IntInsnNode) insn).operand;
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer) return (Integer) ldc.cst;
        return 0;
    }

    private void transformIntegerConstant(ClassNode classNode, MethodNode methodNode, AbstractInsnNode insn) {
        int originalValue = getIntegerValue(insn);

        // Build XOR chain encryption using inline constants
        int encryptedValue = originalValue;
        List<Integer> usedKeys = new ArrayList<>();

        // Add XOR layers based on chain depth
        for (int i = 0; i < XOR_CHAIN_DEPTH; i++) {
            int key = (i % 2 == 0) ? xorKey1 : xorKey2;
            encryptedValue ^= key;
            usedKeys.add(key);
        }

        // Build decryption chain
        InsnList decryption = new InsnList();

        // Push encrypted value
        decryption.add(ASMUtils.pushInt(encryptedValue));

        // Add XOR operations in reverse order using inline constants
        Collections.reverse(usedKeys);
        for (int key : usedKeys) {
            decryption.add(ASMUtils.pushInt(key));
            decryption.add(new InsnNode(IXOR));
        }

        // Replace original instruction
        methodNode.instructions.insert(insn, decryption);
        methodNode.instructions.remove(insn);
    }

    // ==================== Phase 4: Lookup Switch Transformation ====================

    /**
     * Lookup Switch Dispatch: Replace GOTO jumps with switch dispatch
     *
     * NOTE: This transformation only works safely on GOTO instructions,
     * not on conditional jumps (IF_XX), because conditional jumps consume
     * stack values that cannot be easily duplicated or preserved.
     *
     * WARNING: Skip for inner classes and static initializers as they may
     * have stricter verification requirements.
     *
     * Original: GOTO target
     *
     * After:    compute_case_value
     *           LOOKUPSWITCH {
     *             case correct_key: GOTO target
     *             case fake_key1: GOTO fake_handler1
     *             case fake_key2: GOTO fake_handler2
     *             case fake_key3: GOTO fake_handler3
     *             default: GOTO exception_block
     *           }
     *           fake_handlers: (dead code)
     *           exception_block: ATHROW
     */
    private void applyLookupSwitchTransformation(ClassNode classNode, MethodNode methodNode) {
        if (methodNode.instructions.size() > 2500) return;

        // Skip inner classes - they may have stricter classloader requirements
        if (classNode.name.contains("$")) return;

        // Skip static initializers
        if (methodNode.name.equals("<clinit>")) return;

        // Only transform original GOTO instructions (safe - no stack values consumed)
        List<JumpInsnNode> gotos = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn.getOpcode() == GOTO)
            .filter(insn -> originalJumps.contains(insn))  // Only process original jumps
            .map(insn -> (JumpInsnNode) insn)
            .filter(jump -> !isBackwardJump(methodNode, jump))
            .filter(jump -> ThreadLocalRandom.current().nextDouble() < SWITCH_DISPATCH_PROBABILITY)
            .collect(Collectors.toList());

        for (JumpInsnNode jump : gotos) {
            transformGotoToLookupSwitch(methodNode, jump);
        }
    }

    private void transformGotoToLookupSwitch(MethodNode methodNode, JumpInsnNode gotoInsn) {
        LabelNode originalTarget = gotoInsn.label;

        // Create labels
        LabelNode exceptionLabel = new LabelNode();
        LabelNode[] fakeLabels = new LabelNode[SWITCH_FAKE_CASES];
        for (int i = 0; i < SWITCH_FAKE_CASES; i++) {
            fakeLabels[i] = new LabelNode();
        }

        // Generate keys
        int correctKey = ThreadLocalRandom.current().nextInt(1000);
        Set<Integer> usedKeys = new HashSet<>();
        usedKeys.add(correctKey);

        int[] keys = new int[SWITCH_FAKE_CASES + 1];
        LabelNode[] labels = new LabelNode[SWITCH_FAKE_CASES + 1];

        // Assign correct key
        int correctIndex = ThreadLocalRandom.current().nextInt(SWITCH_FAKE_CASES + 1);

        int keyIndex = 0;
        for (int i = 0; i < SWITCH_FAKE_CASES + 1; i++) {
            if (i == correctIndex) {
                keys[i] = correctKey;
                labels[i] = originalTarget;  // Direct jump to target
            } else {
                int fakeKey;
                do {
                    fakeKey = ThreadLocalRandom.current().nextInt(1000);
                } while (usedKeys.contains(fakeKey));
                usedKeys.add(fakeKey);
                keys[i] = fakeKey;
                labels[i] = fakeLabels[keyIndex++];
            }
        }

        // Sort by keys for lookupswitch
        Integer[] indices = new Integer[keys.length];
        for (int i = 0; i < keys.length; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingInt(a -> keys[a]));

        int[] sortedKeys = new int[keys.length];
        LabelNode[] sortedLabels = new LabelNode[keys.length];
        for (int i = 0; i < keys.length; i++) {
            sortedKeys[i] = keys[indices[i]];
            sortedLabels[i] = labels[indices[i]];
        }

        // Build switch block
        InsnList switchBlock = new InsnList();

        // Compute case value that results in correctKey
        switchBlock.add(createSwitchKeyComputation(correctKey, methodSeed));

        // Lookup switch
        switchBlock.add(new LookupSwitchInsnNode(exceptionLabel, sortedKeys, sortedLabels));

        // Fake handler blocks (dead code) - jump to exception to avoid infinite loops
        for (int i = 0; i < SWITCH_FAKE_CASES; i++) {
            switchBlock.add(fakeLabels[i]);
            switchBlock.add(createSafeFakeHandler(exceptionLabel));
        }

        // Exception block
        switchBlock.add(exceptionLabel);
        switchBlock.add(createExceptionThrow());

        // Replace GOTO with switch block
        methodNode.instructions.insert(gotoInsn, switchBlock);
        methodNode.instructions.remove(gotoInsn);
    }

    private InsnList createSwitchKeyComputation(int targetKey, int seed) {
        InsnList insns = new InsnList();

        int pattern = ThreadLocalRandom.current().nextInt(4);

        switch (pattern) {
            case 0 -> {
                // Direct: push targetKey
                insns.add(ASMUtils.pushInt(targetKey));
            }
            case 1 -> {
                // XOR: (seed ^ value) == targetKey
                int value = seed ^ targetKey;
                insns.add(ASMUtils.pushInt(seed));
                insns.add(ASMUtils.pushInt(value));
                insns.add(new InsnNode(IXOR));
            }
            case 2 -> {
                // Add: (base + offset) == targetKey
                int base = ThreadLocalRandom.current().nextInt(500);
                int offset = targetKey - base;
                insns.add(ASMUtils.pushInt(base));
                insns.add(ASMUtils.pushInt(offset));
                insns.add(new InsnNode(IADD));
            }
            case 3 -> {
                // Modulo: (value % 1000) == targetKey
                int value = targetKey + (ThreadLocalRandom.current().nextInt(10) * 1000);
                insns.add(ASMUtils.pushInt(value));
                insns.add(ASMUtils.pushInt(1000));
                insns.add(new InsnNode(IREM));
            }
        }

        return insns;
    }

    private InsnList createSafeFakeHandler(LabelNode exceptionLabel) {
        InsnList insns = new InsnList();

        int pattern = ThreadLocalRandom.current().nextInt(3);

        switch (pattern) {
            case 0 -> {
                // Field manipulation then exception
                insns.add(new LdcInsnNode("fake"));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false));
                insns.add(new InsnNode(POP));
                insns.add(new JumpInsnNode(GOTO, exceptionLabel));
            }
            case 1 -> {
                // Math operation then exception
                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "bitCount", "(I)I", false));
                insns.add(new InsnNode(POP));
                insns.add(new JumpInsnNode(GOTO, exceptionLabel));
            }
            case 2 -> {
                // Direct exception jump
                insns.add(new JumpInsnNode(GOTO, exceptionLabel));
            }
        }

        return insns;
    }

    // ==================== Phase 5: Exception Trap Transformation ====================

    /**
     * Exception Trap: Convert GOTO to exception-based flow
     *
     * WARNING: This transformation adds try-catch blocks which can interfere with:
     * - Methods that already have try-catch (can cause nested exception handling issues)
     * - Methods with annotations (annotations may expect specific bytecode structure)
     * - Inner class methods (may have complex control flow)
     * - SecurityManager related methods
     *
     * Original: GOTO target
     *
     * After:    GOTO throw_block
     *           throw_block:
     *             opaque_check -> dispatcher
     *             dead_code
     *           dispatcher:
     *             NEW Exception
     *             DUP
     *             INVOKESPECIAL <init>
     *             ATHROW
     *           [try_end]
     *           handler:
     *             POP
     *             GOTO target
     */
    private void applyExceptionTrapTransformation(ClassNode classNode, MethodNode methodNode) {
        if (methodNode.instructions.size() > 2000) return;

        // Skip methods with existing try-catch blocks - adding more can cause issues
        if (methodHasTryCatch(methodNode)) return;

        // Skip methods with annotations - they may have special requirements
        if (methodHasAnnotations(methodNode)) return;

        // Skip inner class methods (class name contains $)
        if (classNode.name.contains("$")) return;

        // Skip security-related methods
        if (methodUsesSecurity(methodNode)) return;

        // Collect original GOTO instructions only
        List<JumpInsnNode> gotos = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn.getOpcode() == GOTO)
            .filter(insn -> originalJumps.contains(insn))  // Only process original jumps
            .map(insn -> (JumpInsnNode) insn)
            .filter(jump -> !isBackwardJump(methodNode, jump))
            .filter(jump -> ThreadLocalRandom.current().nextDouble() < EXCEPTION_TRAP_PROBABILITY)
            .collect(Collectors.toList());

        for (JumpInsnNode gotoInsn : gotos) {
            transformGotoToExceptionFlow(classNode, methodNode, gotoInsn);
        }
    }

    private boolean methodUsesSecurity(MethodNode methodNode) {
        return Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn instanceof MethodInsnNode)
            .map(insn -> (MethodInsnNode) insn)
            .anyMatch(min -> min.owner.contains("Security") ||
                min.owner.contains("security") ||
                min.owner.equals("java/lang/System") && min.name.equals("getSecurityManager") ||
                min.owner.contains("ClassLoader") ||
                min.owner.contains("AccessController"));
    }

    private void transformGotoToExceptionFlow(ClassNode classNode, MethodNode methodNode, JumpInsnNode gotoInsn) {
        LabelNode originalTarget = gotoInsn.label;

        // Create labels
        LabelNode throwBlockLabel = new LabelNode();
        LabelNode dispatcherLabel = new LabelNode();
        LabelNode tryEndLabel = new LabelNode();
        LabelNode handlerLabel = new LabelNode();

        // Build throw block
        InsnList throwBlock = new InsnList();
        throwBlock.add(throwBlockLabel);

        // Opaque predicate check
        int seed = blockSeeds.getOrDefault(originalTarget, methodSeed);
        int expectedHash = computeBlockHash(seed, methodCounter);
        throwBlock.add(createHashExpression(classNode, seed, expectedHash));
        throwBlock.add(ASMUtils.pushInt(expectedHash));
        throwBlock.add(new JumpInsnNode(IF_ICMPEQ, dispatcherLabel));

        // Dead code
        throwBlock.add(new InsnNode(ACONST_NULL));
        throwBlock.add(new InsnNode(ATHROW));

        // Dispatcher - throw exception
        throwBlock.add(dispatcherLabel);
        String exceptionType = getRandomRuntimeException();
        throwBlock.add(new TypeInsnNode(NEW, exceptionType));
        throwBlock.add(new InsnNode(DUP));
        throwBlock.add(new MethodInsnNode(INVOKESPECIAL, exceptionType, "<init>", "()V", false));
        throwBlock.add(new InsnNode(ATHROW));
        throwBlock.add(tryEndLabel);

        // Handler - catch and jump to target
        throwBlock.add(handlerLabel);
        throwBlock.add(new InsnNode(POP));
        throwBlock.add(new JumpInsnNode(GOTO, originalTarget));

        // Insert throw block after GOTO
        methodNode.instructions.insert(gotoInsn, throwBlock);

        // Redirect GOTO to throw block
        gotoInsn.label = throwBlockLabel;

        // Add try-catch block
        TryCatchBlockNode tryCatch = new TryCatchBlockNode(
            throwBlockLabel,
            tryEndLabel,
            handlerLabel,
            exceptionType
        );
        methodNode.tryCatchBlocks.add(tryCatch);
    }

    private String getRandomRuntimeException() {
        String[] exceptions = {
            "java/lang/RuntimeException",
            "java/lang/IllegalStateException",
            "java/lang/IllegalArgumentException",
            "java/lang/ArrayStoreException"
        };
        return exceptions[ThreadLocalRandom.current().nextInt(exceptions.length)];
    }

    // ==================== Phase 6: Conditional Branch Obfuscation ====================

    /**
     * Conditional Branch Obfuscation: Convert IF_XX jumps to data-dependent indirect jumps
     *
     * This technique transforms conditional jumps (IF_ICMPNE, etc.) into
     * switch-based dispatch where the branch outcome determines a key value.
     *
     * Original: IF_ICMPNE label
     *           (fallthrough code)
     *
     * After:    IF_ICMPNE -> push TRUE_KEY
     *           (else)    -> push FALSE_KEY
     *           LOOKUPSWITCH {
     *             TRUE_KEY  -> label
     *             FALSE_KEY -> fallthrough_label
     *             JUNK_KEY1 -> dead_code1
     *             JUNK_KEY2 -> dead_code2
     *           }
     *           fallthrough_label:
     *           (original fallthrough code)
     *
     * NOTE: Only processes conditional jumps where stack is empty (safe transformation)
     */
    private void applyConditionalBranchTransformation(ClassNode classNode, MethodNode methodNode) {
        if (methodNode.instructions.size() > 2500) return;

        // Skip inner classes and static initializers
        if (classNode.name.contains("$")) return;
        if (methodNode.name.equals("<clinit>")) return;

        // Skip methods with recursive calls - they are sensitive to control flow changes
        if (methodHasRecursiveCall(classNode, methodNode)) return;

        // Re-analyze frames after previous transformations
        try {
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            frames = analyzer.analyze(classNode.name, methodNode);
            insnIndexMap.clear();
            int idx = 0;
            for (AbstractInsnNode insn : methodNode.instructions) {
                insnIndexMap.put(insn, idx++);
            }
        } catch (AnalyzerException e) {
            return; // Skip if analysis fails
        }

        // Collect conditional jumps that are safe to transform (stack empty)
        // Also skip backward jumps to avoid issues with loops
        List<JumpInsnNode> conditionalJumps = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn instanceof JumpInsnNode)
            .filter(insn -> isConditionalJump(insn.getOpcode()))
            .filter(insn -> originalJumps.contains(insn))
            .map(insn -> (JumpInsnNode) insn)
            .filter(jump -> !isBackwardJump(methodNode, jump))
            .filter(this::isStackEmptyAfterJump)
            .filter(jump -> ThreadLocalRandom.current().nextDouble() < CONDITIONAL_BRANCH_PROBABILITY)
            .collect(Collectors.toList());

        for (JumpInsnNode jump : conditionalJumps) {
            transformConditionalBranch(classNode, methodNode, jump);
        }
    }

    /**
     * Check if method contains recursive calls (calls to itself)
     */
    private boolean methodHasRecursiveCall(ClassNode classNode, MethodNode methodNode) {
        return Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn instanceof MethodInsnNode)
            .map(insn -> (MethodInsnNode) insn)
            .anyMatch(min -> min.owner.equals(classNode.name) && min.name.equals(methodNode.name));
    }

    /**
     * Check if stack is empty after the conditional jump (at fallthrough point)
     */
    private boolean isStackEmptyAfterJump(JumpInsnNode jump) {
        if (frames == null) return false; // Be conservative
        Integer index = insnIndexMap.get(jump);
        if (index == null || index + 1 >= frames.length) return false;
        Frame<BasicValue> frame = frames[index + 1];
        return frame != null && frame.getStackSize() == 0;
    }

    private void transformConditionalBranch(ClassNode classNode, MethodNode methodNode, JumpInsnNode condJump) {
        LabelNode originalTarget = condJump.label;
        int originalOpcode = condJump.getOpcode();

        // Create labels
        LabelNode trueBranchLabel = new LabelNode();
        LabelNode fallthroughLabel = new LabelNode();
        LabelNode switchLabel = new LabelNode();
        LabelNode exceptionLabel = new LabelNode();

        // Generate keys
        int trueKey = ThreadLocalRandom.current().nextInt(1000);
        int falseKey;
        do {
            falseKey = ThreadLocalRandom.current().nextInt(1000);
        } while (falseKey == trueKey);

        Set<Integer> usedKeys = new HashSet<>();
        usedKeys.add(trueKey);
        usedKeys.add(falseKey);

        // Generate fake keys
        int[] fakeKeys = new int[CONDITIONAL_FAKE_CASES];
        LabelNode[] fakeLabels = new LabelNode[CONDITIONAL_FAKE_CASES];
        for (int i = 0; i < CONDITIONAL_FAKE_CASES; i++) {
            int fakeKey;
            do {
                fakeKey = ThreadLocalRandom.current().nextInt(1000);
            } while (usedKeys.contains(fakeKey));
            usedKeys.add(fakeKey);
            fakeKeys[i] = fakeKey;
            fakeLabels[i] = new LabelNode();
        }

        // Build all keys and labels arrays
        int totalCases = 2 + CONDITIONAL_FAKE_CASES;
        int[] allKeys = new int[totalCases];
        LabelNode[] allLabels = new LabelNode[totalCases];

        allKeys[0] = trueKey;
        allLabels[0] = originalTarget;  // True branch goes to original target
        allKeys[1] = falseKey;
        allLabels[1] = fallthroughLabel;  // False branch goes to fallthrough
        for (int i = 0; i < CONDITIONAL_FAKE_CASES; i++) {
            allKeys[2 + i] = fakeKeys[i];
            allLabels[2 + i] = fakeLabels[i];
        }

        // Sort by keys for lookupswitch
        Integer[] indices = new Integer[totalCases];
        for (int i = 0; i < totalCases; i++) indices[i] = i;
        Arrays.sort(indices, Comparator.comparingInt(a -> allKeys[a]));

        int[] sortedKeys = new int[totalCases];
        LabelNode[] sortedLabels = new LabelNode[totalCases];
        for (int i = 0; i < totalCases; i++) {
            sortedKeys[i] = allKeys[indices[i]];
            sortedLabels[i] = allLabels[indices[i]];
        }

        // Build transformation block
        InsnList transformBlock = new InsnList();

        // Branch: if condition true, push trueKey, else push falseKey
        transformBlock.add(new JumpInsnNode(originalOpcode, trueBranchLabel));

        // False path: push falseKey using field hash computation
        if (hashFieldCreated && ThreadLocalRandom.current().nextDouble() < FIELD_HASH_PROBABILITY) {
            transformBlock.add(createFieldHashComputation(classNode, falseKey));
        } else {
            transformBlock.add(ASMUtils.pushInt(falseKey));
        }
        transformBlock.add(new JumpInsnNode(GOTO, switchLabel));

        // True path: push trueKey using field hash computation
        transformBlock.add(trueBranchLabel);
        if (hashFieldCreated && ThreadLocalRandom.current().nextDouble() < FIELD_HASH_PROBABILITY) {
            transformBlock.add(createFieldHashComputation(classNode, trueKey));
        } else {
            transformBlock.add(ASMUtils.pushInt(trueKey));
        }

        // Switch dispatch
        transformBlock.add(switchLabel);
        transformBlock.add(new LookupSwitchInsnNode(exceptionLabel, sortedKeys, sortedLabels));

        // Fake handlers (dead code) - jump to exception
        for (int i = 0; i < CONDITIONAL_FAKE_CASES; i++) {
            transformBlock.add(fakeLabels[i]);
            transformBlock.add(new JumpInsnNode(GOTO, exceptionLabel));
        }

        // Exception block
        transformBlock.add(exceptionLabel);
        transformBlock.add(createExceptionThrow());

        // Fallthrough label (after exception block, but execution continues here for false branch)
        transformBlock.add(fallthroughLabel);

        // Replace conditional jump with transformation block
        methodNode.instructions.insert(condJump, transformBlock);
        methodNode.instructions.remove(condJump);
    }

    /**
     * Create field hash computation that results in targetKey
     * Uses: (hashFieldValue XOR magic) == targetKey
     */
    private InsnList createFieldHashComputation(ClassNode classNode, int targetKey) {
        InsnList insns = new InsnList();

        int pattern = ThreadLocalRandom.current().nextInt(3);

        switch (pattern) {
            case 0 -> {
                // Get field.hashCode() XOR magic = targetKey
                int magic = String.valueOf(hashFieldValue).hashCode() ^ targetKey;
                insns.add(new FieldInsnNode(GETSTATIC, classNode.name, HASH_FIELD_NAME, "Ljava/lang/String;"));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false));
                insns.add(ASMUtils.pushInt(magic));
                insns.add(new InsnNode(IXOR));
            }
            case 1 -> {
                // Get field.length() + offset = targetKey
                int fieldLength = String.valueOf(hashFieldValue).length();
                int offset = targetKey - fieldLength;
                insns.add(new FieldInsnNode(GETSTATIC, classNode.name, HASH_FIELD_NAME, "Ljava/lang/String;"));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
                insns.add(ASMUtils.pushInt(offset));
                insns.add(new InsnNode(IADD));
            }
            case 2 -> {
                // Direct with XOR confusion
                int magic = xorKey1 ^ targetKey;
                insns.add(ASMUtils.pushInt(xorKey1));
                insns.add(ASMUtils.pushInt(magic));
                insns.add(new InsnNode(IXOR));
            }
        }

        return insns;
    }

    // ==================== Configuration ====================

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getControlFlowObfuscation() == this.getEnableType().type(),
            BozarConfig.BozarOptions.ControlFlowObfuscationOption.SUPER
        );
    }
}
