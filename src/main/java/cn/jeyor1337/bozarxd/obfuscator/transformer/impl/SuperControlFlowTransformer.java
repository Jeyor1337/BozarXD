package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ControlFlowTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.InsnBuilder;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Super Control Flow Obfuscator - Inspired by Skidfuscator
 *
 * Implements multiple pure-ASM control flow obfuscation techniques:
 * 1. Exception-based control flow (BasicRangeTransformer)
 * 2. Opaque predicate guards
 * 3. Jump chaining (indirect jumps)
 * 4. Bogus conditional branches
 * 5. Stack-based control flow obfuscation
 *
 * Features:
 * - All techniques use only ASM (no Maple IR dependency)
 * - Preserves loop structures (skips backward jumps)
 * - No additional fields added to classes (reflection-safe)
 * - Multiple obfuscation patterns for variety
 */
public class SuperControlFlowTransformer extends ControlFlowTransformer {

    public SuperControlFlowTransformer(Bozar bozar) {
        super(bozar, "Super Control Flow", BozarCategory.ADVANCED);
    }

    // Hash seed for opaque predicates
    private long hashSeed = 0;
    private int methodCounter = 0;

    // Obfuscation intensity (higher = more aggressive)
    private static final int JUMP_CHAIN_MAX_LENGTH = 3;
    private static final double BOGUS_JUMP_PROBABILITY = 0.3;
    private static final double JUMP_CHAIN_PROBABILITY = 0.25;

    @Override
    public void transformClass(ClassNode classNode) {
        if(!ASMUtils.isClassEligibleToModify(classNode)) return;

        // Initialize hash seed for this class
        // We don't add any fields to avoid breaking reflection-based field counting
        this.hashSeed = ThreadLocalRandom.current().nextLong();
    }

    @Override
    public void post() {
        // Clean up
        this.methodCounter = 0;
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if(!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) return;

        // Skip methods that might be problematic
        if(shouldSkipMethod(methodNode)) return;

        methodCounter++;

        // Apply multiple control flow obfuscation techniques
        // Each technique is applied probabilistically for variety

        // 1. Exception-based range transformation to GOTO instructions
        applyRangeTransformation(classNode, methodNode);

        // 2. Jump chaining - split single jumps into chains
        applyJumpChaining(methodNode);

        // 3. Bogus conditional jumps - add fake branches
        applyBogusConditionalJumps(methodNode);

        // 4. Opaque predicate guards to method invocations and field accesses
        applyInvokeObfuscation(classNode, methodNode);

        // 5. Stack-based control flow obfuscation
        applyStackBasedObfuscation(methodNode);
    }

    /**
     * Check if method should be skipped to avoid breaking functionality
     */
    private boolean shouldSkipMethod(MethodNode methodNode) {
        // Skip very small methods (less than 10 instructions to be safer)
        if(methodNode.instructions.size() < 10) return true;

        // Skip methods with existing try-catch blocks to avoid interference
        if(methodNode.tryCatchBlocks != null && !methodNode.tryCatchBlocks.isEmpty()) return true;

        // Skip bridge, synthetic, abstract, and native methods
        if((methodNode.access & ACC_BRIDGE) != 0 ||
           (methodNode.access & ACC_SYNTHETIC) != 0 ||
           (methodNode.access & ACC_ABSTRACT) != 0 ||
           (methodNode.access & ACC_NATIVE) != 0) return true;

        // Skip annotation-related methods
        if(methodNode.visibleAnnotations != null || methodNode.invisibleAnnotations != null) return true;

        // Skip methods that use reflection (more precise detection)
        boolean usesReflection = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn instanceof MethodInsnNode)
            .map(insn -> (MethodInsnNode)insn)
            .anyMatch(min -> {
                String owner = min.owner;
                String name = min.name;
                // Only skip if directly using java.lang.reflect package or specific Class methods
                return owner.startsWith("java/lang/reflect/") ||
                       (owner.equals("java/lang/Class") &&
                        (name.equals("getFields") || name.equals("getDeclaredFields") ||
                         name.equals("getMethods") || name.equals("getDeclaredMethods") ||
                         name.equals("getField") || name.equals("getDeclaredField") ||
                         name.equals("getMethod") || name.equals("getDeclaredMethod") ||
                         name.equals("getAnnotation") || name.equals("getAnnotations") ||
                         name.equals("getDeclaredAnnotations") || name.equals("forName")));
            });

        if(usesReflection) return true;

        return false;
    }

    /**
     * Exception-based Range Transformation (from Skidfuscator's BasicRangeTransformer)
     * Converts simple GOTO instructions into exception-based control flow
     * while preserving loop structures by skipping backward jumps
     */
    private void applyRangeTransformation(ClassNode classNode, MethodNode methodNode) {
        // Skip if method is too small or too large
        int instructionCount = methodNode.instructions.size();
        if(instructionCount < 10 || instructionCount > 5000) return;

        // Collect all GOTO instructions
        List<JumpInsnNode> gotoInsns = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn.getOpcode() == GOTO)
            .map(insn -> (JumpInsnNode)insn)
            .collect(Collectors.toList());

        // Apply transformation to random subset (40% chance per GOTO for balance)
        // Skip loop back-edges to preserve loop structures
        gotoInsns.stream()
            .filter(insn -> ThreadLocalRandom.current().nextDouble() < 0.4)
            .filter(gotoInsn -> !isLoopBackEdge(methodNode, gotoInsn))
            .forEach(gotoInsn -> transformGotoToException(classNode, methodNode, gotoInsn));
    }

    /**
     * Check if a GOTO instruction is a loop back-edge (jumps to an earlier instruction)
     * Loop back-edges are critical for loop structure and should not be transformed
     */
    private boolean isLoopBackEdge(MethodNode methodNode, JumpInsnNode gotoInsn) {
        int gotoIndex = methodNode.instructions.indexOf(gotoInsn);
        int targetIndex = methodNode.instructions.indexOf(gotoInsn.label);

        // If target is before the GOTO, it's a backward jump (loop back-edge)
        return targetIndex < gotoIndex;
    }

    /**
     * Transform: GOTO target
     * Into: GOTO throwBridge -> throwDispatcher -> throw exception -> catch handler -> target
     */
    private void transformGotoToException(ClassNode classNode, MethodNode methodNode, JumpInsnNode gotoInsn) {
        LabelNode originalTarget = gotoInsn.label;

        // Create labels for exception-based flow
        LabelNode throwBridgeLabel = new LabelNode();
        LabelNode throwDispatcherLabel = new LabelNode();
        LabelNode tryCatchEndLabel = new LabelNode();
        LabelNode targetBridgeLabel = new LabelNode();

        // Generate opaque hash predicate
        OpaqueHash hash = generateOpaqueHash();

        // 1. Create THROW_BRIDGE block with opaque predicate
        InsnList throwBridge = new InsnList();
        throwBridge.add(throwBridgeLabel);
        // Opaque predicate: hash expression == expected hash (always true)
        throwBridge.add(hash.getHashExpression());
        throwBridge.add(ASMUtils.pushInt(hash.getExpectedValue()));
        throwBridge.add(new JumpInsnNode(IF_ICMPEQ, throwDispatcherLabel));
        // Dead code (never executed) to confuse decompilers
        throwBridge.add(new InsnNode(ACONST_NULL));
        throwBridge.add(new InsnNode(ATHROW));

        // 2. Create THROW_DISPATCHER block that throws exception
        InsnList throwDispatcher = new InsnList();
        throwDispatcher.add(throwDispatcherLabel);
        String exceptionType = getRandomExceptionType();
        throwDispatcher.add(new TypeInsnNode(NEW, exceptionType));
        throwDispatcher.add(new InsnNode(DUP));
        throwDispatcher.add(new MethodInsnNode(INVOKESPECIAL, exceptionType, "<init>", "()V", false));
        throwDispatcher.add(new InsnNode(ATHROW));
        throwDispatcher.add(tryCatchEndLabel);

        // 3. Create TARGET_BRIDGE block (exception handler)
        InsnList targetBridge = new InsnList();
        targetBridge.add(targetBridgeLabel);
        // Pop the caught exception
        targetBridge.add(new InsnNode(POP));
        // Jump to original target
        targetBridge.add(new JumpInsnNode(GOTO, originalTarget));

        // Combine all blocks into a single InsnList and insert at once
        InsnList combined = new InsnList();
        combined.add(throwBridge);
        combined.add(throwDispatcher);
        combined.add(targetBridge);

        // Insert all blocks after the GOTO instruction
        methodNode.instructions.insert(gotoInsn, combined);

        // Update the original GOTO to jump to throwBridge
        gotoInsn.label = throwBridgeLabel;

        // Add try-catch block (start at throwBridge, end before handler, handler at targetBridge)
        TryCatchBlockNode tryCatch = new TryCatchBlockNode(
            throwBridgeLabel,
            tryCatchEndLabel,
            targetBridgeLabel,
            exceptionType
        );
        methodNode.tryCatchBlocks.add(tryCatch);
    }

    /**
     * Apply opaque predicate guards to method invocations and field accesses
     */
    private void applyInvokeObfuscation(ClassNode classNode, MethodNode methodNode) {
        Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> ASMUtils.isInvokeMethod(insn, true) || insn.getOpcode() == NEW || ASMUtils.isFieldInsn(insn))
            .forEach(insn -> {
                // Add opaque predicate checks before important instructions
                if(ThreadLocalRandom.current().nextInt(10) < 4) { // 40% chance
                    InsnList before = new InsnList();
                    OpaqueHash hash = generateOpaqueHash();

                    LabelNode continueLabel = new LabelNode();
                    LabelNode confuseLabel = new LabelNode();

                    before.add(hash.getHashExpression());
                    before.add(ASMUtils.pushInt(hash.getExpectedValue()));
                    before.add(new JumpInsnNode(IF_ICMPEQ, continueLabel));

                    // Dead code branch
                    before.add(confuseLabel);
                    before.add(new InsnNode(ACONST_NULL));
                    before.add(new InsnNode(POP));

                    before.add(continueLabel);

                    methodNode.instructions.insertBefore(insn, before);
                }
            });
    }

    /**
     * Jump Chaining - Transform single jumps into chains of indirect jumps
     * Example: GOTO A -> GOTO B -> GOTO C -> GOTO A
     * This makes static analysis harder by adding indirection layers
     */
    private void applyJumpChaining(MethodNode methodNode) {
        if(methodNode.instructions.size() > 3000) return; // Skip large methods

        List<JumpInsnNode> conditionalJumps = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn instanceof JumpInsnNode)
            .map(insn -> (JumpInsnNode)insn)
            .filter(jump -> jump.getOpcode() >= IFEQ && jump.getOpcode() <= IF_ACMPNE) // Conditional jumps only
            .filter(jump -> !isLoopBackEdge(methodNode, jump))
            .collect(Collectors.toList());

        for(JumpInsnNode jump : conditionalJumps) {
            if(ThreadLocalRandom.current().nextDouble() > JUMP_CHAIN_PROBABILITY) continue;

            LabelNode originalTarget = jump.label;
            LabelNode currentLabel = originalTarget;

            // Create a chain of 2-3 intermediate jumps
            int chainLength = ThreadLocalRandom.current().nextInt(JUMP_CHAIN_MAX_LENGTH - 1) + 2;

            // Build chain in reverse order
            for(int i = 0; i < chainLength; i++) {
                LabelNode intermediateLabel = new LabelNode();
                InsnList chain = new InsnList();
                chain.add(intermediateLabel);

                // Add some neutral stack operations to confuse decompilers
                if(ThreadLocalRandom.current().nextBoolean()) {
                    chain.add(new InsnNode(ICONST_0));
                    chain.add(new InsnNode(POP));
                }

                chain.add(new JumpInsnNode(GOTO, currentLabel));

                // Insert chain before original target
                methodNode.instructions.insertBefore(originalTarget, chain);
                currentLabel = intermediateLabel;
            }

            // Update original jump to point to first intermediate
            jump.label = currentLabel;
        }
    }

    /**
     * Bogus Conditional Jumps - Insert fake conditional branches that never execute
     * These add complexity to the control flow graph without changing behavior
     */
    private void applyBogusConditionalJumps(MethodNode methodNode) {
        if(methodNode.instructions.size() > 3000) return;

        List<AbstractInsnNode> insertionPoints = new ArrayList<>();

        // Find good insertion points (after labels, before returns)
        for(AbstractInsnNode insn : methodNode.instructions) {
            if(insn instanceof LabelNode ||
               (insn.getOpcode() >= IRETURN && insn.getOpcode() <= RETURN)) {
                if(ThreadLocalRandom.current().nextDouble() < BOGUS_JUMP_PROBABILITY) {
                    insertionPoints.add(insn);
                }
            }
        }

        for(AbstractInsnNode insertPoint : insertionPoints) {
            InsnList bogus = createBogusConditionalBranch();

            if(insertPoint instanceof LabelNode) {
                methodNode.instructions.insert(insertPoint, bogus);
            } else {
                methodNode.instructions.insertBefore(insertPoint, bogus);
            }
        }
    }

    /**
     * Create a bogus conditional branch that always falls through
     * Pattern: if(opaque_false_predicate) { dead_code } else { continue }
     */
    private InsnList createBogusConditionalBranch() {
        InsnList bogus = new InsnList();
        LabelNode deadCodeLabel = new LabelNode();
        LabelNode continueLabel = new LabelNode();

        // Create an opaque false condition
        // Pattern: (X XOR X) != 0 (always false)
        int randomValue = ThreadLocalRandom.current().nextInt();

        bogus.add(ASMUtils.pushInt(randomValue));
        bogus.add(ASMUtils.pushInt(randomValue));
        bogus.add(new InsnNode(IXOR)); // Result is always 0
        bogus.add(new JumpInsnNode(IFNE, deadCodeLabel)); // Never jumps (0 != 0 is false)

        // Normal execution path
        bogus.add(new JumpInsnNode(GOTO, continueLabel));

        // Dead code path (never executed)
        bogus.add(deadCodeLabel);
        // Add some confusing dead code
        bogus.add(new InsnNode(ACONST_NULL));
        bogus.add(new InsnNode(ATHROW)); // Would throw NPE if reached

        bogus.add(continueLabel);
        // Add NOP to ensure label is valid
        bogus.add(new InsnNode(NOP));

        return bogus;
    }

    /**
     * Stack-based Control Flow Obfuscation
     * Add neutral stack operations that don't change behavior but confuse analysis
     */
    private void applyStackBasedObfuscation(MethodNode methodNode) {
        if(methodNode.instructions.size() > 3000) return;

        List<AbstractInsnNode> safePoints = Arrays.stream(methodNode.instructions.toArray())
            .filter(insn -> insn instanceof LabelNode)
            .collect(Collectors.toList());

        for(AbstractInsnNode point : safePoints) {
            if(ThreadLocalRandom.current().nextDouble() < 0.2) { // 20% chance
                InsnList stackOps = createNeutralStackOperations();
                methodNode.instructions.insert(point, stackOps);
            }
        }
    }

    /**
     * Create neutral stack operations that don't affect program behavior
     * These are identity operations that cancel out
     */
    private InsnList createNeutralStackOperations() {
        InsnList ops = new InsnList();

        int pattern = ThreadLocalRandom.current().nextInt(4);

        switch(pattern) {
            case 0:
                // Push and pop integer
                ops.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                ops.add(new InsnNode(POP));
                break;
            case 1:
                // Push two, add, push negative, add (net zero)
                int value = ThreadLocalRandom.current().nextInt(1000);
                ops.add(ASMUtils.pushInt(value));
                ops.add(ASMUtils.pushInt(-value));
                ops.add(new InsnNode(IADD));
                ops.add(new InsnNode(POP));
                break;
            case 2:
                // XOR with self (always 0)
                int xorValue = ThreadLocalRandom.current().nextInt();
                ops.add(ASMUtils.pushInt(xorValue));
                ops.add(ASMUtils.pushInt(xorValue));
                ops.add(new InsnNode(IXOR));
                ops.add(new InsnNode(POP));
                break;
            case 3:
                // Multiple NOPs
                for(int i = 0; i < ThreadLocalRandom.current().nextInt(3) + 1; i++) {
                    ops.add(new InsnNode(NOP));
                }
                break;
        }

        return ops;
    }

    /**
     * Generate an opaque hash predicate that always evaluates to true
     * but is difficult to analyze statically
     */
    private OpaqueHash generateOpaqueHash() {
        int seed = (int)(hashSeed ^ methodCounter);
        int expectedValue = ThreadLocalRandom.current().nextInt();

        // Create hash expression that computes to expectedValue
        // Using multiple operations to make it harder to analyze
        return new OpaqueHash(seed, expectedValue);
    }

    /**
     * Returns a random exception type for exception-based control flow
     * Only using unchecked exceptions to avoid method signature issues
     */
    private String getRandomExceptionType() {
        String[] exceptions = {
            "java/lang/RuntimeException",
            "java/lang/ArrayStoreException",
            "java/lang/IllegalStateException",
            "java/lang/IllegalArgumentException"
        };
        return exceptions[ThreadLocalRandom.current().nextInt(exceptions.length)];
    }

    /**
     * Opaque Hash predicate generator (inspired by Skidfuscator's hash system)
     * Creates an expression that computes to the expected value through complex operations
     */
    private static class OpaqueHash {
        private final int seed;
        private final int expectedValue;

        public OpaqueHash(int seed, int expectedValue) {
            this.seed = seed;
            this.expectedValue = expectedValue;
        }

        /**
         * Generate bytecode that computes to expectedValue using the seed
         * Multiple patterns for variety and increased complexity
         */
        public InsnList getHashExpression() {
            InsnList expr = new InsnList();

            // Choose random obfuscation pattern (expanded from skidfuscator)
            switch(ThreadLocalRandom.current().nextInt(7)) {
                case 0 -> {
                    // Pattern: (seed XOR magic) == expectedValue
                    int magic = seed ^ expectedValue;
                    expr.add(ASMUtils.pushInt(seed));
                    expr.add(ASMUtils.pushInt(magic));
                    expr.add(new InsnNode(IXOR));
                }
                case 1 -> {
                    // Pattern: (seed + offset) == expectedValue
                    int offset = expectedValue - seed;
                    expr.add(ASMUtils.pushInt(seed));
                    expr.add(ASMUtils.pushInt(offset));
                    expr.add(new InsnNode(IADD));
                }
                case 2 -> {
                    // Pattern: ((seed XOR v1) + v2) == expectedValue
                    int v1 = ThreadLocalRandom.current().nextInt();
                    int v2 = expectedValue - (seed ^ v1);
                    expr.add(ASMUtils.pushInt(seed));
                    expr.add(ASMUtils.pushInt(v1));
                    expr.add(new InsnNode(IXOR));
                    expr.add(ASMUtils.pushInt(v2));
                    expr.add(new InsnNode(IADD));
                }
                case 3 -> {
                    // Pattern: ((seed * multiplier) + offset) == expectedValue
                    int multiplier = ThreadLocalRandom.current().nextInt(10) + 1;
                    int offset = expectedValue - (seed * multiplier);
                    expr.add(ASMUtils.pushInt(seed));
                    expr.add(ASMUtils.pushInt(multiplier));
                    expr.add(new InsnNode(IMUL));
                    expr.add(ASMUtils.pushInt(offset));
                    expr.add(new InsnNode(IADD));
                }
                case 4 -> {
                    // Pattern: ((seed - v1) + v1) == expectedValue (identity with offset)
                    int v1 = ThreadLocalRandom.current().nextInt();
                    int offset = expectedValue - seed;
                    expr.add(ASMUtils.pushInt(seed));
                    expr.add(ASMUtils.pushInt(v1));
                    expr.add(new InsnNode(ISUB));
                    expr.add(ASMUtils.pushInt(v1 + offset));
                    expr.add(new InsnNode(IADD));
                }
                case 5 -> {
                    // Pattern: ((seed & mask) | (seed & ~mask)) == seed, then add offset
                    // This is an identity: (a & b) | (a & ~b) = a
                    int mask = ThreadLocalRandom.current().nextInt();
                    int offset = expectedValue - seed;
                    expr.add(ASMUtils.pushInt(seed));
                    expr.add(ASMUtils.pushInt(mask));
                    expr.add(new InsnNode(IAND));
                    expr.add(ASMUtils.pushInt(seed));
                    expr.add(ASMUtils.pushInt(~mask));
                    expr.add(new InsnNode(IAND));
                    expr.add(new InsnNode(IOR));
                    expr.add(ASMUtils.pushInt(offset));
                    expr.add(new InsnNode(IADD));
                }
                case 6 -> {
                    // Pattern: ((seed XOR v1 XOR v1) + offset) == expectedValue
                    // Double XOR cancels out
                    int v1 = ThreadLocalRandom.current().nextInt();
                    int offset = expectedValue - seed;
                    expr.add(ASMUtils.pushInt(seed));
                    expr.add(ASMUtils.pushInt(v1));
                    expr.add(new InsnNode(IXOR));
                    expr.add(ASMUtils.pushInt(v1));
                    expr.add(new InsnNode(IXOR));
                    expr.add(ASMUtils.pushInt(offset));
                    expr.add(new InsnNode(IADD));
                }
            }

            return expr;
        }

        public int getExpectedValue() {
            return expectedValue;
        }
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getControlFlowObfuscation() == this.getEnableType().type(),
            BozarConfig.BozarOptions.ControlFlowObfuscationOption.SUPER
        );
    }
}
