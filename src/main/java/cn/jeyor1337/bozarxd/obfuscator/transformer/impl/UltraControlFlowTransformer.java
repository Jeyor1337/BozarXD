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

public class UltraControlFlowTransformer extends ControlFlowTransformer {

    public UltraControlFlowTransformer(Bozar bozar) {
        super(bozar, "Ultra Control Flow", BozarCategory.ADVANCED);
    }

    private static final int FAKE_STATE_COUNT = 3;

    private static final double EXCEPTION_TRANSFER_PROBABILITY = 0.30;

    private static final int MIN_METHOD_SIZE = 20;

    private static final int MAX_METHOD_SIZE = 1500;

    private static final String DATA_CARRIER_CLASS = "cn/jeyor1337/bozarxd/BozarDataCarrier";

    private boolean dataCarrierCreated = false;

    private Map<LabelNode, Integer> labelToStateMap = new HashMap<>();

    private int stateVarIndex = 0;

    private Frame<BasicValue>[] frames = null;

    private List<Integer> fakeStateKeys = new ArrayList<>();

    @Override
    public void pre() {

        if (!dataCarrierCreated) {
            createDataCarrierClass();
            dataCarrierCreated = true;
        }
    }

    @Override
    public void post() {
        labelToStateMap.clear();
        fakeStateKeys.clear();
        frames = null;
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        if (!ASMUtils.isMethodEligibleToModify(classNode, methodNode)) return;
        if (!isMethodSuitable(classNode, methodNode)) return;

        labelToStateMap.clear();
        fakeStateKeys.clear();
        frames = null;

        try {

            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            frames = analyzer.analyze(classNode.name, methodNode);
        } catch (AnalyzerException e) {
            return;
        }

        Set<LabelNode> jumpTargets = collectJumpTargets(methodNode);
        if (jumpTargets.isEmpty()) return;

        if (!allLabelsHaveEmptyStack(methodNode)) {
            return;
        }

        stateVarIndex = findNextLocalIndex(methodNode);

        assignStateKeys(jumpTargets);

        rewriteAsStateMachine(classNode, methodNode, jumpTargets);
    }

    private boolean isMethodSuitable(ClassNode classNode, MethodNode methodNode) {

        if (methodNode.instructions.size() < MIN_METHOD_SIZE) return false;

        if (methodNode.instructions.size() > MAX_METHOD_SIZE) return false;

        if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) return false;

        if ((methodNode.access & (ACC_ABSTRACT | ACC_NATIVE | ACC_BRIDGE | ACC_SYNTHETIC)) != 0) return false;

        if (methodNode.tryCatchBlocks != null && !methodNode.tryCatchBlocks.isEmpty()) return false;

        if (classNode.name.contains("$")) return false;

        return true;
    }

    private Set<LabelNode> collectJumpTargets(MethodNode methodNode) {
        Set<LabelNode> targets = new HashSet<>();

        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof JumpInsnNode jump) {
                targets.add(jump.label);
            } else if (insn instanceof LookupSwitchInsnNode lookupSwitch) {
                targets.add(lookupSwitch.dflt);
                targets.addAll(Arrays.asList(lookupSwitch.labels.toArray(new LabelNode[0])));
            } else if (insn instanceof TableSwitchInsnNode tableSwitch) {
                targets.add(tableSwitch.dflt);
                targets.addAll(tableSwitch.labels);
            }
        }

        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof LabelNode label) {
                targets.add(label);
                break;
            }
        }

        return targets;
    }

    private boolean allLabelsHaveEmptyStack(MethodNode methodNode) {
        if (frames == null || frames.length == 0) return false;

        Map<LabelNode, Integer> labelIndices = new HashMap<>();
        int index = 0;
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof LabelNode label) {
                labelIndices.put(label, index);
            }
            index++;
        }

        for (AbstractInsnNode insn : methodNode.instructions) {
            Set<LabelNode> targets = new HashSet<>();

            if (insn instanceof JumpInsnNode jump) {
                targets.add(jump.label);
            } else if (insn instanceof LookupSwitchInsnNode lookup) {
                targets.add(lookup.dflt);
                targets.addAll(lookup.labels);
            } else if (insn instanceof TableSwitchInsnNode table) {
                targets.add(table.dflt);
                targets.addAll(table.labels);
            }

            for (LabelNode target : targets) {
                Integer idx = labelIndices.get(target);
                if (idx == null || idx >= frames.length) return false;

                Frame<BasicValue> frame = frames[idx];

                if (frame == null || frame.getStackSize() > 0) {
                    return false;
                }
            }
        }

        return true;
    }

    private int findNextLocalIndex(MethodNode methodNode) {
        int max = 0;
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof VarInsnNode varInsn) {
                int size = (varInsn.getOpcode() == LLOAD || varInsn.getOpcode() == LSTORE ||
                           varInsn.getOpcode() == DLOAD || varInsn.getOpcode() == DSTORE) ? 2 : 1;
                max = Math.max(max, varInsn.var + size);
            } else if (insn instanceof IincInsnNode iinc) {
                max = Math.max(max, iinc.var + 1);
            }
        }
        return max;
    }

    private void assignStateKeys(Set<LabelNode> targets) {
        Set<Integer> usedKeys = new HashSet<>();

        for (LabelNode label : targets) {
            int key;
            do {
                key = ThreadLocalRandom.current().nextInt();
            } while (usedKeys.contains(key));
            usedKeys.add(key);
            labelToStateMap.put(label, key);
        }

        for (int i = 0; i < FAKE_STATE_COUNT; i++) {
            int fakeKey;
            do {
                fakeKey = ThreadLocalRandom.current().nextInt();
            } while (usedKeys.contains(fakeKey));
            usedKeys.add(fakeKey);
            fakeStateKeys.add(fakeKey);
        }
    }

    private void rewriteAsStateMachine(ClassNode classNode, MethodNode methodNode, Set<LabelNode> jumpTargets) {
        InsnList newInsns = new InsnList();

        LabelNode firstLabel = null;
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof LabelNode label && jumpTargets.contains(label)) {
                firstLabel = label;
                break;
            }
        }

        if (firstLabel == null) return;

        Integer startState = labelToStateMap.get(firstLabel);
        if (startState == null) return;

        LabelNode loopLabel = new LabelNode();
        LabelNode defaultLabel = new LabelNode();

        List<Map.Entry<LabelNode, Integer>> sortedEntries = labelToStateMap.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .collect(Collectors.toList());

        List<LabelNode> fakeLabels = new ArrayList<>();
        for (int fakeKey : fakeStateKeys) {
            LabelNode fakeLabel = new LabelNode();
            fakeLabels.add(fakeLabel);
        }

        List<Integer> allKeys = new ArrayList<>();
        List<LabelNode> allLabels = new ArrayList<>();

        for (Map.Entry<LabelNode, Integer> entry : sortedEntries) {
            allKeys.add(entry.getValue());
            allLabels.add(entry.getKey());
        }
        for (int i = 0; i < fakeStateKeys.size(); i++) {
            allKeys.add(fakeStateKeys.get(i));
            allLabels.add(fakeLabels.get(i));
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < allKeys.size(); i++) indices.add(i);
        indices.sort(Comparator.comparingInt(allKeys::get));

        int[] sortedKeys = indices.stream().mapToInt(allKeys::get).toArray();
        LabelNode[] sortedLabels = indices.stream().map(allLabels::get).toArray(LabelNode[]::new);

        newInsns.add(ASMUtils.pushInt(startState));
        newInsns.add(new VarInsnNode(ISTORE, stateVarIndex));

        newInsns.add(loopLabel);
        newInsns.add(new VarInsnNode(ILOAD, stateVarIndex));

        newInsns.add(new LookupSwitchInsnNode(defaultLabel, sortedKeys, sortedLabels));

        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof LabelNode label) {
                newInsns.add(label);
            } else if (insn instanceof JumpInsnNode jump) {

                newInsns.add(transformJumpInstruction(jump, loopLabel));
            } else if (insn instanceof LookupSwitchInsnNode || insn instanceof TableSwitchInsnNode) {

                newInsns.add(transformSwitchInstruction(insn, loopLabel));
            } else {
                newInsns.add(insn);
            }
        }

        for (LabelNode fakeLabel : fakeLabels) {
            newInsns.add(fakeLabel);
            newInsns.add(createFakeBranchCode());
        }

        newInsns.add(defaultLabel);
        newInsns.add(createExceptionThrow());

        methodNode.instructions = newInsns;
        methodNode.maxLocals = Math.max(methodNode.maxLocals, stateVarIndex + 1);
    }

    private InsnList transformJumpInstruction(JumpInsnNode jump, LabelNode loopLabel) {
        InsnList insns = new InsnList();

        LabelNode target = jump.label;
        Integer targetState = labelToStateMap.get(target);

        if (targetState == null) {

            insns.add(jump);
            return insns;
        }

        int opcode = jump.getOpcode();

        if (opcode == GOTO) {

            if (ThreadLocalRandom.current().nextDouble() < EXCEPTION_TRANSFER_PROBABILITY) {

                insns.add(createExceptionBasedTransfer(targetState, loopLabel));
            } else {
                insns.add(ASMUtils.pushInt(targetState));
                insns.add(new VarInsnNode(ISTORE, stateVarIndex));
                insns.add(new JumpInsnNode(GOTO, loopLabel));
            }
        } else {

            LabelNode trueLabel = new LabelNode();
            LabelNode continueLabel = new LabelNode();

            insns.add(new JumpInsnNode(opcode, trueLabel));

            insns.add(new JumpInsnNode(GOTO, continueLabel));

            insns.add(trueLabel);
            insns.add(ASMUtils.pushInt(targetState));
            insns.add(new VarInsnNode(ISTORE, stateVarIndex));
            insns.add(new JumpInsnNode(GOTO, loopLabel));

            insns.add(continueLabel);
        }

        return insns;
    }

    private InsnList transformSwitchInstruction(AbstractInsnNode insn, LabelNode loopLabel) {
        InsnList result = new InsnList();

        if (insn instanceof LookupSwitchInsnNode lookup) {

            LabelNode[] newLabels = new LabelNode[lookup.labels.size()];
            for (int i = 0; i < lookup.labels.size(); i++) {
                LabelNode oldLabel = lookup.labels.get(i);
                Integer state = labelToStateMap.get(oldLabel);
                if (state != null) {
                    LabelNode caseLabel = new LabelNode();
                    newLabels[i] = caseLabel;
                } else {
                    newLabels[i] = oldLabel;
                }
            }

            LabelNode newDefault = labelToStateMap.containsKey(lookup.dflt)
                ? new LabelNode() : lookup.dflt;

            result.add(new LookupSwitchInsnNode(newDefault,
                lookup.keys.stream().mapToInt(i -> i).toArray(),
                newLabels));

            for (int i = 0; i < lookup.labels.size(); i++) {
                LabelNode oldLabel = lookup.labels.get(i);
                Integer state = labelToStateMap.get(oldLabel);
                if (state != null) {
                    result.add(newLabels[i]);
                    result.add(ASMUtils.pushInt(state));
                    result.add(new VarInsnNode(ISTORE, stateVarIndex));
                    result.add(new JumpInsnNode(GOTO, loopLabel));
                }
            }

            Integer defaultState = labelToStateMap.get(lookup.dflt);
            if (defaultState != null) {
                result.add(newDefault);
                result.add(ASMUtils.pushInt(defaultState));
                result.add(new VarInsnNode(ISTORE, stateVarIndex));
                result.add(new JumpInsnNode(GOTO, loopLabel));
            }
        } else {

            result.add(insn);
        }

        return result;
    }

    private void createDataCarrierClass() {
        ClassNode carrierClass = new ClassNode();
        carrierClass.visit(V1_8, ACC_PUBLIC | ACC_SUPER, DATA_CARRIER_CLASS,
            null, "java/lang/RuntimeException", null);

        carrierClass.fields.add(new FieldNode(
            ACC_PUBLIC, "state", "I", null, null));

        MethodNode initMethod = new MethodNode(ACC_PUBLIC, "<init>", "(I)V", null, null);
        initMethod.instructions.add(new VarInsnNode(ALOAD, 0));
        initMethod.instructions.add(new MethodInsnNode(INVOKESPECIAL,
            "java/lang/RuntimeException", "<init>", "()V", false));
        initMethod.instructions.add(new VarInsnNode(ALOAD, 0));
        initMethod.instructions.add(new VarInsnNode(ILOAD, 1));
        initMethod.instructions.add(new FieldInsnNode(PUTFIELD,
            DATA_CARRIER_CLASS, "state", "I"));
        initMethod.instructions.add(new InsnNode(RETURN));
        initMethod.maxStack = 2;
        initMethod.maxLocals = 2;
        carrierClass.methods.add(initMethod);

        MethodNode defaultInit = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        defaultInit.instructions.add(new VarInsnNode(ALOAD, 0));
        defaultInit.instructions.add(new MethodInsnNode(INVOKESPECIAL,
            "java/lang/RuntimeException", "<init>", "()V", false));
        defaultInit.instructions.add(new InsnNode(RETURN));
        defaultInit.maxStack = 1;
        defaultInit.maxLocals = 1;
        carrierClass.methods.add(defaultInit);

        this.getBozar().getClasses().add(carrierClass);
    }

    private InsnList createExceptionBasedTransfer(int targetState, LabelNode loopLabel) {
        InsnList insns = new InsnList();

        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode handler = new LabelNode();

        insns.add(tryStart);
        insns.add(new TypeInsnNode(NEW, DATA_CARRIER_CLASS));
        insns.add(new InsnNode(DUP));
        insns.add(ASMUtils.pushInt(targetState));
        insns.add(new MethodInsnNode(INVOKESPECIAL,
            DATA_CARRIER_CLASS, "<init>", "(I)V", false));
        insns.add(new InsnNode(ATHROW));
        insns.add(tryEnd);

        insns.add(handler);
        insns.add(new TypeInsnNode(CHECKCAST, DATA_CARRIER_CLASS));
        insns.add(new FieldInsnNode(GETFIELD, DATA_CARRIER_CLASS, "state", "I"));
        insns.add(new VarInsnNode(ISTORE, stateVarIndex));
        insns.add(new JumpInsnNode(GOTO, loopLabel));

        InsnList simpleInsns = new InsnList();
        simpleInsns.add(ASMUtils.pushInt(targetState));
        simpleInsns.add(new VarInsnNode(ISTORE, stateVarIndex));
        simpleInsns.add(new JumpInsnNode(GOTO, loopLabel));
        return simpleInsns;
    }

    private InsnList createFakeBranchCode() {
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

    private InsnList createExceptionThrow() {
        InsnList insns = new InsnList();

        int pattern = ThreadLocalRandom.current().nextInt(3);

        switch (pattern) {
            case 0 -> {
                insns.add(new TypeInsnNode(NEW, "java/lang/RuntimeException"));
                insns.add(new InsnNode(DUP));
                insns.add(new MethodInsnNode(INVOKESPECIAL,
                    "java/lang/RuntimeException", "<init>", "()V", false));
                insns.add(new InsnNode(ATHROW));
            }
            case 1 -> {
                insns.add(new InsnNode(ACONST_NULL));
                insns.add(new InsnNode(ATHROW));
            }
            case 2 -> {
                insns.add(new TypeInsnNode(NEW, "java/lang/Error"));
                insns.add(new InsnNode(DUP));
                insns.add(new LdcInsnNode(String.valueOf(ThreadLocalRandom.current().nextLong())));
                insns.add(new MethodInsnNode(INVOKESPECIAL,
                    "java/lang/Error", "<init>", "(Ljava/lang/String;)V", false));
                insns.add(new InsnNode(ATHROW));
            }
        }

        return insns;
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getControlFlowObfuscation() == this.getEnableType().type(),
            BozarConfig.BozarOptions.ControlFlowObfuscationOption.ULTRA
        );
    }
}
