package cn.jeyor1337.bozarxd.obfuscator.transformer;

import cn.jeyor1337.bozarxd.obfuscator.Bozar;
import cn.jeyor1337.bozarxd.obfuscator.transformer.impl.*;
import cn.jeyor1337.bozarxd.obfuscator.transformer.impl.renamer.ClassRenamerTransformer;
import cn.jeyor1337.bozarxd.obfuscator.transformer.impl.renamer.FieldRenamerTransformer;
import cn.jeyor1337.bozarxd.obfuscator.transformer.impl.renamer.MethodRenamerTransformer;
import cn.jeyor1337.bozarxd.obfuscator.transformer.impl.watermark.DummyClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.transformer.impl.watermark.TextInsideClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.transformer.impl.watermark.UnusedStringTransformer;
import cn.jeyor1337.bozarxd.obfuscator.transformer.impl.watermark.ZipCommentTransformer;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import lombok.Getter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class TransformManager {

    private final Bozar bozar;
    private final List<ClassTransformer> classTransformers = new ArrayList<>();

    public TransformManager(Bozar bozar) {
        this.bozar = bozar;
        this.classTransformers.addAll(getTransformers().stream()
            .map(clazz -> {
                try {
                    return clazz.getConstructor(Bozar.class).newInstance(this.bozar);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList()));
    }

    public static List<Class<? extends ClassTransformer>> getTransformers() {
        final var transformers = new ArrayList<Class<? extends ClassTransformer>>();

        // Phase 1: Renaming (must be first - executed separately in transformAll)
        transformers.add(ClassRenamerTransformer.class);
        transformers.add(FieldRenamerTransformer.class);
        transformers.add(MethodRenamerTransformer.class);

        // Phase 2: Information removal (pure metadata removal, no logic changes)
        transformers.add(LocalVariableTransformer.class);
        transformers.add(LineNumberTransformer.class);
        transformers.add(SourceFileTransformer.class);

        // Phase 3: Structure adjustments (class structure modifications)
        transformers.add(InnerClassTransformer.class);
        transformers.add(ShuffleTransformer.class);
        transformers.add(BadAnnoTransformer.class);

        // Phase 4: Constant obfuscation (strings and numbers)
        transformers.add(ConstantTransformer.class);

        // Phase 5: Advanced transformations (method signature and call modifications)
        transformers.add(AntiPromptTransformer.class);
        transformers.add(ParamObfTransformer.class);      // CRITICAL: Must be before InvokeDynamic
        transformers.add(InvokeDynamicTransformer.class); // CRITICAL: Must be after ParamObf

        // Phase 6: Control flow obfuscation (maximize analysis difficulty)
        // Applied late to maximize complexity after all other transformations
        transformers.add(LightControlFlowTransformer.class);
        transformers.add(HeavyControlFlowTransformer.class);
        transformers.add(SuperControlFlowTransformer.class);
        transformers.add(UltraControlFlowTransformer.class);

        // Phase 7: Crashers and watermarks (final touches)
        transformers.add(CrasherTransformer.class);
        transformers.add(DummyClassTransformer.class);
        transformers.add(TextInsideClassTransformer.class);
        transformers.add(UnusedStringTransformer.class);
        transformers.add(ZipCommentTransformer.class);

        // TODO: AntiDebugTransformer

        return transformers;
    }

    public static ClassTransformer createTransformerInstance(Class<? extends ClassTransformer> transformerClass) {
        try {
            return transformerClass.getConstructor(Bozar.class).newInstance((Object)null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void transformAll() {
        // Apply renamer transformers
        var map = new HashMap<String, String>();
        this.classTransformers.stream()
                .filter(ClassTransformer::isEnabled)
                .filter(ct -> ct instanceof RenamerTransformer)
                .map(ct -> (RenamerTransformer)ct)
                .forEach(crt -> {
                    this.bozar.log("Applying renamer %s", crt.getName());
                    this.bozar.getClasses().forEach(classNode -> this.transform(classNode, crt.getClass()));
                    this.bozar.getResources().forEach(crt::transformResource);
                    map.putAll(crt.map);
                });

        // Remap classes
        if(this.bozar.getConfig().getOptions().getRename() != BozarConfig.BozarOptions.RenameOption.OFF) {
            this.bozar.log("Applying renamer...");
            var reMapper = new SimpleRemapper(map);
            for (int i = 0; i < this.bozar.getClasses().size(); i++) {
                ClassNode classNode = this.bozar.getClasses().get(i);
                ClassNode remappedClassNode = new ClassNode();
                ClassRemapper adapter = new ClassRemapper(remappedClassNode, reMapper);
                classNode.accept(adapter);
                this.bozar.getClasses().set(i, remappedClassNode);
            }
        }

        // Pre
        this.classTransformers.stream()
                .filter(ClassTransformer::isEnabled)
                .forEach(ClassTransformer::pre);

        // Transform all classes
        this.classTransformers.stream()
            .filter(ClassTransformer::isEnabled)
            .filter(ct -> !(ct instanceof RenamerTransformer))
            .forEach(ct -> {
                this.bozar.log("Applying %s", ct.getName());
                this.bozar.getClasses().forEach(classNode -> this.transform(classNode, ct.getClass()));
                this.bozar.getResources().forEach(ct::transformResource);
        });

        // Post
        this.classTransformers.stream()
                .filter(ClassTransformer::isEnabled)
                .forEach(ClassTransformer::post);
    }

    public void transform(ClassNode classNode, Class<? extends ClassTransformer> transformerClass) {
        ClassTransformer classTransformer = this.getClassTransformer(transformerClass);
        if(this.bozar.isExcluded(classTransformer, ASMUtils.getName(classNode))) return;

        classTransformer.transformClass(classNode);
        classNode.fields.stream()
                .filter(fieldNode -> !this.bozar.isExcluded(classTransformer, ASMUtils.getName(classNode, fieldNode)))
                .forEach(fieldNode -> classTransformer.transformField(classNode, fieldNode));
        classNode.methods.stream()
                .filter(methodNode -> !this.bozar.isExcluded(classTransformer, ASMUtils.getName(classNode) + "." + methodNode.name + "()"))
                .forEach(methodNode -> {
            AbstractInsnNode[] insns = methodNode.instructions.toArray().clone();
            classTransformer.transformMethod(classNode, methodNode);

            // Revert changes if method size is invalid
            if (!ASMUtils.isMethodSizeValid(methodNode)) {
                this.bozar.log("Cannot apply \"%s\" on \"%s\" due to low method capacity", classTransformer.getName(), classNode.name + "." + methodNode.name + methodNode.desc);
                methodNode.instructions = ASMUtils.arrayToList(insns);
            }
        });
    }

    @SuppressWarnings("unchecked") // Checked using stream
    public <T extends ClassTransformer> T getClassTransformer(Class<T> transformerClass) {
        if(transformerClass == null)
            throw new NullPointerException("transformerClass cannot be null");
        return (T) this.classTransformers.stream()
                .filter(ct -> ct.getClass().equals(transformerClass))
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Cannot find transformerClass: " + transformerClass.getName()));
    }
}
