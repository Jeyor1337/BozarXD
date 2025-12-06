package cn.jeyor1337.bozarxd.obfuscator.transformer.impl.watermark;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class BadAnnoTransformer extends ClassTransformer {

    private static final String DEFAULT_ANNOTATION = "\n\n\n\n\n\n\n\n\n\n\nBOZAR-XD PROTECTED\n\n\n\n\n\n\n\n\n\n\n";
    private String annotationDescriptor;
    private int counter = 0;

    public BadAnnoTransformer(Bozar bozar) {
        super(bozar, "Bad annotation injector", BozarCategory.ADVANCED);
    }

    @Override
    public void pre() {
        this.counter = 0;
        String customText = this.getBozar().getConfig().getOptions().getWatermarkOptions().getBadAnnoText();
        String data = (customText == null || customText.isEmpty()) ? DEFAULT_ANNOTATION : customText;
        this.annotationDescriptor = "L" + data + ";";
    }

    @Override
    public void transformClass(ClassNode classNode) {

        addInvisibleAnnotation(classNode);
    }

    @Override
    public void transformField(ClassNode classNode, FieldNode fieldNode) {

        if (fieldNode.invisibleAnnotations == null) {
            fieldNode.invisibleAnnotations = new java.util.ArrayList<>();
        }
        fieldNode.invisibleAnnotations.add(new AnnotationNode(annotationDescriptor));
        counter++;
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {

        if (methodNode.invisibleAnnotations == null) {
            methodNode.invisibleAnnotations = new java.util.ArrayList<>();
        }
        methodNode.invisibleAnnotations.add(new AnnotationNode(annotationDescriptor));
        counter++;
    }

    @Override
    public void post() {
        this.getBozar().log("Added %d bad annotations", counter);
    }

    private void addInvisibleAnnotation(ClassNode classNode) {
        if (classNode.invisibleAnnotations == null) {
            classNode.invisibleAnnotations = new java.util.ArrayList<>();
        }
        classNode.invisibleAnnotations.add(new AnnotationNode(annotationDescriptor));
        counter++;
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getWatermarkOptions().isBadAnno(),
            boolean.class
        );
    }
}
