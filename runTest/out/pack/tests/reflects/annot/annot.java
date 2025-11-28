/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.annot;

import java.lang.reflect.Method;
import pack.tests.reflects.annot.anno;
import pack.tests.reflects.annot.annoe;

public class annot {
    public void run() throws Exception {
        annoe annoe2 = new annoe();
        for (Method method : annoe.class.getDeclaredMethods()) {
            method.setAccessible(true);
            anno anno2 = method.getAnnotation(anno.class);
            if (anno2 == null || !anno2.val().equals("yes")) continue;
            method.invoke((Object)annoe2, new Object[0]);
        }
    }
}

