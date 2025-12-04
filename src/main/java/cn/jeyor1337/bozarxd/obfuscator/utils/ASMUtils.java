package cn.jeyor1337.bozarxd.obfuscator.utils;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ASMUtils implements Opcodes {

    private ASMUtils() { }

    public static class BuiltInstructions {
        public static InsnList getPrintln(String s) {
            final InsnList insnList = new InsnList();
            insnList.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            insnList.add(new LdcInsnNode(s));
            insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
            return insnList;
        }

        public static InsnList getThrowNull() {
            final InsnList insnList = new InsnList();
            insnList.add(new InsnNode(ACONST_NULL));
            insnList.add(new InsnNode(ATHROW));
            return insnList;
        }
    }

    public static InsnList getCastConvertInsnList(Type type) {
        final InsnList insnList = new InsnList();

        if(type.getDescriptor().equals("V")) {
            insnList.add(new InsnNode(POP));
            return insnList;
        }

        String methodName = switch (type.getDescriptor()) {
            case "I" -> "intValue";
            case "Z" -> "booleanValue";
            case "B" -> "byteValue";
            case "C" -> "charValue";
            case "S" -> "shortValue";
            case "D" -> "doubleValue";
            case "F" -> "floatValue";
            case "J" -> "longValue";
            default -> null;
        };
        if(methodName != null) insnList.add(getCastConvertInsnList(type, getPrimitiveClassType(type), methodName));
        else insnList.add(new TypeInsnNode(CHECKCAST, type.getInternalName()));
        return insnList;
    }

    private static InsnList getCastConvertInsnList(Type type, Type classType, String convertMethodName) {
        return InsnBuilder.createEmpty()
                .insn(new TypeInsnNode(CHECKCAST, classType.getInternalName()))
                .insn(new MethodInsnNode(INVOKEVIRTUAL, classType.getInternalName(), convertMethodName, "()" + type.getDescriptor()))
                .getInsnList();
    }

    private static final Map<String, String> primitives = Map.of(
            "V", "java/lang/Void",
            "I", "java/lang/Integer",
            "Z",  "java/lang/Boolean",
            "B",  "java/lang/Byte",
            "C",  "java/lang/Character",
            "S",  "java/lang/Short",
            "D",  "java/lang/Double",
            "F",  "java/lang/Float",
            "J",  "java/lang/Long"
    );
    public static Type getPrimitiveClassType(Type type) {
        if(!primitives.containsKey(type.getDescriptor()))
            throw new IllegalArgumentException(type + " is not a primitive type");
        return Type.getType("L" + primitives.get(type.getDescriptor()) + ";");
    }

    public static Type getPrimitiveFromClassType(Type type) throws IllegalArgumentException {
        return primitives.entrySet().stream()
                .filter(entry -> entry.getValue().equals(type.getInternalName()))
                .map(Map.Entry::getKey)
                .map(Type::getType)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public static List<Type> getMethodArguments(String desc) {
        String args = desc.substring(1, desc.indexOf(")"));

        List<Type> typeStrings = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean isClass = false, isArray = false;
        for (char c : args.toCharArray()) {
            if(c == '[') {
                isArray = true;
                continue;
            }

            if(c == 'L') isClass = true;
            if(!isClass) {
                Type type = getPrimitiveClassType(Type.getType(String.valueOf(c)));
                if(isArray) type = Type.getType("[" + type);
                typeStrings.add(type);
                isArray = false;
            }
            else {
                sb.append(c);
                if(c == ';') {
                    typeStrings.add(Type.getType((isArray ? "[" : "") + sb));
                    sb = new StringBuilder();

                    isClass = false;
                    isArray = false;
                }
            }
        }
        return typeStrings;
    }

    public static boolean isClassEligibleToModify(ClassNode classNode) {
        return (classNode.access & ACC_INTERFACE) == 0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isMethodEligibleToModify(ClassNode classNode, MethodNode methodNode) {
        return isClassEligibleToModify(classNode) && (methodNode.access & ACC_ABSTRACT) == 0;
    }

    public static byte[] toByteArrayDefault(ClassNode classNode) {
        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static String getName(ClassNode classNode) {
        return classNode.name.replace("/", ".");
    }

    public static String getName(ClassNode classNode, FieldNode fieldNode) {
        return classNode.name + "." + fieldNode.name;
    }

    public static String getName(ClassNode classNode, MethodNode methodNode) {
        return classNode.name + "." + methodNode.name + methodNode.desc;
    }

    public static InsnList arrayToList(AbstractInsnNode[] insns) {
        final InsnList insnList = new InsnList();
        Arrays.stream(insns).forEach(insnList::add);
        return insnList;
    }

    public static boolean isMethodSizeValid(MethodNode methodNode) {
        return getCodeSize(methodNode) <= 65536;
    }

    public static int getCodeSize(MethodNode methodNode) {
        CodeSizeEvaluator cse = new CodeSizeEvaluator(null);
        methodNode.accept(cse);
        return cse.getMaxSize();
    }

    public static MethodNode findOrCreateInit(ClassNode classNode) {
        MethodNode clinit = findMethod(classNode, "<init>", "()V");
        if (clinit == null) {
            clinit = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
            clinit.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(clinit);
        } return clinit;
    }

    public static MethodNode findOrCreateClinit(ClassNode classNode) {
        MethodNode clinit = findMethod(classNode, "<clinit>", "()V");
        if (clinit == null) {
            clinit = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(clinit);
        } return clinit;
    }

    public static MethodNode findMethod(ClassNode classNode, String name, String desc) {
        return classNode.methods
                .stream()
                .filter(methodNode -> name.equals(methodNode.name) && desc.equals(methodNode.desc))
                .findAny()
                .orElse(null);
    }

    public static boolean isInvokeMethod(AbstractInsnNode insn, boolean includeInvokeDynamic) {
        return insn.getOpcode() >= INVOKEVIRTUAL && (includeInvokeDynamic ? insn.getOpcode() <= INVOKEDYNAMIC : insn.getOpcode() < INVOKEDYNAMIC);
    }

    public static boolean isFieldInsn(AbstractInsnNode insn) {
        return insn.getOpcode() >= GETSTATIC && insn.getOpcode() <= PUTFIELD;
    }

    public static boolean isIf(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        return (op >= IFEQ && op <= IF_ACMPNE) || op == IFNULL || op == IFNONNULL;
    }

    public static AbstractInsnNode pushLong(long value) {
        if (value == 0) return new InsnNode(LCONST_0);
        else if (value == 1) return new InsnNode(LCONST_1);
        else return new LdcInsnNode(value);
    }

    public static boolean isPushLong(AbstractInsnNode insn) {
        try {
            getPushedLong(insn);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static long getPushedLong(AbstractInsnNode insn) throws IllegalArgumentException {
        var ex = new IllegalArgumentException("Insn is not a push long instruction");
        return switch (insn.getOpcode()) {
            case LCONST_0 -> 0;
            case LCONST_1 -> 1;
            case LDC -> {
                Object cst = ((LdcInsnNode)insn).cst;
                if (cst instanceof Long)
                    yield (long) cst;
                throw ex;
            }
            default -> throw ex;
        };
    }

    public static AbstractInsnNode pushInt(int value) {
        if (value >= -1 && value <= 5) {
            return new InsnNode(ICONST_0 + value);
        }
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return new IntInsnNode(BIPUSH, value);
        }
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return new IntInsnNode(SIPUSH, value);
        }
        return new LdcInsnNode(value);
    }

    public static boolean isPushInt(AbstractInsnNode insn) {
        try {
            getPushedInt(insn);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static int getPushedInt(AbstractInsnNode insn) throws IllegalArgumentException {
        var ex = new IllegalArgumentException("Insn is not a push int instruction");
        int op = insn.getOpcode();
        return switch (op) {
            case ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> op - ICONST_0;
            case BIPUSH, SIPUSH -> ((IntInsnNode)insn).operand;
            case LDC -> {
                Object cst = ((LdcInsnNode)insn).cst;
                if (cst instanceof Integer)
                    yield  (int) cst;
                throw ex;
            }
            default -> throw ex;
        };
    }

    public static AbstractInsnNode pushFloat(float value) {
        if (value == 0.0f) return new InsnNode(FCONST_0);
        else if (value == 1.0f) return new InsnNode(FCONST_1);
        else if (value == 2.0f) return new InsnNode(FCONST_2);
        else return new LdcInsnNode(value);
    }

    public static boolean isPushFloat(AbstractInsnNode insn) {
        try {
            getPushedFloat(insn);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static float getPushedFloat(AbstractInsnNode insn) throws IllegalArgumentException {
        var ex = new IllegalArgumentException("Insn is not a push float instruction");
        return switch (insn.getOpcode()) {
            case FCONST_0 -> 0.0f;
            case FCONST_1 -> 1.0f;
            case FCONST_2 -> 2.0f;
            case LDC -> {
                Object cst = ((LdcInsnNode)insn).cst;
                if (cst instanceof Float)
                    yield (float) cst;
                throw ex;
            }
            default -> throw ex;
        };
    }

    public static AbstractInsnNode pushDouble(double value) {
        if (value == 0.0d) return new InsnNode(DCONST_0);
        else if (value == 1.0d) return new InsnNode(DCONST_1);
        else return new LdcInsnNode(value);
    }

    public static boolean isPushDouble(AbstractInsnNode insn) {
        try {
            getPushedDouble(insn);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static double getPushedDouble(AbstractInsnNode insn) throws IllegalArgumentException {
        var ex = new IllegalArgumentException("Insn is not a push double instruction");
        return switch (insn.getOpcode()) {
            case DCONST_0 -> 0.0d;
            case DCONST_1 -> 1.0d;
            case LDC -> {
                Object cst = ((LdcInsnNode)insn).cst;
                if (cst instanceof Double)
                    yield (double) cst;
                throw ex;
            }
            default -> throw ex;
        };
    }

    public static void boxPrimitive(String desc, InsnList list) {
        switch (desc) {
            case "I" -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
            case "Z" -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
            case "B" -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
            case "C" -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false));
            case "S" -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false));
            case "J" -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
            case "F" -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
            case "D" -> list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
            default -> {
                if (!desc.equals("Lnull;") && !desc.equals("Ljava/lang/Object;")) {
                    String internalName = desc.startsWith("L") && desc.endsWith(";") ? desc.substring(1, desc.length() - 1) : desc;
                    list.add(new TypeInsnNode(CHECKCAST, internalName));
                }
            }
        }
    }

    public static void unboxPrimitive(String desc, InsnList list) {
        switch (desc) {
            case "I" -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
            }
            case "Z" -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Boolean"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
            }
            case "B" -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Byte"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false));
            }
            case "C" -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Character"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false));
            }
            case "S" -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Short"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false));
            }
            case "J" -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Long"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
            }
            case "F" -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Float"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
            }
            case "D" -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Double"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
            }
            default -> {
                if (!desc.equals("Lnull;") && !desc.equals("Ljava/lang/Object;")) {
                    String internalName = desc.startsWith("L") && desc.endsWith(";") ? desc.substring(1, desc.length() - 1) : desc;
                    list.add(new TypeInsnNode(CHECKCAST, internalName));
                }
            }
        }
    }

    public static String parentName(String name) {
        if (name.contains("/")) {
            return name.substring(0, name.lastIndexOf("/") + 1);
        }
        return "";
    }
}
