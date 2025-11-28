/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.reflects.loader;

import pack.tests.reflects.loader.Loader;

public class LRun {
    public void run() throws Exception {
        Loader l = new Loader();
        Class<?> c = l.findClass("pack.tests.reflects.loader.LTest");
        Object o = c.newInstance();
        c.getMethod("run", new Class[0]).invoke(o, new Object[0]);
    }
}

