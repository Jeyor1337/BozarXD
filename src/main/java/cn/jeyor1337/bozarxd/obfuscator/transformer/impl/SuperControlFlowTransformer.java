package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ControlFlowTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.*;

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
    private static final int XOR_CHAIN_DEPTH = 3;
    private static final double XOR_CHAIN_PROBABILITY = 0.45;

    // Lookup switch configuration
    private static final int SWITCH_FAKE_CASES = 3;
    private static final double SWITCH_DISPATCH_PROBABILITY = 0.40;

    // Exception trap configuration
    private static final double EXCEPTION_TRAP_PROBABILITY = 0.35;

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

    // ==================== Method-Level State ====================

    // Method-level seed
    private int methodSeed = 0;

    // Block seeds map (label -> seed)
    private Map<LabelNode, Integer> blockSeeds = new HashMap<>();

    // Block hashes map (label -> hash)
    private Map<LabelNode, Integer> blockHashes = new HashMap<>();

    // Track original jump instructions to avoid processing newly created ones
    private Set<AbstractInsnNode> originalJumps = new HashSet<>();

    // ==================== Lifecycle Methods ====================

    @Override
    public void transformClass(ClassNode classNode) {
        if (!ASMUtils.isClassEligibleToModify(classNode)) return;

        this.currentClass = classNode;
        this.methodCounter = 0;

        // Generate class-level seed
        this.classSeed = ThreadLocalRandom.current().nextInt();

        // Generate XOR keys (no field creation)
        this.xorKey1 = ThreadLocalRandom.current().nextInt();
        this.xorKey2 = ThreadLocalRandom.current().nextInt();
        this.hashKey = ThreadLocalRandom.current().nextInt();
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
        if (!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) return;
        if (shouldSkipMethod(methodNode)) return;

        // Initialize method-level state
        this.methodCounter++;
        this.methodSeed = classSeed ^ (methodCounter * HASH_PRIME_1);
        this.blockSeeds.clear();
        this.blockHashes.clear();
        this.originalJumps.clear();

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
        // Skip small methods
        if (methodNode.instructions.size() < 15) return true;

        // Skip special methods
        if ((methodNode.access & (ACC_BRIDGE | ACC_SYNTHETIC | ACC_ABSTRACT | ACC_NATIVE)) != 0) return true;

        // Skip reflection-heavy methods
        return methodUsesReflection(methodNode);
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
        // -1, 0, 1, 2 are often used in loop counters, array indices, boolean ops
        return value >= -1 && value <= 2;
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

    // ==================== Configuration ====================

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getControlFlowObfuscation() == this.getEnableType().type(),
            BozarConfig.BozarOptions.ControlFlowObfuscationOption.SUPER
        );
    }
}
