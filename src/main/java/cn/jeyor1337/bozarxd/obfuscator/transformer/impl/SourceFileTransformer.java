package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.ClassNode;

public class SourceFileTransformer extends ClassTransformer {

    public SourceFileTransformer(Bozar bozar) {
        super(bozar, "Remove SourceFile", BozarCategory.STABLE);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        classNode.sourceFile = "";
        classNode.sourceDebug = "";
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().isRemoveSourceFile(), boolean.class);
    }
}
