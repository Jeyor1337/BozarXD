package cn.jeyor1337.bozarxd.obfuscator.transformer.impl;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarCategory;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * BadAnnoTransformer - Adds invisible annotations to classes, methods and fields
 * Ported from class-obf obfuscator
 */
public class BadAnnoTransformer extends ClassTransformer {

    private static final String DEFAULT_ANNOTATION = "\n\n\n\n\n\n\n\n\n\n\nBOZAR-XD PROTECTED\n\n\n\n\n\n\n\n\n\n\n";
    private final String annotationDescriptor;
    private int counter = 0;

    public BadAnnoTransformer(Bozar bozar) {
        super(bozar, "Bad annotation injector", BozarCategory.ADVANCED);
        this.annotationDescriptor = generateAnnotationDescriptor();
    }

    @Override
    public void pre() {
        this.counter = 0;
    }

    @Override
    public void transformClass(ClassNode classNode) {
        // Add invisible annotation to class
        addInvisibleAnnotation(classNode);
    }

    @Override
    public void transformField(ClassNode classNode, FieldNode fieldNode) {
        // Add invisible annotation to field
        if (fieldNode.invisibleAnnotations == null) {
            fieldNode.invisibleAnnotations = new java.util.ArrayList<>();
        }
        fieldNode.invisibleAnnotations.add(new AnnotationNode(annotationDescriptor));
        counter++;
    }

    @Override
    public void transformMethod(ClassNode classNode, MethodNode methodNode) {
        // Add invisible annotation to method
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

    private String generateAnnotationDescriptor() {
        String data;
        String customPath = this.getBozar().getConfig().getOptions().getWatermarkOptions().getBadAnnoText();

        if (customPath == null || customPath.isEmpty()) {
            data = DEFAULT_ANNOTATION;
        } else {
            Path annoPath = Paths.get(customPath);
            if (Files.notExists(annoPath)) {
                data = DEFAULT_ANNOTATION;
            } else {
                try {
                    byte[] b = Files.readAllBytes(annoPath);
                    data = new String(b);
                } catch (IOException ignored) {
                    data = DEFAULT_ANNOTATION;
                }
            }
        }
        return "L" + data + ";";
    }

    @Override
    public BozarConfig.EnableType getEnableType() {
        return new BozarConfig.EnableType(
            () -> this.getBozar().getConfig().getOptions().getWatermarkOptions().isBadAnno(),
            boolean.class
        );
    }
}
