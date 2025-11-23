package cn.jeyor1337.bozarxd.obfuscator.transformer.impl.renamer;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.RenamerTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.ResourceWrapper;
import org.objectweb.asm.tree.ClassNode;

public class ClassRenamerTransformer extends RenamerTransformer {

    public ClassRenamerTransformer(Bozar bozar) {
        super(bozar, "Rename", BozarCategory.STABLE);
    }

    @Override
    public void transformClass(ClassNode classNode) {
        this.registerMap(classNode.name);
    }

    @Override
    public void transformResource(ResourceWrapper resource) {
        if(resource.getZipEntry().isDirectory()) return;

        String str = new String(resource.getBytes());
        for (var set : map.entrySet()) {
            String s1 = set.getKey().replace("/", ".");
            String s2 = set.getValue().replace("/", ".");
            str = str.replace(s1, s2);
        } resource.setBytes(str.getBytes());
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().getRename() != this.getEnableType().type(), BozarConfig.BozarOptions.RenameOption.OFF);
    }
}
