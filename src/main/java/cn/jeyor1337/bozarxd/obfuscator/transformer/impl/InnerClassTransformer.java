package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;

public class InnerClassTransformer extends ClassTransformer {

    public InnerClassTransformer(Bozar bozar) {
        super(bozar, "Remove inner classes", BozarCategory.STABLE);
    }

    public void transformClass(ClassNode classNode) {
        classNode.innerClasses = new ArrayList<>();
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().isRemoveInnerClasses(), boolean.class);
    }
}
