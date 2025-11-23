package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

public class InvokeDynamicTransformer extends ClassTransformer {

    private static final String BOOTSTRAP_METHOD_NAME = "bootstrap";
    private static final String BOOTSTRAP_METHOD_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;" +
            "Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/invoke/CallSite;";

    private boolean hasBootstrapMethod = false;
    private String currentClassName;

    public InvokeDynamicTransformer(Bozar bozar) {
        super(bozar, "InvokeDynamic Obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        this.currentClassName = classNode.name;
        this.hasBootstrapMethod = false;
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        // Skip constructor and static initializer
        if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) {
            return;
        }

        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : instructions) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;

                // Only process static method calls
                if (methodInsn.getOpcode() == INVOKESTATIC && shouldObfuscate(methodInsn.owner, methodInsn.name)) {
                    hasBootstrapMethod = true;

                    Handle bootstrapHandle = new Handle(
                            H_INVOKESTATIC,
                            currentClassName,
                            BOOTSTRAP_METHOD_NAME,
                            BOOTSTRAP_METHOD_DESC,
                            false
                    );

                    InvokeDynamicInsnNode invokeDynamicInsn = new InvokeDynamicInsnNode(
                            methodInsn.name,
                            methodInsn.desc,
                            bootstrapHandle,
                            methodInsn.owner.replace('/', '.'),
                            methodInsn.name
                    );

                    methodNode.instructions.set(insn, invokeDynamicInsn);
                }
            }
        }
    }

    @Override
    public boolean transformOutput(ClassNode classNode) {
        // Add bootstrap method if needed
        if (hasBootstrapMethod && currentClassName != null && currentClassName.equals(classNode.name)) {
            addBootstrapMethod(classNode);
        }
        return true;
    }

    private void addBootstrapMethod(ClassNode classNode) {
        // Check if bootstrap method already exists
        boolean exists = classNode.methods.stream()
                .anyMatch(m -> m.name.equals(BOOTSTRAP_METHOD_NAME) && m.desc.equals(BOOTSTRAP_METHOD_DESC));

        if (exists) {
            return;
        }

        MethodNode bootstrapMethod = new MethodNode(
                ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                BOOTSTRAP_METHOD_NAME,
                BOOTSTRAP_METHOD_DESC,
                null,
                new String[]{"java/lang/Exception"}
        );

        InsnList instructions = new InsnList();

        // new ConstantCallSite
        instructions.add(new TypeInsnNode(NEW, "java/lang/invoke/ConstantCallSite"));
        instructions.add(new InsnNode(DUP));

        // Class.forName(className)
        instructions.add(new VarInsnNode(ALOAD, 3)); // className parameter
        instructions.add(new MethodInsnNode(INVOKESTATIC,
                "java/lang/Class",
                "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false));

        // lookup.findStatic(class, methodName, methodType)
        instructions.add(new VarInsnNode(ALOAD, 0)); // lookup parameter
        instructions.add(new InsnNode(SWAP));
        instructions.add(new VarInsnNode(ALOAD, 4)); // methodName parameter
        instructions.add(new VarInsnNode(ALOAD, 2)); // methodType parameter
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findStatic",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false));

        // new ConstantCallSite(methodHandle)
        instructions.add(new MethodInsnNode(INVOKESPECIAL,
                "java/lang/invoke/ConstantCallSite",
                "<init>",
                "(Ljava/lang/invoke/MethodHandle;)V",
                false));

        instructions.add(new InsnNode(ARETURN));

        bootstrapMethod.instructions = instructions;
        bootstrapMethod.maxStack = 6;
        bootstrapMethod.maxLocals = 5;

        classNode.methods.add(bootstrapMethod);
    }

    private boolean shouldObfuscate(String owner, String name) {
        // Don't obfuscate Java standard library calls
        if (owner.startsWith("java/") || owner.startsWith("javax/") ||
                owner.startsWith("sun/") || owner.startsWith("jdk/")) {
            return false;
        }

        // Don't obfuscate special methods
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
