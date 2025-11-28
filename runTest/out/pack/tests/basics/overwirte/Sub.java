/*
 * Decompiled with CFR 0.152.
 */
package pack.tests.basics.overwirte;

import pack.tests.basics.overwirte.Face;
import pack.tests.basics.overwirte.Super;

public class Sub
extends Super {
    @Override
    public void run() {
        Sub sub = this;
        System.out.println(((Face)sub).face(1));
    }

    @Override
    public String face(int n) {
        if (n == 1) {
            return "PASS";
        }
        return "FAIL";
    }
}

