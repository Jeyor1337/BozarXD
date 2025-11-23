package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class CrasherTransformer extends ClassTransformer {

    public CrasherTransformer(Bozar bozar) {
        super(bozar, "Decompiler crasher", BozarCategory.ADVANCED);
    }

    public static final String PACKAGE_NAME;
    public static final String REPEAT_BASE = "\u0001/";

    static {
        int caseNum;
        PACKAGE_NAME = switch (caseNum = ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> "com";
            case 1 -> "net";
            case 2 -> "io";
            case 3 -> "org";
            default -> throw new IllegalArgumentException("Invalid PACKAGE_NAME case " + caseNum);
        };
    }

    @Override
    public void transformOutput(JarOutputStream jarOutputStream) {
        ClassNode invalid = new ClassNode();
        invalid.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, PACKAGE_NAME + REPEAT_BASE.repeat((Character.MAX_VALUE / REPEAT_BASE.length()) - PACKAGE_NAME.length()), null, "java/lang/Object", null);
        try {
            jarOutputStream.putNextEntry(new JarEntry("\u0020".repeat(4) + ".class"));
            jarOutputStream.write(ASMUtils.toByteArrayDefault(invalid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().isCrasher(), boolean.class);
    }
}
