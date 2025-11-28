package cn.jeyor1337.bozarxd.obfuscator;

import cn.jeyor1337.bozarxd.obfuscator.transformer.ClassTransformer;
import cn.jeyor1337.bozarxd.obfuscator.transformer.TransformManager;
import cn.jeyor1337.bozarxd.obfuscator.utils.ASMUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.BozarClassVerifier;
import cn.jeyor1337.bozarxd.obfuscator.utils.StreamUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.StringUtils;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.BozarConfig;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.CustomClassWriter;
import cn.jeyor1337.bozarxd.obfuscator.utils.model.ResourceWrapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Getter
@RequiredArgsConstructor
public class Bozar implements Runnable {

    private final BozarConfig config;
    private final List<ClassNode> classes = new ArrayList<>();
    private final List<ResourceWrapper> resources = new ArrayList<>();
    private ClassLoader classLoader;
    private TransformManager transformHandler;

    @Override
    public void run() {
        try {
            // Used to calculate time elapsed
            final long startTime = System.currentTimeMillis();

            // Input file checks
            if(!this.config.getInput().exists())
                throw new FileNotFoundException("Cannot find input");
            if(!this.config.getInput().isFile())
                throw new IllegalArgumentException("Received input is not a file");

            String inputExtension = this.config.getInput().getName().substring(this.config.getInput().getName().lastIndexOf(".") + 1).toLowerCase();
            switch (inputExtension) {
                case "jar" -> {
                    // Read JAR input
                    log("Processing JAR input...");
                    try (var jarInputStream = new ZipInputStream(Files.newInputStream(this.config.getInput().toPath()))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = jarInputStream.getNextEntry()) != null) {
                            if (zipEntry.getName().endsWith(".class")) {
                                if(classes.size() == Integer.MAX_VALUE)
                                    throw new IllegalArgumentException("Maximum class count exceeded");
                                ClassReader reader = new ClassReader(jarInputStream);
                                ClassNode classNode = new ClassNode();
                                reader.accept(classNode, 0);
                                classes.add(classNode);
                            } else {
                                if(resources.size() == Integer.MAX_VALUE)
                                    throw new IllegalArgumentException("Maximum resource count exceeded");
                                resources.add(new ResourceWrapper(zipEntry, StreamUtils.readAll(jarInputStream)));
                            }
                        }
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported file extension: " + inputExtension);
            }
            
            // Empty/corrupted file check
            if(classes.size() == 0)
                throw new IllegalArgumentException("Received input does not look like a proper JAR file");

            // Convert string library paths to URL array
            final var libs = this.getConfig().getLibraries();
            URL[] urls = new URL[libs.size() + 1];
            urls[libs.size()] = this.config.getInput().toURI().toURL();
            for (int i = 0; i < libs.size(); i++)
                urls[i] = new File(libs.get(i)).toURI().toURL();
            this.classLoader = new URLClassLoader(urls);

            // Transform
            log("Transforming...");
            this.transformHandler = new TransformManager(this);
            transformHandler.transformAll();

            // Write output
            log("Writing...");
            try (var out = new JarOutputStream(Files.newOutputStream(this.config.getOutput()))) {
                // Write resources
                resources.stream()
                        .filter(resourceWrapper -> !resourceWrapper.getZipEntry().isDirectory())
                        .filter(resourceWrapper -> resourceWrapper.getBytes() != null)
                        .forEach(resourceWrapper -> {
                    try {
                        out.putNextEntry(new JarEntry(resourceWrapper.getZipEntry().getName()));
                        StreamUtils.copy(new ByteArrayInputStream(resourceWrapper.getBytes()), out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // Write classes
                for(ClassNode classNode : this.classes) {
                    // Transform latest ASM output
                    if(!transformHandler.getClassTransformers().stream()
                            .filter(ClassTransformer::isEnabled)
                            .allMatch(classTransformer -> classTransformer.transformOutput(classNode)))
                        continue;

                    int flags = ClassWriter.COMPUTE_FRAMES;

                    // Skip frames if the class is excluded
                    if(this.isExcluded(null, ASMUtils.getName(classNode)))
                        flags = ClassWriter.COMPUTE_MAXS;

                    var classWriter = new CustomClassWriter(this, flags, this.classLoader);
                    var checkClassAdapter = new CheckClassAdapter(classWriter,true);

                    // for verification
                    classNode.methods.forEach(methodNode -> {
                        methodNode.maxStack += 10; methodNode.maxLocals += 10;
                    });

                    // Process class
                    try {
                        classNode.accept(checkClassAdapter);
                    } catch (Throwable t) {
                        err("Cannot process class: %s", classNode.name);
                        t.printStackTrace();
                        continue;
                    }

                    // Transform ClassWriter
                    transformHandler.getClassTransformers().stream()
                            .filter(ClassTransformer::isEnabled)
                            .forEach(classTransformer -> classTransformer.transformClassWriter(classWriter));

                    // Write class
                    try {
                        byte[] bytes = classWriter.toByteArray();
                        out.putNextEntry(new JarEntry(classNode.name + ".class"));
                        out.write(bytes);
                    } catch (IOException e) {
                        err("Cannot write class: %s" , classNode.name);
                        e.printStackTrace();
                    }
                }

                // Transform jar output
                transformHandler.getClassTransformers().stream()
                        .filter(ClassTransformer::isEnabled)
                        .forEach(classTransformer -> classTransformer.transformOutput(out));
            }

            // Verify classes
            try {
                log("Verifying JAR...");
                if(!BozarClassVerifier.verify(this, this.config.getOutput(), this.classLoader))
                    err("Invalid classes present");
                else log("JAR verified successfully!");
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Elapsed time information
            final String timeElapsed = new DecimalFormat("##.###").format(((double)System.currentTimeMillis() - (double)startTime) / 1000D);
            log("Done. Took %ss", timeElapsed);

            // File size information
            final String oldSize = StringUtils.getConvertedSize(this.config.getInput().length());
            final String newSize = StringUtils.getConvertedSize(this.config.getOutput().toFile().length());
            log("File size changed from %s to %s", oldSize, newSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isExcluded(ClassTransformer classTransformer, final String str) {
        final String s = (str.contains("$")) ? str.substring(0, str.indexOf("$")) : str;

        // Collect all matching rules with their specificity
        List<MatchResult> matches = new ArrayList<>();

        for (String line : this.getConfig().getExclude().lines().toList()) {
            line = line.trim();
            if (line.isEmpty()) continue;

            boolean isInclude = line.startsWith("!");
            String pattern = isInclude ? line.substring(1) : line;

            int specificity = calculateSpecificity(classTransformer, s, pattern);
            if (specificity > 0) {
                matches.add(new MatchResult(isInclude, specificity));
            }
        }

        // If no matches, use default behavior
        if (matches.isEmpty()) {
            // Check if any include rules exist
            boolean hasIncludeRules = this.getConfig().getExclude().lines()
                    .anyMatch(line -> line.trim().startsWith("!"));
            // If include rules exist, default is excluded; otherwise, default is included
            return hasIncludeRules;
        }

        // Find the most specific match
        MatchResult mostSpecific = matches.stream()
                .max(Comparator.comparingInt(m -> m.specificity))
                .orElseThrow();

        // Return true (excluded) if the most specific rule is NOT an include rule
        return !mostSpecific.isInclude;
    }

    private int calculateSpecificity(ClassTransformer classTransformer, final String s, String line) {
        // Detect target transformer
        String targetTransformer = null;
        if(line.contains(":")) {
            targetTransformer = line.split(":")[0];
            line = line.substring((targetTransformer + ":").length());
        }

        // Check transformer match
        if(targetTransformer != null && classTransformer == null) return 0;
        if(targetTransformer != null && !classTransformer.getName().equals(targetTransformer)) return 0;

        // Calculate base specificity score based on pattern type
        int baseScore = 0;
        boolean matches = false;

        if(line.startsWith("**")) {
            // Suffix wildcard: lowest priority
            matches = s.endsWith(line.substring(2));
            baseScore = 100;
        } else if(line.startsWith("*")) {
            // Suffix wildcard
            matches = s.endsWith(line.substring(1));
            baseScore = 200;
        } else if(line.endsWith("**")) {
            // Prefix multi-level wildcard
            matches = s.startsWith(line.substring(0, line.length() - 2));
            baseScore = 300;
        } else if(line.endsWith("*")) {
            // Prefix single-level wildcard
            matches = s.startsWith(line.substring(0, line.length() - 1))
                    && s.chars().filter(ch -> ch == '.').count() == line.chars().filter(ch -> ch == '.').count();
            baseScore = 400;
        } else {
            // Exact match: highest priority
            matches = line.equals(s);
            baseScore = 500;
        }

        if (!matches) return 0;

        // Add pattern length to score for more specific patterns
        // Longer patterns are more specific
        int lengthScore = line.replace("*", "").length();

        // Transformer-specific rules get bonus points
        int transformerBonus = targetTransformer != null ? 1000 : 0;

        return transformerBonus + baseScore + lengthScore;
    }

    private boolean matchesPattern(ClassTransformer classTransformer, final String s, String line) {
        return calculateSpecificity(classTransformer, s, line) > 0;
    }

    private static class MatchResult {
        final boolean isInclude;
        final int specificity;

        MatchResult(boolean isInclude, int specificity) {
            this.isInclude = isInclude;
            this.specificity = specificity;
        }
    }

    public void log(String format, Object... args) {
        System.out.println("[BozarXD] " + String.format(format, args));
    }

    public void err(String format, Object... args) {
        System.err.println("[BozarXD] [ERROR] " + String.format(format, args));
    }
}
