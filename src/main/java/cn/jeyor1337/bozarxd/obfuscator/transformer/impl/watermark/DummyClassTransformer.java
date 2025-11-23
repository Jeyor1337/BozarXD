package cn.jeyor1337.bozarxd.obfuscator.transformer.impl.watermark;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class DummyClassTransformer extends ClassTransformer {

    public DummyClassTransformer(Bozar bozar) {
        super(bozar, "Dummy class", BozarCategory.WATERMARK);
    }

    @Override
    public void transformOutput(JarOutputStream jarOutputStream) {
        ClassNode dummy = new ClassNode();
        dummy.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, this.getBozar().getConfig().getOptions().getWatermarkOptions().getDummyClassText(), null, "java/lang/Object", null);
        dummy.visitMethod(random.nextInt(100), "\u0001", "(\u0001/)L\u0001/;", null, null);
        try {
            jarOutputStream.putNextEntry(new JarEntry(dummy.name + ".class"));
            jarOutputStream.write(ASMUtils.toByteArrayDefault(dummy));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().getWatermarkOptions().isDummyClass(), ".OBFUSCATED WITH BOZAR");
    }
}
