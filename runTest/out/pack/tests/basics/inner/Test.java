/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.inner;

import pack.tests.basics.inner.Exec;

public class Test {
    public void run() {
        Exec exec;
        Exec exec2 = exec = new Exec();
        exec2.getClass();
        Exec.Inner inner1 = exec2.new Exec.Inner(3);
        inner1.doAdd();
        Exec exec3 = exec;
        exec3.getClass();
        Exec.Inner inner2 = exec3.new Exec.Inner(100);
        inner2.doAdd();
        if (exec.fuss == 108) {
            System.out.println("PASS");
        } else {
            System.out.println("ERROR");
        }
    }
}

