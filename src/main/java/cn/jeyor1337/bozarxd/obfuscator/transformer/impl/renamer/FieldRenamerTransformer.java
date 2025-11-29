package cn.jeyor1337.bozarxd.obfuscator.transformer.impl.renamer;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.RenamerTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class FieldRenamerTransformer extends RenamerTransformer {

    public FieldRenamerTransformer(Bozar bozar) {
        super(bozar, "Rename", BozarCategory.STABLE);
    }

    @Override
    public void transformClass(ClassNode classNode) {

        getSuperHierarchy(classNode)
                .forEach(cn -> cn.fields.stream()
                        .filter(fieldNode -> !this.getBozar().isExcluded(this, ASMUtils.getName(cn, fieldNode)))
                        .filter(fieldNode -> !this.isMapRegistered(getFieldMapFormat(cn, fieldNode)))
                        .forEach(fieldNode -> this.registerMap(getFieldMapFormat(cn, fieldNode)))
                );

        getSuperHierarchy(classNode)
                .forEach(cn -> cn.fields.stream()
                        .filter(fieldNode -> this.isMapRegistered(getFieldMapFormat(cn, fieldNode)))
                        .filter(fieldNode -> !this.isMapRegistered(getFieldMapFormat(classNode, fieldNode)))
                        .forEach(fieldNode -> this.registerMap(getFieldMapFormat(classNode, fieldNode), this.map.get(getFieldMapFormat(cn, fieldNode))))
                );
    }

    private static String getFieldMapFormat(ClassNode classNode, FieldNode fieldNode) {
        return classNode.name + "." + fieldNode.name;
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().getRename() != this.getEnableType().type(), BozarConfig.BozarOptions.RenameOption.OFF);
    }
}
