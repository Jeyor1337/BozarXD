package cn.jeyor1337.bozarxd.obfuscator.transformer.impl.watermark;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.ClassWriter;

public class TextInsideClassTransformer extends ClassTransformer {

    public TextInsideClassTransformer(Bozar bozar) {
        super(bozar, "Text inside class", BozarCategory.WATERMARK);
    }

    @Override
    public void transformClassWriter(ClassWriter classWriter) {
        classWriter.newUTF8(this.getBozar().getConfig().getOptions().getWatermarkOptions().getTextInsideClassText());
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().getWatermarkOptions().isTextInsideClass(), "Bozar");
    }
}
