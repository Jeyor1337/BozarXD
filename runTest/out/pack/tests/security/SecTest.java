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
                Method m = SecExec.class.getDeclaredMethod("doShutdown", new Class[0]);
                m.setAccessible(true);
                m.invoke(null, new Object[0]);
            }
            catch (Throwable t) {
                Throwable r;
                Throwable f = t;
                while ((r = f.getCause()) != null) {
                    f = r;
                }
                String str = f.getMessage();
                if (str == null) {
                    return;
                }
                if (!str.contains("HOOK")) break block4;
                System.out.println("\b\b\b\bPASS");
            }
        }
    }
}

