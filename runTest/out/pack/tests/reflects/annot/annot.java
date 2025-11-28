/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.annot;

import java.lang.reflect.Method;
import pack.tests.reflects.annot.anno;
import pack.tests.reflects.annot.annoe;

public class annot {
    public void run() throws Exception {
        annoe a = new annoe();
        for (Method m : annoe.class.getDeclaredMethods()) {
            m.setAccessible(true);
            anno an = m.getAnnotation(anno.class);
            if (an == null || !an.val().equals("yes")) continue;
            m.invoke((Object)a, new Object[0]);
        }
    }
}

