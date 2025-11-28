/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.security;

import java.lang.reflect.Method;
import pack.tests.security.SecExec;
import pack.tests.security.Sman;

public class SecTest {
    public void run() {
        block4: {
            System.setSecurityManager(new Sman());
            System.out.print("FAIL");
            try {
                Method method = SecExec.class.getDeclaredMethod("doShutdown", new Class[0]);
                method.setAccessible(true);
                method.invoke(null, new Object[0]);
            }
            catch (Throwable throwable) {
                Throwable throwable2;
                Throwable throwable3 = throwable;
                while ((throwable2 = throwable3.getCause()) != null) {
                    throwable3 = throwable2;
                }
                String string = throwable3.getMessage();
                if (string == null) {
                    return;
                }
                if (!string.contains("HOOK")) break block4;
                System.out.println("\b\b\b\bPASS");
            }
        }
    }
}

