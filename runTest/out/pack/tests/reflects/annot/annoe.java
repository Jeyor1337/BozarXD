/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.annot;

import java.lang.reflect.Field;
import pack.tests.reflects.annot.anno;

public class annoe {
    @anno(val="PASS")
    private static final String fail = "WHAT";

    @anno(val="no")
    public void dov() {
        System.out.println("FAIL");
    }

    @anno
    public void dox() throws Exception {
        String string = "FAIL";
        for (Field field : annoe.class.getDeclaredFields()) {
            field.setAccessible(true);
            anno anno2 = field.getAnnotation(anno.class);
            if (anno2 == null) continue;
            string = anno2.val();
        }
        System.out.println(string);
    }
}

