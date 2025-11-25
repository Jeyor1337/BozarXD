package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class InvokeDynamicTransformer extends ClassTransformer {

    private static final String BOOTSTRAP_METHOD_NAME = "bootstrap";
    private static final String BOOTSTRAP_METHOD_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;" +
            "Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/CallSite;";

    // Invoke type encoding (using magic numbers like GotoObfuscator)
    private static final long INVOKE_STATIC = 0x1FFFFFF2L;
    private static final long INVOKE_VIRTUAL = 0x1FFFFFF3L;

    // Inner class to store method invocation data
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

    // Track method mappings per class
    private final Map<String, LinkedHashMap<String, InvokeData>> classInvokeMap = new HashMap<>();
    private String currentClassName;

    public InvokeDynamicTransformer(Bozar bozar) {
        super(bozar, "InvokeDynamic Obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        this.currentClassName = classNode.name;
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        // Skip constructor and static initializer
        if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) {
            return;
        }

        // Get or create invoke map for this class
        LinkedHashMap<String, InvokeData> invokeMap = classInvokeMap.computeIfAbsent(
                currentClassName, k -> new LinkedHashMap<>()
        );

        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : instructions) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                int opcode = methodInsn.getOpcode();

                // Process INVOKESTATIC and INVOKEVIRTUAL
                if ((opcode == INVOKESTATIC || opcode == INVOKEVIRTUAL) &&
                    shouldObfuscate(methodInsn.owner, methodInsn.name)) {

                    // Compute descriptor based on invocation type
                    String descriptor;
                    if (opcode == INVOKESTATIC) {
                        descriptor = methodInsn.desc;
                    } else {
                        // For virtual calls, insert owner type as first parameter
                        descriptor = insertOwnerIntoDescriptor(methodInsn.owner, methodInsn.desc);
                    }

                    // Get or create InvokeData for this method
                    String key = methodInsn.owner + "." + methodInsn.name;
                    InvokeData invokeData = invokeMap.computeIfAbsent(key, k ->
                            new InvokeData(invokeMap.size(), methodInsn.owner, methodInsn.name)
                    );

                    // Encode data: (index << 32) | invokeType
                    long encodedData = ((long) invokeData.index << 32) |
                            (opcode == INVOKESTATIC ? INVOKE_STATIC : INVOKE_VIRTUAL);

                    Handle bootstrapHandle = new Handle(
                            H_INVOKESTATIC,
                            currentClassName,
                            BOOTSTRAP_METHOD_NAME,
                            BOOTSTRAP_METHOD_DESC,
                            false
                    );

                    InvokeDynamicInsnNode invokeDynamicInsn = new InvokeDynamicInsnNode(
                            "invoke", // Generic name
                            descriptor,
                            bootstrapHandle,
                            encodedData
                    );

                    methodNode.instructions.set(insn, invokeDynamicInsn);
                }
            }
        }
    }

    /**
     * Insert owner type as first parameter for virtual method calls
     * Example: (Ljava/lang/String;)V -> (Lcom/Owner;Ljava/lang/String;)V
     */
    private String insertOwnerIntoDescriptor(String owner, String desc) {
        Type ownerType = Type.getObjectType(owner);
        Type methodType = Type.getMethodType(desc);
        Type[] argTypes = methodType.getArgumentTypes();

        // Create new argument array with owner as first parameter
        Type[] newArgTypes = new Type[argTypes.length + 1];
        newArgTypes[0] = ownerType;
        System.arraycopy(argTypes, 0, newArgTypes, 1, argTypes.length);

        return Type.getMethodDescriptor(methodType.getReturnType(), newArgTypes);
    }

    @Override
    public boolean transformOutput(ClassNode classNode) {
        // Add bootstrap method if this class has invoke mappings
        LinkedHashMap<String, InvokeData> invokeMap = classInvokeMap.get(classNode.name);
        if (invokeMap != null && !invokeMap.isEmpty()) {
            addBootstrapMethod(classNode, new ArrayList<>(invokeMap.values()));
        }
        return true;
    }

    private void addBootstrapMethod(ClassNode classNode, List<InvokeData> invokeList) {
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

        // Local variable slots
        // 0: MethodHandles.Lookup lookup
        // 1: String name (unused)
        // 2: MethodType type
        // 3-4: long data (takes 2 slots)
        // 5: String owner
        // 6: String methodName
        // 7: Class refc
        // 8: MethodHandle methodHandle

        LabelNode labelSwitchIndexEnd = new LabelNode();
        LabelNode labelSwitchTypeEnd = new LabelNode();
        LabelNode labelError = new LabelNode();

        // First tableswitch: decode method index from high 32 bits
        // Extract index: data >>> 32
        instructions.add(new VarInsnNode(LLOAD, 3)); // load data
        instructions.add(new LdcInsnNode(32));
        instructions.add(new InsnNode(LUSHR));
        instructions.add(new InsnNode(L2I));

        // Create labels for each method index
        LabelNode[] indexLabels = new LabelNode[invokeList.size()];
        for (int i = 0; i < invokeList.size(); i++) {
            indexLabels[i] = new LabelNode();
        }

        instructions.add(new TableSwitchInsnNode(
                0,
                invokeList.size() - 1,
                labelError,
                indexLabels
        ));

        // Generate code for each index case
        for (int i = 0; i < invokeList.size(); i++) {
            InvokeData data = invokeList.get(i);
            instructions.add(indexLabels[i]);
            instructions.add(new FrameNode(F_SAME, 0, null, 0, null));

            // Load owner and name
            instructions.add(new LdcInsnNode(data.owner.replace('/', '.')));
            instructions.add(new VarInsnNode(ASTORE, 5)); // store owner
            instructions.add(new LdcInsnNode(data.name));
            instructions.add(new VarInsnNode(ASTORE, 6)); // store methodName
            instructions.add(new JumpInsnNode(GOTO, labelSwitchIndexEnd));
        }

        // After index switch: find class
        instructions.add(labelSwitchIndexEnd);
        instructions.add(new FrameNode(F_APPEND, 2, new Object[]{"java/lang/String", "java/lang/String"}, 0, null));

        // lookup.findClass(owner)
        instructions.add(new VarInsnNode(ALOAD, 0)); // lookup
        instructions.add(new VarInsnNode(ALOAD, 5)); // owner
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findClass",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false));
        instructions.add(new VarInsnNode(ASTORE, 7)); // store refc

        // Second tableswitch: decode invoke type from low 32 bits
        // Extract type: data & 0xFFFFFFFF
        instructions.add(new VarInsnNode(LLOAD, 3)); // load data
        instructions.add(new LdcInsnNode(0xFFFFFFFFL));
        instructions.add(new InsnNode(LAND));
        instructions.add(new InsnNode(L2I));

        LabelNode labelStatic = new LabelNode();
        LabelNode labelVirtual = new LabelNode();

        instructions.add(new TableSwitchInsnNode(
                (int) INVOKE_STATIC,
                (int) INVOKE_VIRTUAL,
                labelError,
                labelStatic,
                labelVirtual
        ));

        // Case: INVOKESTATIC
        instructions.add(labelStatic);
        instructions.add(new FrameNode(F_APPEND, 1, new Object[]{"java/lang/Class"}, 0, null));
        instructions.add(new VarInsnNode(ALOAD, 0)); // lookup
        instructions.add(new VarInsnNode(ALOAD, 7)); // refc
        instructions.add(new VarInsnNode(ALOAD, 6)); // methodName
        instructions.add(new VarInsnNode(ALOAD, 2)); // methodType
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findStatic",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false));
        instructions.add(new VarInsnNode(ASTORE, 8)); // store methodHandle
        instructions.add(new JumpInsnNode(GOTO, labelSwitchTypeEnd));

        // Case: INVOKEVIRTUAL
        instructions.add(labelVirtual);
        instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
        instructions.add(new VarInsnNode(ALOAD, 0)); // lookup
        instructions.add(new VarInsnNode(ALOAD, 7)); // refc
        instructions.add(new VarInsnNode(ALOAD, 6)); // methodName
        instructions.add(new VarInsnNode(ALOAD, 2)); // methodType

        // Drop first parameter (we added owner to descriptor, but findVirtual expects original)
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
        instructions.add(new VarInsnNode(ASTORE, 8)); // store methodHandle
        instructions.add(new JumpInsnNode(GOTO, labelSwitchTypeEnd));

        // Error case: throw IllegalStateException
        instructions.add(labelError);
        instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
        instructions.add(new TypeInsnNode(NEW, "java/lang/IllegalStateException"));
        instructions.add(new InsnNode(DUP));
        instructions.add(new MethodInsnNode(INVOKESPECIAL,
                "java/lang/IllegalStateException",
                "<init>",
                "()V",
                false));
        instructions.add(new InsnNode(ATHROW));

        // After type switch: create ConstantCallSite and return
        instructions.add(labelSwitchTypeEnd);
        instructions.add(new FrameNode(F_APPEND, 1, new Object[]{"java/lang/invoke/MethodHandle"}, 0, null));
        instructions.add(new TypeInsnNode(NEW, "java/lang/invoke/ConstantCallSite"));
        instructions.add(new InsnNode(DUP));
        instructions.add(new VarInsnNode(ALOAD, 8)); // methodHandle
        instructions.add(new MethodInsnNode(INVOKESPECIAL,
                "java/lang/invoke/ConstantCallSite",
                "<init>",
                "(Ljava/lang/invoke/MethodHandle;)V",
                false));
        instructions.add(new InsnNode(ARETURN));

        bootstrapMethod.instructions = instructions;
        bootstrapMethod.maxStack = 6;
        bootstrapMethod.maxLocals = 9;

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
