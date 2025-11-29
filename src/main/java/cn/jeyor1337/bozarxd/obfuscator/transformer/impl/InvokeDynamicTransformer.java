package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class InvokeDynamicTransformer extends ClassTransformer {

    private static final String BOOTSTRAP_METHOD_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;" +
            "Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;";

    private static class InvokeData {
        final int index;
        final String owner;
        final String name;

        InvokeData(int index, String owner, String name) {
            this.index = index;
            this.owner = owner;
            this.name = name;
        }
    }

    private static class ClassContext {
        final String bootstrapMethodName;
        final long invokeStaticMagic;
        final long invokeVirtualMagic;
        final LinkedHashMap<String, InvokeData> invokeMap = new LinkedHashMap<>();

        ClassContext() {

            this.bootstrapMethodName = generateRandomName();

            long magic1, magic2;
            do {
                magic1 = ThreadLocalRandom.current().nextLong(0x10000000L, 0x7FFFFFFFL);
                magic2 = ThreadLocalRandom.current().nextLong(0x10000000L, 0x7FFFFFFFL);
            } while (magic1 == magic2);
            this.invokeStaticMagic = magic1;
            this.invokeVirtualMagic = magic2;
        }

        private static String generateRandomName() {
            StringBuilder sb = new StringBuilder();
            String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            int length = ThreadLocalRandom.current().nextInt(8, 13);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            return sb.toString();
        }
    }

    private final Map<String, ClassContext> classContextMap = new HashMap<>();
    private String currentClassName;

    public InvokeDynamicTransformer(Bozar bozar) {
        super(bozar, "InvokeDynamic Obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        this.currentClassName = classNode.name;

        classContextMap.computeIfAbsent(currentClassName, k -> new ClassContext());
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {

        if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) {
            return;
        }

        ClassContext ctx = classContextMap.computeIfAbsent(currentClassName, k -> new ClassContext());

        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : instructions) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                int opcode = methodInsn.getOpcode();

                if ((opcode == INVOKESTATIC || opcode == INVOKEVIRTUAL) &&
                    shouldObfuscate(methodInsn.owner, methodInsn.name)) {

                    String descriptor;
                    if (opcode == INVOKESTATIC) {
                        descriptor = methodInsn.desc;
                    } else {

                        descriptor = insertOwnerIntoDescriptor(methodInsn.owner, methodInsn.desc);
                    }

                    String key = methodInsn.owner + "." + methodInsn.name;
                    InvokeData invokeData = ctx.invokeMap.computeIfAbsent(key, k ->
                            new InvokeData(ctx.invokeMap.size(), methodInsn.owner, methodInsn.name)
                    );

                    long encodedData = ((long) invokeData.index << 32) |
                            (opcode == INVOKESTATIC ? ctx.invokeStaticMagic : ctx.invokeVirtualMagic);

                    Handle bootstrapHandle = new Handle(
                            H_INVOKESTATIC,
                            currentClassName,
                            ctx.bootstrapMethodName,
                            BOOTSTRAP_METHOD_DESC,
                            false
                    );

                    InvokeDynamicInsnNode invokeDynamicInsn = new InvokeDynamicInsnNode(
                            "invoke",
                            descriptor,
                            bootstrapHandle,
                            encodedData
                    );

                    methodNode.instructions.set(insn, invokeDynamicInsn);
                }
            }
        }
    }

    private String insertOwnerIntoDescriptor(String owner, String desc) {
        Type ownerType = Type.getObjectType(owner);
        Type methodType = Type.getMethodType(desc);
        Type[] argTypes = methodType.getArgumentTypes();

        Type[] newArgTypes = new Type[argTypes.length + 1];
        newArgTypes[0] = ownerType;
        System.arraycopy(argTypes, 0, newArgTypes, 1, argTypes.length);

        return Type.getMethodDescriptor(methodType.getReturnType(), newArgTypes);
    }

    @Override
    public boolean transformOutput(ClassNode classNode) {

        ClassContext ctx = classContextMap.get(classNode.name);
        if (ctx != null && !ctx.invokeMap.isEmpty()) {
            addBootstrapMethod(classNode, ctx, new ArrayList<>(ctx.invokeMap.values()));
        }
        return true;
    }

    private void addBootstrapMethod(ClassNode classNode, ClassContext ctx, List<InvokeData> invokeList) {

        boolean exists = classNode.methods.stream()
                .anyMatch(m -> m.name.equals(ctx.bootstrapMethodName) && m.desc.equals(BOOTSTRAP_METHOD_DESC));

        if (exists) {
            return;
        }

        MethodNode bootstrapMethod = new MethodNode(
                ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                ctx.bootstrapMethodName,
                BOOTSTRAP_METHOD_DESC,
                null,
                new String[]{"java/lang/Exception"}
        );

        InsnList instructions = new InsnList();

        LabelNode labelIndexEnd = new LabelNode();
        LabelNode labelTypeEnd = new LabelNode();
        LabelNode labelError = new LabelNode();
        LabelNode labelStatic = new LabelNode();
        LabelNode labelVirtual = new LabelNode();

        instructions.add(new VarInsnNode(LLOAD, 3));
        instructions.add(new LdcInsnNode(32));
        instructions.add(new InsnNode(LUSHR));
        instructions.add(new InsnNode(L2I));
        instructions.add(new VarInsnNode(ISTORE, 5));

        instructions.add(new VarInsnNode(LLOAD, 3));
        instructions.add(new LdcInsnNode(0xFFFFFFFFL));
        instructions.add(new InsnNode(LAND));
        instructions.add(new InsnNode(L2I));
        instructions.add(new VarInsnNode(ISTORE, 6));

        LabelNode[] indexLabels = new LabelNode[invokeList.size()];
        for (int i = 0; i < invokeList.size(); i++) {
            indexLabels[i] = new LabelNode();
        }

        for (int i = 0; i < invokeList.size(); i++) {
            instructions.add(new VarInsnNode(ILOAD, 5));
            instructions.add(new LdcInsnNode(i));
            instructions.add(new JumpInsnNode(IF_ICMPEQ, indexLabels[i]));
        }

        instructions.add(new JumpInsnNode(GOTO, labelError));

        for (int i = 0; i < invokeList.size(); i++) {
            InvokeData data = invokeList.get(i);
            instructions.add(indexLabels[i]);

            instructions.add(new LdcInsnNode(data.owner.replace('/', '.')));
            instructions.add(new VarInsnNode(ASTORE, 7));
            instructions.add(new LdcInsnNode(data.name));
            instructions.add(new VarInsnNode(ASTORE, 8));
            instructions.add(new JumpInsnNode(GOTO, labelIndexEnd));
        }

        instructions.add(labelIndexEnd);

        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new VarInsnNode(ALOAD, 7));
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findClass",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false));
        instructions.add(new VarInsnNode(ASTORE, 9));

        instructions.add(new VarInsnNode(ILOAD, 6));
        instructions.add(new LdcInsnNode((int) ctx.invokeStaticMagic));
        instructions.add(new JumpInsnNode(IF_ICMPEQ, labelStatic));

        instructions.add(new VarInsnNode(ILOAD, 6));
        instructions.add(new LdcInsnNode((int) ctx.invokeVirtualMagic));
        instructions.add(new JumpInsnNode(IF_ICMPEQ, labelVirtual));

        instructions.add(new JumpInsnNode(GOTO, labelError));

        instructions.add(labelStatic);
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new VarInsnNode(ALOAD, 9));
        instructions.add(new VarInsnNode(ALOAD, 8));
        instructions.add(new VarInsnNode(ALOAD, 2));
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findStatic",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false));
        instructions.add(new VarInsnNode(ASTORE, 10));
        instructions.add(new JumpInsnNode(GOTO, labelTypeEnd));

        instructions.add(labelVirtual);
        instructions.add(new VarInsnNode(ALOAD, 0));
        instructions.add(new VarInsnNode(ALOAD, 9));
        instructions.add(new VarInsnNode(ALOAD, 8));
        instructions.add(new VarInsnNode(ALOAD, 2));

        instructions.add(new LdcInsnNode(0));
        instructions.add(new LdcInsnNode(1));
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodType",
                "dropParameterTypes",
                "(II)Ljava/lang/invoke/MethodType;",
                false));

        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findVirtual",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false));
        instructions.add(new VarInsnNode(ASTORE, 10));
        instructions.add(new JumpInsnNode(GOTO, labelTypeEnd));

        instructions.add(labelError);
        instructions.add(new TypeInsnNode(NEW, "java/lang/IllegalStateException"));
        instructions.add(new InsnNode(DUP));
        instructions.add(new MethodInsnNode(INVOKESPECIAL,
                "java/lang/IllegalStateException",
                "<init>",
                "()V",
                false));
        instructions.add(new InsnNode(ATHROW));

        instructions.add(labelTypeEnd);
        instructions.add(new TypeInsnNode(NEW, "java/lang/invoke/ConstantCallSite"));
        instructions.add(new InsnNode(DUP));
        instructions.add(new VarInsnNode(ALOAD, 10));
        instructions.add(new MethodInsnNode(INVOKESPECIAL,
                "java/lang/invoke/ConstantCallSite",
                "<init>",
                "(Ljava/lang/invoke/MethodHandle;)V",
                false));
        instructions.add(new InsnNode(ARETURN));

        bootstrapMethod.instructions = instructions;
        bootstrapMethod.maxStack = 6;
        bootstrapMethod.maxLocals = 11;

        classNode.methods.add(bootstrapMethod);
    }

    private boolean shouldObfuscate(String owner, String name) {

        if (owner.startsWith("java/") || owner.startsWith("javax/") ||
                owner.startsWith("sun/") || owner.startsWith("jdk/")) {
            return false;
        }

        if (name.equals("<init>") || name.equals("<clinit>") || name.equals("main")) {
            return false;
        }

        return true;
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().isInvokeDynamic(), boolean.class);
    }
}
