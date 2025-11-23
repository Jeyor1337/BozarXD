package cn.jeyor1337.bozarxd.obfuscator.transformer.impl.watermark;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;

import java.util.jar.JarOutputStream;

public class ZipCommentTransformer extends ClassTransformer {

    public ZipCommentTransformer(Bozar bozar) {
        super(bozar, "Zip comment", BozarCategory.WATERMARK);
    }

    @Override
    public void transformOutput(JarOutputStream jarOutputStream) {
        jarOutputStream.setComment(this.getBozar().getConfig().getOptions().getWatermarkOptions().getZipCommentText());
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(() -> this.getBozar().getConfig().getOptions().getWatermarkOptions().isZipComment(), "Obfuscation provided by\nhttps://github.com/Jeyor1337/BozarXD");
    }
}
