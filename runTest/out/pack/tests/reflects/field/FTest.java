/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.field;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import pack.tests.reflects.field.FObject;

public class FTest {
    public void run() throws Exception {
        Constructor constructor = FObject.class.getDeclaredConstructor(Integer.TYPE);
        if (constructor.isAccessible()) {
            System.out.println("FAIL");
            return;
        }
        constructor.setAccessible(true);
        FObject fObject = (FObject)constructor.newInstance(1);
        Method method = FObject.class.getDeclaredMethod("add", null);
        if (method.isAccessible()) {
            System.out.println("FAIL");
            return;
        }
        method.setAccessible(true);
        method.invoke((Object)fObject, new Object[0]);
        Field field = FObject.class.getDeclaredField("i");
        if (field.isAccessible()) {
            System.out.println("FAIL");
            return;
        }
        field.setAccessible(true);
        if (field.getInt(fObject) != 4) {
            System.out.println("FAIL");
            return;
        }
        System.out.println("PASS");
    }
}

