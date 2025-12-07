package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ControlFlowTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SuperControlFlowTransformer extends ControlFlowTransformer {

    public SuperControlFlowTransformer(Bozar bozar) {
        super(bozar, "Super Control Flow", BozarCategory.ADVANCED);
    }

    private static final double OPAQUE_PREDICATE_PROB = 0.70;
    private static final double PROXY_BLOCK_PROB = 0.55;
    private static final double GOTO_SWITCH_PROB = 0.45;
    private static final double BOGUS_EXCEPTION_PROB = 0.35;
    private static final double ADVANCED_EXCEPTION_PROB = 0.40;

    private static final int MAX_METHOD_SIZE = 5000;
    private static final int MAX_TRY_CATCH_COUNT = 20;
    private static final int MAX_CODE_SIZE = 0xFFFF;

    private static final int HASH_PRIME_1 = 0x9E3779B1;
    private static final int HASH_PRIME_2 = 0x85EBCA6B;

    private int classSeed;
    private int methodSeed;
    private int transformCounter;

    private String exceptionHandlerName;

    @Override
    public void pre() {
        if (this.getBozar().getClasses().isEmpty()) return;
        ClassNode referenceClass = this.getBozar().getClasses().get(0);
        createExceptionHandlerClass(referenceClass);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        if (!ASMUtils.isClassEligibleToModify(classNode)) return;

        if (isInterfaceOrAnnotation(classNode)) return;

        this.classSeed = ThreadLocalRandom.current().nextInt();
        this.transformCounter = 0;
    }

    private void createExceptionHandlerClass(ClassNode referenceClass) {
        String baseName = ASMUtils.parentName(referenceClass.name);
        String randomSuffix = generateRandomString(5);
        this.exceptionHandlerName = baseName + randomSuffix;

        for (ClassNode cn : this.getBozar().getClasses()) {
            if (cn.name.equals(this.exceptionHandlerName)) {
                this.exceptionHandlerName = baseName + generateRandomString(7);
                break;
            }
        }

        ClassNode handlerClass = new ClassNode();
        handlerClass.visit(V1_8, ACC_PUBLIC | ACC_SUPER, exceptionHandlerName, null, "java/lang/Throwable", null);

        FieldVisitor fv = handlerClass.visitField(ACC_PUBLIC, "o", "Ljava/lang/Object;", null, null);
        fv.visitEnd();

        MethodVisitor mv = handlerClass.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, exceptionHandlerName, "o", "Ljava/lang/Object;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        this.getBozar().getClasses().add(handlerClass);
        System.out.println("[SuperControlFlow] Created exception handler class: " + exceptionHandlerName);
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if (!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) return;
        if (shouldSkipMethod(methodNode)) return;

        this.methodSeed = classSeed ^ (++transformCounter * HASH_PRIME_1);

        applyOpaquePredicates(classNode, methodNode);

        applyProxyBlocks(methodNode);

        applyGotoSwitchDispatch(methodNode);

        if (canAddTryCatch(methodNode)) {
            applyBogusExceptionFlow(methodNode);
        }

        if (canAddTryCatch(methodNode) && this.exceptionHandlerName != null) {
            applyAdvancedExceptionFlow(classNode, methodNode);
        }

        System.out.println("[SuperControlFlow] Obfuscated: " + classNode.name + "." + methodNode.name + methodNode.desc);
    }

    private boolean isInterfaceOrAnnotation(ClassNode classNode) {
        return (classNode.access & (ACC_INTERFACE | ACC_ANNOTATION)) != 0;
    }

    private boolean shouldSkipMethod(MethodNode methodNode) {

        if ((methodNode.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) return true;

        if (methodNode.instructions.size() < 5) return true;

        if (methodNode.instructions.size() > MAX_METHOD_SIZE) return true;

        return false;
    }

    private boolean canAddTryCatch(MethodNode methodNode) {

        if (methodNode.tryCatchBlocks != null && methodNode.tryCatchBlocks.size() > MAX_TRY_CATCH_COUNT) {
            return false;
        }
        return true;
    }

    private void applyOpaquePredicates(ClassNode classNode, MethodNode methodNode) {
        List<AbstractInsnNode> insertionPoints = new ArrayList<>();

        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn.getOpcode() == GOTO) {
                AbstractInsnNode next = insn.getNext();

                if (next != null && !(next instanceof LabelNode)) {
                    if (ThreadLocalRandom.current().nextDouble() < OPAQUE_PREDICATE_PROB) {
                        insertionPoints.add(insn);
                    }
                }
            }
        }

        AbstractInsnNode first = methodNode.instructions.getFirst();
        if (first != null) {

            while (first != null && (first instanceof LabelNode || first instanceof LineNumberNode || first instanceof FrameNode)) {
                first = first.getNext();
            }
            if (first != null && first.getOpcode() >= 0) {
                insertionPoints.add(0, null);
            }
        }

        for (AbstractInsnNode insertPoint : insertionPoints) {
            if (insertPoint == null) {
                insertOpaquePredicateAtStart(methodNode);
            } else {
                insertOpaquePredicateAfter(methodNode, insertPoint);
            }
        }
    }

    private void insertOpaquePredicateAtStart(MethodNode methodNode) {
        InsnList predicate = new InsnList();
        LabelNode skipDeadCode = new LabelNode();

        predicate.add(createOpaqueFalsePredicate());

        LabelNode deadCodeLabel = new LabelNode();
        predicate.add(new JumpInsnNode(IFNE, deadCodeLabel));
        predicate.add(new JumpInsnNode(GOTO, skipDeadCode));

        predicate.add(deadCodeLabel);
        predicate.add(createDeadCodeBlock());
        predicate.add(new JumpInsnNode(GOTO, skipDeadCode));

        predicate.add(skipDeadCode);

        AbstractInsnNode first = methodNode.instructions.getFirst();
        while (first != null && (first instanceof LabelNode || first instanceof LineNumberNode || first instanceof FrameNode)) {
            first = first.getNext();
        }
        if (first != null) {
            methodNode.instructions.insertBefore(first, predicate);
        }
    }

    private void insertOpaquePredicateAfter(MethodNode methodNode, AbstractInsnNode insertPoint) {
        InsnList predicate = new InsnList();
        LabelNode skipDeadCode = new LabelNode();

        predicate.add(createOpaqueFalsePredicate());

        LabelNode deadCodeLabel = new LabelNode();
        predicate.add(new JumpInsnNode(IFNE, deadCodeLabel));
        predicate.add(new JumpInsnNode(GOTO, skipDeadCode));

        predicate.add(deadCodeLabel);
        predicate.add(createDeadCodeBlock());
        predicate.add(new JumpInsnNode(GOTO, skipDeadCode));

        predicate.add(skipDeadCode);

        methodNode.instructions.insert(insertPoint, predicate);
    }

    private InsnList createOpaqueFalsePredicate() {
        InsnList insns = new InsnList();
        int pattern = ThreadLocalRandom.current().nextInt(6);

        switch (pattern) {
            case 0 -> {

                int x = ThreadLocalRandom.current().nextInt();
                insns.add(ASMUtils.pushInt(x));
                insns.add(ASMUtils.pushInt(x));
                insns.add(new InsnNode(IXOR));
            }
            case 1 -> {

                int x = ThreadLocalRandom.current().nextInt();
                insns.add(ASMUtils.pushInt(x));
                insns.add(ASMUtils.pushInt(x));
                insns.add(new InsnNode(ISUB));
            }
            case 2 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(ASMUtils.pushInt(0));
                insns.add(new InsnNode(IAND));
            }
            case 3 -> {

                insns.add(ASMUtils.pushInt(0));
                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(new InsnNode(IMUL));
            }
            case 4 -> {

                int x = ThreadLocalRandom.current().nextInt();
                insns.add(ASMUtils.pushInt(x));
                insns.add(ASMUtils.pushInt(x));
                insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "compare", "(II)I", false));
            }
            case 5 -> {

                insns.add(new LdcInsnNode(""));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
            }
        }

        return insns;
    }

    private InsnList createDeadCodeBlock() {
        InsnList insns = new InsnList();
        int pattern = ThreadLocalRandom.current().nextInt(5);

        switch (pattern) {
            case 0 -> {

                insns.add(new TypeInsnNode(NEW, "java/lang/RuntimeException"));
                insns.add(new InsnNode(DUP));
                insns.add(new LdcInsnNode("dead_" + Integer.toHexString(methodSeed)));
                insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false));
                insns.add(new InsnNode(ATHROW));
            }
            case 1 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt(100)));
                insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "exit", "(I)V", false));
            }
            case 2 -> {

                LabelNode loopLabel = new LabelNode();
                insns.add(loopLabel);
                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(new InsnNode(POP));
                insns.add(new JumpInsnNode(GOTO, loopLabel));
            }
            case 3 -> {

                insns.add(new InsnNode(ACONST_NULL));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false));
                insns.add(new InsnNode(POP));
            }
            case 4 -> {

                insns.add(ASMUtils.pushInt(0));
                insns.add(new IntInsnNode(NEWARRAY, T_INT));
                insns.add(ASMUtils.pushInt(100));
                insns.add(new InsnNode(IALOAD));
                insns.add(new InsnNode(POP));
            }
        }

        return insns;
    }

    private void applyProxyBlocks(MethodNode methodNode) {
        List<JumpInsnNode> gotos = new ArrayList<>();

        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn.getOpcode() == GOTO && insn instanceof JumpInsnNode jump) {
                if (!isBackwardJump(methodNode, jump)) {
                    if (ThreadLocalRandom.current().nextDouble() < PROXY_BLOCK_PROB) {
                        gotos.add(jump);
                    }
                }
            }
        }

        for (JumpInsnNode gotoInsn : gotos) {
            insertProxyBlock(methodNode, gotoInsn);
        }
    }

    private void insertProxyBlock(MethodNode methodNode, JumpInsnNode gotoInsn) {
        LabelNode originalTarget = gotoInsn.label;
        LabelNode proxyLabel = new LabelNode();

        InsnList proxyBlock = new InsnList();
        proxyBlock.add(proxyLabel);

        int junkCount = 2 + ThreadLocalRandom.current().nextInt(3);
        for (int i = 0; i < junkCount; i++) {
            proxyBlock.add(createJunkOperation());
        }

        proxyBlock.add(new JumpInsnNode(GOTO, originalTarget));

        methodNode.instructions.insert(gotoInsn, proxyBlock);

        gotoInsn.label = proxyLabel;
    }

    private InsnList createJunkOperation() {
        InsnList insns = new InsnList();
        int pattern = ThreadLocalRandom.current().nextInt(6);

        switch (pattern) {
            case 0 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(new InsnNode(IXOR));
                insns.add(new InsnNode(POP));
            }
            case 1 -> {

                insns.add(new LdcInsnNode("junk_" + ThreadLocalRandom.current().nextInt(1000)));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false));
                insns.add(new InsnNode(POP));
            }
            case 2 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "bitCount", "(I)I", false));
                insns.add(new InsnNode(POP));
            }
            case 3 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "reverse", "(I)I", false));
                insns.add(new InsnNode(POP));
            }
            case 4 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt() | 1));
                insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "numberOfLeadingZeros", "(I)I", false));
                insns.add(new InsnNode(POP));
            }
            case 5 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt(31) + 1));
                insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false));
                insns.add(new InsnNode(POP));
            }
        }

        return insns;
    }

    private void applyGotoSwitchDispatch(MethodNode methodNode) {
        List<JumpInsnNode> gotos = new ArrayList<>();

        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn.getOpcode() == GOTO && insn instanceof JumpInsnNode jump) {
                if (!isBackwardJump(methodNode, jump)) {
                    if (ThreadLocalRandom.current().nextDouble() < GOTO_SWITCH_PROB) {
                        gotos.add(jump);
                    }
                }
            }
        }

        for (JumpInsnNode gotoInsn : gotos) {
            transformGotoToSwitch(methodNode, gotoInsn);
        }
    }

    private void transformGotoToSwitch(MethodNode methodNode, JumpInsnNode gotoInsn) {
        LabelNode originalTarget = gotoInsn.label;

        LabelNode defaultLabel = new LabelNode();
        LabelNode dead1 = new LabelNode();
        LabelNode dead2 = new LabelNode();

        int correctKey = ThreadLocalRandom.current().nextInt(1000);
        int fakeKey1, fakeKey2;
        do {
            fakeKey1 = ThreadLocalRandom.current().nextInt(1000);
        } while (fakeKey1 == correctKey);
        do {
            fakeKey2 = ThreadLocalRandom.current().nextInt(1000);
        } while (fakeKey2 == correctKey || fakeKey2 == fakeKey1);

        int[] keys = {correctKey, fakeKey1, fakeKey2};
        LabelNode[] labels = {originalTarget, dead1, dead2};
        sortKeysAndLabels(keys, labels);

        InsnList switchBlock = new InsnList();

        switchBlock.add(createKeyComputation(correctKey));

        switchBlock.add(new LookupSwitchInsnNode(defaultLabel, keys, labels));

        switchBlock.add(dead1);
        switchBlock.add(createMiniDeadCode());
        switchBlock.add(new JumpInsnNode(GOTO, defaultLabel));

        switchBlock.add(dead2);
        switchBlock.add(createMiniDeadCode());
        switchBlock.add(new JumpInsnNode(GOTO, defaultLabel));

        switchBlock.add(defaultLabel);
        switchBlock.add(createExceptionThrow());

        methodNode.instructions.insert(gotoInsn, switchBlock);
        methodNode.instructions.remove(gotoInsn);
    }

    private void sortKeysAndLabels(int[] keys, LabelNode[] labels) {

        for (int i = 0; i < keys.length - 1; i++) {
            for (int j = 0; j < keys.length - i - 1; j++) {
                if (keys[j] > keys[j + 1]) {
                    int tempKey = keys[j];
                    keys[j] = keys[j + 1];
                    keys[j + 1] = tempKey;

                    LabelNode tempLabel = labels[j];
                    labels[j] = labels[j + 1];
                    labels[j + 1] = tempLabel;
                }
            }
        }
    }

    private InsnList createKeyComputation(int targetKey) {
        InsnList insns = new InsnList();
        int pattern = ThreadLocalRandom.current().nextInt(4);

        switch (pattern) {
            case 0 -> {

                insns.add(ASMUtils.pushInt(targetKey));
            }
            case 1 -> {

                int a = ThreadLocalRandom.current().nextInt();
                int b = a ^ targetKey;
                insns.add(ASMUtils.pushInt(a));
                insns.add(ASMUtils.pushInt(b));
                insns.add(new InsnNode(IXOR));
            }
            case 2 -> {

                int a = ThreadLocalRandom.current().nextInt(500);
                int b = targetKey - a;
                insns.add(ASMUtils.pushInt(a));
                insns.add(ASMUtils.pushInt(b));
                insns.add(new InsnNode(IADD));
            }
            case 3 -> {

                int val = targetKey + (ThreadLocalRandom.current().nextInt(10) * 1000);
                insns.add(ASMUtils.pushInt(val));
                insns.add(ASMUtils.pushInt(1000));
                insns.add(new InsnNode(IREM));
            }
        }

        return insns;
    }

    private InsnList createMiniDeadCode() {
        InsnList insns = new InsnList();
        insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
        insns.add(new InsnNode(POP));
        return insns;
    }

    private InsnList createExceptionThrow() {
        InsnList insns = new InsnList();
        String[] exceptions = {
            "java/lang/RuntimeException",
            "java/lang/IllegalStateException",
            "java/lang/Error"
        };
        String exType = exceptions[ThreadLocalRandom.current().nextInt(exceptions.length)];

        insns.add(new TypeInsnNode(NEW, exType));
        insns.add(new InsnNode(DUP));
        insns.add(new MethodInsnNode(INVOKESPECIAL, exType, "<init>", "()V", false));
        insns.add(new InsnNode(ATHROW));

        return insns;
    }

    private void applyBogusExceptionFlow(MethodNode methodNode) {
        List<JumpInsnNode> gotos = new ArrayList<>();

        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn.getOpcode() == GOTO && insn instanceof JumpInsnNode jump) {
                if (!isBackwardJump(methodNode, jump)) {
                    if (ThreadLocalRandom.current().nextDouble() < BOGUS_EXCEPTION_PROB) {
                        gotos.add(jump);
                    }
                }
            }
        }

        for (JumpInsnNode gotoInsn : gotos) {
            transformGotoToException(methodNode, gotoInsn);
        }
    }

    private void transformGotoToException(MethodNode methodNode, JumpInsnNode gotoInsn) {
        LabelNode originalTarget = gotoInsn.label;

        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode handler = new LabelNode();

        String exType = "java/lang/RuntimeException";

        InsnList exceptionBlock = new InsnList();

        exceptionBlock.add(tryStart);

        exceptionBlock.add(createOpaqueTruePredicate());
        LabelNode throwLabel = new LabelNode();
        exceptionBlock.add(new JumpInsnNode(IFNE, throwLabel));

        exceptionBlock.add(new InsnNode(ACONST_NULL));
        exceptionBlock.add(new InsnNode(ATHROW));

        exceptionBlock.add(throwLabel);
        exceptionBlock.add(new TypeInsnNode(NEW, exType));
        exceptionBlock.add(new InsnNode(DUP));
        exceptionBlock.add(new MethodInsnNode(INVOKESPECIAL, exType, "<init>", "()V", false));
        exceptionBlock.add(new InsnNode(ATHROW));
        exceptionBlock.add(tryEnd);

        exceptionBlock.add(handler);
        exceptionBlock.add(new InsnNode(POP));
        exceptionBlock.add(new JumpInsnNode(GOTO, originalTarget));

        methodNode.instructions.insert(gotoInsn, exceptionBlock);
        methodNode.instructions.remove(gotoInsn);

        if (methodNode.tryCatchBlocks == null) {
            methodNode.tryCatchBlocks = new ArrayList<>();
        }
        methodNode.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, handler, exType));
    }

    private InsnList createOpaqueTruePredicate() {
        InsnList insns = new InsnList();
        int pattern = ThreadLocalRandom.current().nextInt(4);

        switch (pattern) {
            case 0 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(ASMUtils.pushInt(1));
                insns.add(new InsnNode(IOR));
            }
            case 1 -> {

                insns.add(ASMUtils.pushInt(ThreadLocalRandom.current().nextInt()));
                insns.add(ASMUtils.pushInt(0));
                insns.add(new InsnNode(IMUL));
                insns.add(ASMUtils.pushInt(1));
                insns.add(new InsnNode(IADD));
            }
            case 2 -> {

                insns.add(new LdcInsnNode("x"));
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
            }
            case 3 -> {

                insns.add(ASMUtils.pushInt(1 + ThreadLocalRandom.current().nextInt(100)));
            }
        }

        return insns;
    }

    private void applyAdvancedExceptionFlow(ClassNode classNode, MethodNode methodNode) {
        int storeCount = 0;
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof VarInsnNode) {
                int op = insn.getOpcode();
                if (op >= ISTORE && op <= ASTORE) {
                    storeCount++;
                }
            }
        }

        if (ASMUtils.getCodeSize(methodNode) + (storeCount * 20) >= MAX_CODE_SIZE) {
            return;
        }

        Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
        Frame<BasicValue>[] frames;

        try {
            frames = analyzer.analyzeAndComputeMaxs(classNode.name, methodNode);
        } catch (Exception ex) {
            return;
        }

        List<AbstractInsnNode> toProcess = new ArrayList<>();
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof VarInsnNode) {
                int op = insn.getOpcode();
                if (op >= ISTORE && op <= ASTORE) {
                    if (ThreadLocalRandom.current().nextDouble() < ADVANCED_EXCEPTION_PROB) {
                        toProcess.add(insn);
                    }
                }
            }
        }

        for (AbstractInsnNode instruction : toProcess) {
            int insnIndex = methodNode.instructions.indexOf(instruction);
            if (insnIndex < 0 || insnIndex >= frames.length) continue;

            Frame<BasicValue> frame = frames[insnIndex];
            if (frame == null || frame.getStackSize() < 1) continue;

            BasicValue value = frame.getStack(frame.getStackSize() - 1);
            if (value == null || value == BasicValue.UNINITIALIZED_VALUE) continue;

            Type type = value.getType();
            if (type == null) continue;

            int expectedStackSize = type.getSize();
            if (frame.getStackSize() != expectedStackSize) continue;

            String typeDesc = type.getDescriptor();
            if (typeDesc.equals("Lnull;") || typeDesc.equals("Ljava/lang/Object;")) continue;

            if (instruction.getOpcode() == ASTORE && !typeDesc.startsWith("L") && !typeDesc.startsWith("[")) {
                continue;
            }

            wrapStoreWithException(methodNode, instruction, typeDesc);
        }
    }

    private void wrapStoreWithException(MethodNode methodNode, AbstractInsnNode storeInsn, String typeDesc) {
        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode handler = new LabelNode();
        LabelNode finish = new LabelNode();

        InsnList list = new InsnList();

        list.add(tryStart);

        ASMUtils.boxPrimitive(typeDesc, list);

        list.add(new TypeInsnNode(NEW, exceptionHandlerName));
        list.add(new InsnNode(DUP));
        list.add(new InsnNode(DUP2_X1));
        list.add(new InsnNode(POP2));
        list.add(new MethodInsnNode(INVOKESPECIAL, exceptionHandlerName, "<init>", "(Ljava/lang/Object;)V", false));
        list.add(new InsnNode(ATHROW));

        list.add(tryEnd);

        list.add(handler);
        list.add(new FieldInsnNode(GETFIELD, exceptionHandlerName, "o", "Ljava/lang/Object;"));
        ASMUtils.unboxPrimitive(typeDesc, list);
        list.add(new JumpInsnNode(GOTO, finish));

        list.add(finish);

        VarInsnNode originalStore = (VarInsnNode) storeInsn;
        list.add(new VarInsnNode(originalStore.getOpcode(), originalStore.var));

        methodNode.instructions.insert(storeInsn, list);
        methodNode.instructions.remove(storeInsn);

        if (methodNode.tryCatchBlocks == null) {
            methodNode.tryCatchBlocks = new ArrayList<>();
        }
        methodNode.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, handler, exceptionHandlerName));
    }

    private boolean isBackwardJump(MethodNode methodNode, JumpInsnNode jump) {
        int jumpIndex = methodNode.instructions.indexOf(jump);
        int targetIndex = methodNode.instructions.indexOf(jump.label);
        return targetIndex < jumpIndex;
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getControlFlowObfuscation() == this.getEnableType().type(),
            BozarConfig.BozarOptions.ControlFlowObfuscationOption.SUPER
        );
    }
}
