package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class ParamObfTransformer extends ClassTransformer {

    private static final int EXTRA_PARAM_COUNT = 3;
    private static final String[] STANDARD_TYPES = {"I", "J", "F", "D", "Z", "B", "C", "S"};

    // Map: className + methodName + oldDesc -> newDesc
    private final Map<String, String> methodDescMap = new HashMap<>();
    // Map: className + methodName + oldDesc -> list of added param types
    private final Map<String, List<Type>> addedParamsMap = new HashMap<>();

    public ParamObfTransformer(Bozar bozar) {
        super(bozar, "Parameter Obfuscation", BozarCategory.ADVANCED);
    }

    @Override
    public void pre() {
        // First pass: collect all methods that need to be modified and generate new descriptors
        for (ClassNode classNode : this.getBozar().getClasses()) {
            for (MethodNode methodNode : classNode.methods) {
                if (shouldObfuscate(classNode, methodNode)) {
                    String key = getMethodKey(classNode.name, methodNode.name, methodNode.desc);
                    String newDesc = expandMethodDescriptor(key, methodNode.desc);
                    methodDescMap.put(key, newDesc);

                    this.getBozar().log("Expanding method %s%s -> %s",
                        classNode.name + "." + methodNode.name, methodNode.desc, newDesc);
                }
            }
        }
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        String key = getMethodKey(classNode.name, methodNode.name, methodNode.desc);

        // Modify method definition if it's in our map
        if (methodDescMap.containsKey(key)) {
            String newDesc = methodDescMap.get(key);
            List<Type> addedParams = addedParamsMap.get(key);

            if (addedParams == null) {
                this.getBozar().log("Warning: No added params found for method %s", key);
                return;
            }

            // Update method descriptor
            methodNode.desc = newDesc;

            // Add obfuscation operations at the beginning of the method
            addObfuscationCode(methodNode, addedParams);
        }

        // Modify method calls - this handles both same-class and cross-class calls
        AbstractInsnNode[] instructions = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : instructions) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                String callKey = getMethodKey(methodInsn.owner, methodInsn.name, methodInsn.desc);

                if (methodDescMap.containsKey(callKey)) {
                    String newDesc = methodDescMap.get(callKey);
                    List<Type> addedParams = addedParamsMap.get(callKey);

                    if (addedParams == null) {
                        this.getBozar().log("Warning: No added params found for call to %s", callKey);
                        continue;
                    }

                    // Insert default values for added parameters before the method call
                    InsnList insertList = new InsnList();
                    for (Type paramType : addedParams) {
                        insertList.add(getDefaultValueInsn(paramType));
                    }

                    methodNode.instructions.insertBefore(insn, insertList);

                    // Update the method call descriptor
                    methodInsn.desc = newDesc;

                    this.getBozar().log("Updated call to %s in %s.%s",
                        methodInsn.name, classNode.name, methodNode.name);
                }
            }
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

        // Skip synthetic and bridge methods (includes lambdas and compiler-generated methods)
        if ((methodNode.access & ACC_SYNTHETIC) != 0 ||
            (methodNode.access & ACC_BRIDGE) != 0) {
            return false;
        }

        // Skip main method
        if (methodNode.name.equals("main") && methodNode.desc.equals("([Ljava/lang/String;)V")) {
            return false;
        }

        BozarConfig.BozarOptions.ParamObfuscationOption level =
            this.getBozar().getConfig().getOptions().getParamObfuscation();

        boolean isPrivate = (methodNode.access & ACC_PRIVATE) != 0;
        boolean isStatic = (methodNode.access & ACC_STATIC) != 0;
        boolean isPublic = (methodNode.access & ACC_PUBLIC) != 0;
        boolean isProtected = (methodNode.access & ACC_PROTECTED) != 0;
        boolean isPackagePrivate = !isPrivate && !isPublic && !isProtected;

        switch (level) {
            case OFF:
                return false;

            case CONSERVATIVE:
                // Only private static methods
                // Safest: avoids reflection issues with private instance methods
                return isPrivate && isStatic;

            case MODERATE:
                // Private static methods + package-private static methods
                // Avoids reflection issues with private instance methods
                return (isPrivate && isStatic) || (isStatic && isPackagePrivate);

            case AGGRESSIVE:
                // Private static methods + all other static methods
                // Avoids reflection issues with private instance methods, but may break external API calls
                return isStatic;

            default:
                return false;
        }
    }

    private String expandMethodDescriptor(String key, String descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        Type returnType = Type.getReturnType(descriptor);

        List<Type> addedParams = new ArrayList<>();
        StringBuilder newDesc = new StringBuilder("(");

        // Keep original parameters
        for (Type argType : argumentTypes) {
            newDesc.append(argType.getDescriptor());
        }

        // Add random parameters
        for (int i = 0; i < EXTRA_PARAM_COUNT; i++) {
            Type randomType = generateRandomType();
            addedParams.add(randomType);
            newDesc.append(randomType.getDescriptor());
        }

        newDesc.append(")").append(returnType.getDescriptor());

        // Store added params for later use
        addedParamsMap.put(key, addedParams);

        return newDesc.toString();
    }

    private Type generateRandomType() {
        String typeDesc = STANDARD_TYPES[random.nextInt(STANDARD_TYPES.length)];
        return Type.getType(typeDesc);
    }

    private void addObfuscationCode(MethodNode methodNode, List<Type> addedParams) {
        if (methodNode.instructions == null || methodNode.instructions.size() == 0) {
            methodNode.instructions = new InsnList();
        }

        Type[] allArgs = Type.getArgumentTypes(methodNode.desc);
        boolean isStatic = (methodNode.access & ACC_STATIC) != 0;

        // Calculate the starting local variable index for added params
        int paramIndex = isStatic ? 0 : 1;
        for (int i = 0; i < allArgs.length - addedParams.size(); i++) {
            paramIndex += allArgs[i].getSize();
        }

        // Insert obfuscation operations at the beginning
        InsnList obfuscationCode = new InsnList();
        LabelNode startLabel = new LabelNode();
        obfuscationCode.add(startLabel);

        for (int i = 0; i < addedParams.size(); i++) {
            Type paramType = addedParams.get(i);
            obfuscationCode.add(generateObfuscationInsns(paramType, paramIndex));
            paramIndex += paramType.getSize();
        }

        // Insert at the beginning of the method
        AbstractInsnNode firstInsn = methodNode.instructions.getFirst();
        if (firstInsn != null) {
            methodNode.instructions.insertBefore(firstInsn, obfuscationCode);
        } else {
            methodNode.instructions.add(obfuscationCode);
        }
    }

    private InsnList generateObfuscationInsns(Type paramType, int varIndex) {
        InsnList insns = new InsnList();

        switch (paramType.getSort()) {
            case Type.INT:
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
                insns.add(new VarInsnNode(ILOAD, varIndex));
                insns.add(new InsnNode(ICONST_1));
                insns.add(new InsnNode(IADD));
                insns.add(new InsnNode(ICONST_1));
                insns.add(new InsnNode(ISUB));
                insns.add(new InsnNode(POP));
                break;
            case Type.LONG:
                insns.add(new VarInsnNode(LLOAD, varIndex));
                insns.add(new InsnNode(LCONST_1));
                insns.add(new InsnNode(LADD));
                insns.add(new InsnNode(LCONST_1));
                insns.add(new InsnNode(LSUB));
                insns.add(new InsnNode(POP2));
                break;
            case Type.FLOAT:
                insns.add(new VarInsnNode(FLOAD, varIndex));
                insns.add(new InsnNode(FCONST_1));
                insns.add(new InsnNode(FADD));
                insns.add(new InsnNode(FCONST_1));
                insns.add(new InsnNode(FSUB));
                insns.add(new InsnNode(POP));
                break;
            case Type.DOUBLE:
                insns.add(new VarInsnNode(DLOAD, varIndex));
                insns.add(new InsnNode(DCONST_1));
                insns.add(new InsnNode(DADD));
                insns.add(new InsnNode(DCONST_1));
                insns.add(new InsnNode(DSUB));
                insns.add(new InsnNode(POP2));
                break;
        }

        return insns;
    }

    private AbstractInsnNode getDefaultValueInsn(Type paramType) {
        switch (paramType.getSort()) {
            case Type.INT:
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
                return new InsnNode(ICONST_0);
            case Type.LONG:
                return new InsnNode(LCONST_0);
            case Type.FLOAT:
                return new InsnNode(FCONST_0);
            case Type.DOUBLE:
                return new InsnNode(DCONST_0);
            default:
                return new InsnNode(ACONST_NULL);
        }
    }

    private String getMethodKey(String owner, String name, String desc) {
        return owner + "." + name + desc;
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getParamObfuscation() != BozarConfig.BozarOptions.ParamObfuscationOption.OFF,
            BozarConfig.BozarOptions.ParamObfuscationOption.class
        );
    }
}
