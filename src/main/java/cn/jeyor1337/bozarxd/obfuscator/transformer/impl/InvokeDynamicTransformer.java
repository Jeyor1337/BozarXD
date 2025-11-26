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

    // Per-class context for randomized bootstrap name and magic numbers
    private static class ClassContext {
        final String bootstrapMethodName;
        final long invokeStaticMagic;
        final long invokeVirtualMagic;
        final LinkedHashMap<String, InvokeData> invokeMap = new LinkedHashMap<>();

        ClassContext() {
            // Generate random bootstrap method name (8-12 chars)
            this.bootstrapMethodName = generateRandomName();
            // Generate random magic numbers (ensure they are different and non-zero)
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

    // Track per-class context
    private final Map<String, ClassContext> classContextMap = new HashMap<>();
    private String currentClassName;

    public InvokeDynamicTransformer(Bozar bozar) {
        super(bozar, "InvokeDynamic Obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        this.currentClassName = classNode.name;
        // Create context for this class with randomized bootstrap name and magic numbers
        classContextMap.computeIfAbsent(currentClassName, k -> new ClassContext());
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        // Skip constructor and static initializer
        if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) {
            return;
        }

        // Get class context (with randomized bootstrap name and magic numbers)
        ClassContext ctx = classContextMap.computeIfAbsent(currentClassName, k -> new ClassContext());

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
                    InvokeData invokeData = ctx.invokeMap.computeIfAbsent(key, k ->
                            new InvokeData(ctx.invokeMap.size(), methodInsn.owner, methodInsn.name)
                    );

                    // Encode data: (index << 32) | invokeType (using randomized magic numbers)
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
        ClassContext ctx = classContextMap.get(classNode.name);
        if (ctx != null && !ctx.invokeMap.isEmpty()) {
            addBootstrapMethod(classNode, ctx, new ArrayList<>(ctx.invokeMap.values()));
        }
        return true;
    }

    private void addBootstrapMethod(ClassNode classNode, ClassContext ctx, List<InvokeData> invokeList) {
        // Check if bootstrap method already exists
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

        // Local variable slots
        // 0: MethodHandles.Lookup lookup
        // 1: String name (unused)
        // 2: MethodType type
        // 3-4: long data (takes 2 slots)
        // 5: int index (extracted from high 32 bits)
        // 6: int invokeType (extracted from low 32 bits)
        // 7: String owner
        // 8: String methodName
        // 9: Class refc
        // 10: MethodHandle methodHandle

        LabelNode labelIndexEnd = new LabelNode();
        LabelNode labelTypeEnd = new LabelNode();
        LabelNode labelError = new LabelNode();
        LabelNode labelStatic = new LabelNode();
        LabelNode labelVirtual = new LabelNode();

        // Extract index: index = (int)(data >>> 32)
        instructions.add(new VarInsnNode(LLOAD, 3)); // load data
        instructions.add(new LdcInsnNode(32));
        instructions.add(new InsnNode(LUSHR));
        instructions.add(new InsnNode(L2I));
        instructions.add(new VarInsnNode(ISTORE, 5)); // store index

        // Extract invokeType: invokeType = (int)(data & 0xFFFFFFFF)
        instructions.add(new VarInsnNode(LLOAD, 3)); // load data
        instructions.add(new LdcInsnNode(0xFFFFFFFFL));
        instructions.add(new InsnNode(LAND));
        instructions.add(new InsnNode(L2I));
        instructions.add(new VarInsnNode(ISTORE, 6)); // store invokeType

        // === FIRST IF-ELSE CHAIN: Method index dispatch ===
        // Generate if-else chain for method index lookup (replaces TableSwitch)
        LabelNode[] indexLabels = new LabelNode[invokeList.size()];
        for (int i = 0; i < invokeList.size(); i++) {
            indexLabels[i] = new LabelNode();
        }

        // Generate: if (index == 0) goto label0; else if (index == 1) goto label1; ...
        for (int i = 0; i < invokeList.size(); i++) {
            instructions.add(new VarInsnNode(ILOAD, 5)); // load index
            instructions.add(new LdcInsnNode(i));
            instructions.add(new JumpInsnNode(IF_ICMPEQ, indexLabels[i]));
        }
        // Default: error
        instructions.add(new JumpInsnNode(GOTO, labelError));

        // Generate code for each index case
        for (int i = 0; i < invokeList.size(); i++) {
            InvokeData data = invokeList.get(i);
            instructions.add(indexLabels[i]);

            // Load owner and name
            instructions.add(new LdcInsnNode(data.owner.replace('/', '.')));
            instructions.add(new VarInsnNode(ASTORE, 7)); // store owner
            instructions.add(new LdcInsnNode(data.name));
            instructions.add(new VarInsnNode(ASTORE, 8)); // store methodName
            instructions.add(new JumpInsnNode(GOTO, labelIndexEnd));
        }

        // After index dispatch: find class
        instructions.add(labelIndexEnd);

        // lookup.findClass(owner)
        instructions.add(new VarInsnNode(ALOAD, 0)); // lookup
        instructions.add(new VarInsnNode(ALOAD, 7)); // owner
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findClass",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false));
        instructions.add(new VarInsnNode(ASTORE, 9)); // store refc

        // === SECOND IF-ELSE CHAIN: Invoke type dispatch ===
        // if (invokeType == INVOKE_STATIC_MAGIC) goto labelStatic
        instructions.add(new VarInsnNode(ILOAD, 6)); // load invokeType
        instructions.add(new LdcInsnNode((int) ctx.invokeStaticMagic));
        instructions.add(new JumpInsnNode(IF_ICMPEQ, labelStatic));

        // else if (invokeType == INVOKE_VIRTUAL_MAGIC) goto labelVirtual
        instructions.add(new VarInsnNode(ILOAD, 6)); // load invokeType
        instructions.add(new LdcInsnNode((int) ctx.invokeVirtualMagic));
        instructions.add(new JumpInsnNode(IF_ICMPEQ, labelVirtual));

        // else goto error
        instructions.add(new JumpInsnNode(GOTO, labelError));

        // Case: INVOKESTATIC
        instructions.add(labelStatic);
        instructions.add(new VarInsnNode(ALOAD, 0)); // lookup
        instructions.add(new VarInsnNode(ALOAD, 9)); // refc
        instructions.add(new VarInsnNode(ALOAD, 8)); // methodName
        instructions.add(new VarInsnNode(ALOAD, 2)); // methodType
        instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles$Lookup",
                "findStatic",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false));
        instructions.add(new VarInsnNode(ASTORE, 10)); // store methodHandle
        instructions.add(new JumpInsnNode(GOTO, labelTypeEnd));

        // Case: INVOKEVIRTUAL
        instructions.add(labelVirtual);
        instructions.add(new VarInsnNode(ALOAD, 0)); // lookup
        instructions.add(new VarInsnNode(ALOAD, 9)); // refc
        instructions.add(new VarInsnNode(ALOAD, 8)); // methodName
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
        instructions.add(new VarInsnNode(ASTORE, 10)); // store methodHandle
        instructions.add(new JumpInsnNode(GOTO, labelTypeEnd));

        // Error case: throw IllegalStateException
        instructions.add(labelError);
        instructions.add(new TypeInsnNode(NEW, "java/lang/IllegalStateException"));
        instructions.add(new InsnNode(DUP));
        instructions.add(new MethodInsnNode(INVOKESPECIAL,
                "java/lang/IllegalStateException",
                "<init>",
                "()V",
                false));
        instructions.add(new InsnNode(ATHROW));

        // After type dispatch: create ConstantCallSite and return
        instructions.add(labelTypeEnd);
        instructions.add(new TypeInsnNode(NEW, "java/lang/invoke/ConstantCallSite"));
        instructions.add(new InsnNode(DUP));
        instructions.add(new VarInsnNode(ALOAD, 10)); // methodHandle
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
